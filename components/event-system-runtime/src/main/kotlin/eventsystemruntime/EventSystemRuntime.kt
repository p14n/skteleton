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

/**
 * Main orchestrator for event routing and handler subscription.
 *
 * Responsible for:
 * - Initializing event system from SystemDefinition
 * - Subscribing handlers to appropriate channels (transient or persistent)
 * - Publishing events to channels
 * - Graceful shutdown with timeout
 */
class EventSystemRuntime internal constructor(
    private val systemDefinition: SystemDefinition,
    private val config: RuntimeConfig,
    private val vertx: Vertx,
    private val eventBus: EventBus
) {
    private val logger = LoggerFactory.getLogger(EventSystemRuntime::class.java)
    private var state: RuntimeState = RuntimeState.UNINITIALIZED
    private val channelSubscribers = mutableMapOf<String, ChannelSubscriber>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Get current runtime status.
     */
    fun getStatus(): RuntimeStatus {
        return RuntimeStatus(
            state = state,
            inFlightHandlers = 0,
            subscribedChannels = channelSubscribers.keys.toList(),
            circuitBreakerStates = emptyMap()
        )
    }
    
    /**
     * Publish an event to a channel.
     *
     * T036: Publish to transient channels via Vert.x EventBus
     * T037: Publish to persistent channels via postevent
     * T038: Generate correlation ID if missing
     * T039: Log errors for non-existent channels (best-effort delivery)
     *
     * @param channelName Name of the channel to publish to
     * @param event Event to publish
     * @param context Optional handler context (auto-generated if not provided)
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
     * Will be fully implemented in User Story 3.
     */
    private fun publishToPersistentChannel(channelName: String, event: BaseEvent) {
        // Placeholder for postevent publishing
        logger.info("Persistent channel publishing for $channelName (placeholder)")
    }
    
    /**
     * Gracefully shutdown the event system.
     */
    suspend fun shutdown(timeoutMs: Long? = null): Boolean {
        // Placeholder for shutdown logic
        // Will be implemented in T085
        state = RuntimeState.SHUTDOWN
        return true
    }
}

/**
 * Initialize event system runtime from SystemDefinition.
 *
 * @param systemDefinition Immutable system configuration
 * @param config Runtime configuration (optional, uses defaults if not provided)
 * @return Initialized EventSystemRuntime instance
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

    // Create runtime instance
    val runtime = EventSystemRuntime(systemDefinition, config, vertx, eventBus)

    // T018: Subscribe handlers to transient channels
    // T019: Subscribe handlers to persistent channels
    runBlocking {
        runtime.initializeChannelSubscriptions()
    }

    runtime.state = RuntimeState.RUNNING
    logger.info("Event system runtime initialized successfully")

    return runtime
}

/**
 * Internal function to initialize channel subscriptions.
 */
private suspend fun EventSystemRuntime.initializeChannelSubscriptions() {
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

        val eventRouter = EventRouter(systemDefinition, handlers)
        val subscriber = ChannelSubscriber(
            channelName = channelName,
            channelType = channelType,
            handlers = handlers,
            eventRouter = eventRouter,
            systemDefinition = systemDefinition,
            eventBus = eventBus,
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

/**
 * Runtime state enum.
 */
enum class RuntimeState {
    UNINITIALIZED,
    INITIALIZING,
    RUNNING,
    SHUTTING_DOWN,
    SHUTDOWN
}

/**
 * Runtime status data class.
 */
data class RuntimeStatus(
    val state: RuntimeState,
    val inFlightHandlers: Int,
    val subscribedChannels: List<String>,
    val circuitBreakerStates: Map<String, CircuitBreakerState>
)

/**
 * Circuit breaker state enum.
 */
enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

/**
 * Exception thrown when channel is not found.
 */
class ChannelNotFoundException(message: String) : Exception(message)

