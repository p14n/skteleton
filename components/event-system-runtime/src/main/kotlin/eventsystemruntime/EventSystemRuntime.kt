package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventsystemruntime.config.ValidationException
import eventsystemruntime.config.ConfigurationException
import eventsystemruntime.config.ValidationRules
import systemdefinition.SystemDefinition
import systemdefinition.HandlerRegistration
import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import postevent.PosteventSystem

/**
 * Main orchestrator for event routing and handler subscription.
 *
 * The EventSystemRuntime is the central component that manages the lifecycle of an event-driven system.
 * It coordinates event routing, handler execution, and resource management based on a SystemDefinition.
 *
 * ## Responsibilities
 *
 * - **Initialization**: Validates and initializes the event system from a SystemDefinition
 * - **Channel Management**: Subscribes handlers to appropriate channels (transient or persistent)
 * - **Event Publishing**: Routes events to channels with automatic correlation ID management
 * - **Resilience**: Provides circuit breaker protection for persistent channels
 * - **Lifecycle Management**: Supports graceful shutdown with configurable timeout
 *
 * ## Channel Types
 *
 * - **Transient Channels**: In-memory routing via Vert.x EventBus (low latency, no persistence)
 * - **Persistent Channels**: Database-backed routing via postevent system (durable, transactional)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val systemDef = SystemDefinition.Builder()
 *     .addChannel("orders", "Order events")
 *     .addEvent("order.placed", setOf("orders"))
 *     .addHandler(OrderHandler(), "OrderHandler")
 *     .build()
 *
 * val config = RuntimeConfig(
 *     persistentChannels = setOf("orders"),
 *     datasource = DataSourceConfig(...)
 * )
 *
 * val runtime = initialize(systemDef, config)
 *
 * // Publish events
 * runtime.publish("orders", BaseEvent(...))
 *
 * // Monitor status
 * val status = runtime.getStatus()
 *
 * // Graceful shutdown
 * runtime.shutdown()
 * ```
 *
 * @property systemDefinition Immutable system configuration defining handlers, events, and channels
 * @property config Runtime configuration including timeouts, circuit breaker settings, and channel types
 * @property vertx Vert.x instance for event bus and async operations
 * @property eventBus Vert.x EventBus for transient channel routing
 * @property posteventSystem Optional postevent system for persistent channel routing
 * @property circuitBreakerManager Circuit breaker manager for database resilience
 * @property shutdownCoordinator Coordinator for graceful shutdown
 *
 * @see initialize
 * @see RuntimeConfig
 * @see RuntimeStatus
 */
class EventSystemRuntime internal constructor(
    private val systemDefinition: SystemDefinition,
    private val config: RuntimeConfig,
    private val vertx: Vertx,
    private val eventBus: EventBus,
    private val posteventSystem: PosteventSystem?,
    private val circuitBreakerManager: CircuitBreakerManager,
    private val shutdownCoordinator: ShutdownCoordinator
) {
    private val logger = LoggerFactory.getLogger(EventSystemRuntime::class.java)
    private var state: RuntimeState = RuntimeState.UNINITIALIZED
    private val channelSubscribers = mutableMapOf<String, ChannelSubscriber>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Get current runtime status.
     *
     * Returns a snapshot of the runtime's current state, including:
     * - Runtime state (UNINITIALIZED, RUNNING, SHUTDOWN)
     * - Number of in-flight handlers currently executing
     * - List of subscribed channels
     * - Circuit breaker states for persistent channels
     *
     * This method is thread-safe and can be called at any time.
     *
     * @return RuntimeStatus containing current state information
     */
    fun getStatus(): RuntimeStatus {
        return RuntimeStatus(
            state = state,
            inFlightHandlers = shutdownCoordinator.getInFlightCount(),
            subscribedChannels = channelSubscribers.keys.toList(),
            circuitBreakerStates = circuitBreakerManager.getAllStates()
        )
    }

    /**
     * Get event router for a specific channel.
     * Useful for testing and accessing interceptor chain.
     *
     * @param channelName Channel name (defaults to first channel)
     * @return EventRouter for the channel
     */
    fun getEventRouter(channelName: String? = null): EventRouter {
        val channel = channelName ?: channelSubscribers.keys.firstOrNull()
        requireNotNull(channel) { "No channels subscribed" }

        val subscriber = channelSubscribers[channel]
        requireNotNull(subscriber) { "Channel $channel not found" }

        // Access the event router through reflection (for testing)
        // In production, we'd expose this more cleanly
        val field = subscriber.javaClass.getDeclaredField("eventRouter")
        field.isAccessible = true
        return field.get(subscriber) as EventRouter
    }
    
    /**
     * Publish an event to a channel.
     *
     * Routes the event to the specified channel, which will deliver it to all subscribed handlers
     * that match the event type. The routing mechanism depends on the channel type:
     *
     * - **Transient channels**: Published via Vert.x EventBus (in-memory, low latency)
     * - **Persistent channels**: Published via postevent system (database-backed, durable)
     *
     * ## Correlation ID Management
     *
     * If correlation ID generation is enabled in RuntimeConfig and the event does not have a
     * correlation ID, one will be automatically generated. This enables distributed tracing
     * across multi-step event flows.
     *
     * ## Circuit Breaker Protection
     *
     * Persistent channel publishing is protected by a circuit breaker. If database operations
     * fail repeatedly, the circuit breaker will open and fail fast to prevent cascading failures.
     *
     * ## Best-Effort Delivery
     *
     * Publishing to non-existent channels logs a warning but does not throw an exception.
     *
     * @param channelName Name of the channel to publish to
     * @param event Event to publish
     * @param context Optional handler context (auto-generated if not provided)
     * @throws CircuitBreakerOpenException if circuit breaker is open for persistent channel
     */
    suspend fun publish(
        channelName: String,
        event: BaseEvent,
        context: HandlerContext? = null
    ) {
        // T038: Ensure correlation ID exists
        val eventWithCorrelation = if (config.enableCorrelationIdGeneration && event.correlationId == null) {
            event.copy(correlationId = java.util.UUID.randomUUID().toString())
        } else {
            event
        }

        // T039: Check if channel exists
        val channelExists = systemDefinition.channels.any { it.name == channelName }
        if (!channelExists) {
            logger.warn("Attempted to publish to non-existent channel: $channelName (event: ${event.eventId})")
            return // Best-effort delivery - don't throw
        }

        // Determine channel type
        val channelType = if (config.persistentChannels.contains(channelName)) {
            ChannelType.PERSISTENT
        } else {
            ChannelType.TRANSIENT
        }

        // T036/T037: Publish based on channel type
        when (channelType) {
            ChannelType.TRANSIENT -> publishToTransientChannel(channelName, eventWithCorrelation)
            ChannelType.PERSISTENT -> publishToPersistentChannel(channelName, eventWithCorrelation)
        }

        logger.debug("Published event ${event.eventId} to channel $channelName (type: $channelType)")
    }

    /**
     * Publish to transient channel via Vert.x EventBus.
     *
     * FR-019: Publish to address {channel}.{event-type}
     */
    private fun publishToTransientChannel(channelName: String, event: BaseEvent) {
        val address = "$channelName.${event.type}"
        eventBus.publish(address, event)
        logger.debug("Published to transient address: $address")
    }

    /**
     * Publish to persistent channel via postevent system.
     *
     * T054: Implement persistent channel publishing via postevent
     * T088: Integrate circuit breaker for database resilience
     */
    private suspend fun publishToPersistentChannel(channelName: String, event: BaseEvent) {
        if (posteventSystem == null) {
            logger.error("Cannot publish to persistent channel $channelName: postevent system not initialized")
            return
        }

        try {
            // Execute with circuit breaker protection
            circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                posteventSystem.publish(channelName, event, connection = null)
            }
            logger.debug("Published to persistent channel: $channelName")
        } catch (e: CircuitBreakerOpenException) {
            logger.error("Circuit breaker open for channel $channelName - event ${event.eventId} not published")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to publish to persistent channel $channelName: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Gracefully shutdown the event system.
     *
     * Initiates a graceful shutdown sequence that:
     * 1. Unsubscribes all channel subscribers
     * 2. Waits for in-flight handlers to complete (up to configured timeout)
     * 3. Cleans up resources (Vert.x instance, coroutine scopes)
     *
     * ## Graceful vs Forced Shutdown
     *
     * - **Graceful**: All in-flight handlers complete within timeout, resources cleaned up properly
     * - **Forced**: Timeout expires with handlers still running, resources cleaned up immediately
     *
     * The shutdown timeout is configured via `RuntimeConfig.shutdownTimeoutMs` (default: 30 seconds).
     *
     * ## Thread Safety
     *
     * This method is thread-safe and can be called multiple times (subsequent calls are no-ops).
     *
     * @param timeoutMs Optional timeout override (currently unused, uses config default)
     * @return true if shutdown completed gracefully, false if forced
     */
    suspend fun shutdown(timeoutMs: Long? = null): Boolean {
        logger.info("Shutting down event system runtime...")
        state = RuntimeState.SHUTDOWN

        // Unsubscribe all channels
        channelSubscribers.values.forEach { it.unsubscribe() }
        channelSubscribers.clear()

        // Perform graceful shutdown
        val graceful = shutdownCoordinator.shutdown()

        logger.info("Event system runtime shutdown complete (graceful: $graceful)")
        return graceful
    }

    /**
     * Internal setter for state (used by initialize function).
     */
    internal fun setState(newState: RuntimeState) {
        state = newState
    }

    /**
     * Internal function to initialize channel subscriptions.
     */
    internal suspend fun initializeChannelSubscriptions() {
        // Group handlers by channel
        val handlersByChannel = mutableMapOf<String, MutableList<HandlerRegistration>>()

        // For each handler, determine which channels it should subscribe to
        systemDefinition.handlers.forEach { registration ->
            // Get all event types this handler receives
            val eventTypes = registration.metadata.receives

            // Find channels that route these event types
            systemDefinition.events.forEach { eventDef ->
                if (eventTypes.contains(eventDef.eventType)) {
                    eventDef.outputChannels.forEach { channelName ->
                        handlersByChannel.getOrPut(channelName) { mutableListOf() }.add(registration)
                    }
                }
            }
        }

        // Create ChannelSubscriber for each channel
        handlersByChannel.forEach { (channelName, handlers) ->
            val channelType = if (config.persistentChannels.contains(channelName)) {
                ChannelType.PERSISTENT
            } else {
                ChannelType.TRANSIENT
            }

            val eventRouter = EventRouter(
                systemDefinition,
                handlers,
                shutdownCoordinator = shutdownCoordinator
            )
            val subscriber = ChannelSubscriber(
                channelName = channelName,
                channelType = channelType,
                handlers = handlers,
                eventRouter = eventRouter,
                systemDefinition = systemDefinition,
                eventBus = eventBus,
                posteventSystem = posteventSystem,
                scope = scope,
                publishEvent = { channel, event -> publish(channel, event) }
            )

            // T018/T019: Subscribe to channel
            subscriber.subscribe()
            channelSubscribers[channelName] = subscriber

            // T021: Log subscription
            logger.info("Subscribed ${handlers.size} handler(s) to channel: $channelName (type: $channelType)")
        }
    }
}

/**
 * Initialize event system runtime from SystemDefinition.
 *
 * This is the primary entry point for creating an EventSystemRuntime instance. It performs
 * the following initialization steps:
 *
 * 1. **Validation**: Validates the SystemDefinition and RuntimeConfig
 * 2. **Infrastructure Setup**: Creates Vert.x instance, EventBus, and postevent system (if needed)
 * 3. **Resilience Components**: Initializes circuit breaker manager and shutdown coordinator
 * 4. **Channel Subscription**: Subscribes handlers to appropriate channels based on event types
 * 5. **State Transition**: Transitions runtime to RUNNING state
 *
 * ## Validation Rules
 *
 * - SystemDefinition must have at least one handler
 * - All event types referenced by handlers must be defined
 * - Persistent channels must have datasource configuration
 * - Channel names must be unique
 *
 * ## Channel Type Configuration
 *
 * Channels are configured as transient or persistent via `RuntimeConfig.persistentChannels`:
 *
 * ```kotlin
 * val config = RuntimeConfig(
 *     persistentChannels = setOf("orders", "payments"),
 *     datasource = DataSourceConfig(...)
 * )
 * ```
 *
 * ## Example
 *
 * ```kotlin
 * val systemDef = SystemDefinition.Builder()
 *     .addChannel("orders", "Order events")
 *     .addEvent("order.placed", setOf("orders"))
 *     .addHandler(OrderHandler(), "OrderHandler")
 *     .build()
 *
 * val runtime = initialize(systemDef, RuntimeConfig())
 * ```
 *
 * @param systemDefinition Immutable system configuration defining handlers, events, and channels
 * @param config Runtime configuration including timeouts, circuit breaker settings, and channel types
 * @return Initialized EventSystemRuntime instance in RUNNING state
 * @throws ValidationException if SystemDefinition is invalid
 * @throws ConfigurationException if persistent channels lack datasource configuration
 */
fun initialize(
    systemDefinition: SystemDefinition,
    config: RuntimeConfig = RuntimeConfig()
): EventSystemRuntime {
    val logger = LoggerFactory.getLogger("EventSystemRuntime")

    // T017: FR-001 - Accept SystemDefinition as input
    // T020: FR-002 - Validate SystemDefinition before registering handlers
    logger.info("Initializing event system runtime...")

    try {
        ValidationRules.validate(systemDefinition, config)
    } catch (e: ValidationException) {
        logger.error("SystemDefinition validation failed: ${e.message}")
        throw e
    } catch (e: ConfigurationException) {
        logger.error("Configuration validation failed: ${e.message}")
        throw e
    }

    // Create Vert.x instance and event bus
    val vertx = Vertx.vertx()
    val eventBus = vertx.eventBus()

    // Register BaseEvent codec for Vert.x EventBus
    eventBus.registerDefaultCodec(BaseEvent::class.java, BaseEventCodec())
    logger.debug("Registered BaseEvent codec with EventBus")

    // Create coroutine scope
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Create postevent system if persistent channels are configured
    val posteventSystem = if (config.persistentChannels.isNotEmpty()) {
        PosteventSystem(vertx, scope)
    } else {
        null
    }

    // Create circuit breaker manager
    val circuitBreakerManager = CircuitBreakerManager(config)

    // Create shutdown coordinator
    val shutdownCoordinator = ShutdownCoordinator(config, vertx, scope)

    // Create runtime instance
    val runtime = EventSystemRuntime(
        systemDefinition,
        config,
        vertx,
        eventBus,
        posteventSystem,
        circuitBreakerManager,
        shutdownCoordinator
    )

    // T018: Subscribe handlers to transient channels
    // T019: Subscribe handlers to persistent channels
    runBlocking {
        runtime.initializeChannelSubscriptions()
    }

    runtime.setState(RuntimeState.RUNNING)
    logger.info("Event system runtime initialized successfully")

    return runtime
}

/**
 * Runtime state enum.
 *
 * Represents the lifecycle state of an EventSystemRuntime instance.
 *
 * ## State Transitions
 *
 * ```
 * UNINITIALIZED → RUNNING → SHUTDOWN
 * ```
 *
 * - **UNINITIALIZED**: Runtime created but not yet initialized
 * - **INITIALIZING**: Currently unused (reserved for future async initialization)
 * - **RUNNING**: Runtime is active and processing events
 * - **SHUTTING_DOWN**: Currently unused (reserved for future async shutdown)
 * - **SHUTDOWN**: Runtime has been shut down, resources cleaned up
 */
enum class RuntimeState {
    UNINITIALIZED,
    INITIALIZING,
    RUNNING,
    SHUTTING_DOWN,
    SHUTDOWN
}

/**
 * Runtime status snapshot.
 *
 * Provides a point-in-time view of the runtime's operational state.
 * Returned by [EventSystemRuntime.getStatus].
 *
 * ## Example
 *
 * ```kotlin
 * val status = runtime.getStatus()
 * println("State: ${status.state}")
 * println("In-flight handlers: ${status.inFlightHandlers}")
 * println("Channels: ${status.subscribedChannels}")
 * status.circuitBreakerStates.forEach { (channel, state) ->
 *     println("Circuit breaker for $channel: $state")
 * }
 * ```
 *
 * @property state Current runtime state
 * @property inFlightHandlers Number of handlers currently executing
 * @property subscribedChannels List of channel names with active subscriptions
 * @property circuitBreakerStates Circuit breaker state for each persistent channel
 */
data class RuntimeStatus(
    val state: RuntimeState,
    val inFlightHandlers: Int,
    val subscribedChannels: List<String>,
    val circuitBreakerStates: Map<String, CircuitBreakerState>
)

/**
 * Exception thrown when channel is not found.
 */
class ChannelNotFoundException(message: String) : Exception(message)

