package eventsystemruntime

import systemdefinition.HandlerRegistration
import systemdefinition.SystemDefinition
import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import postevent.PosteventSystem
import java.util.UUID

/**
 * Manages subscriptions for a single channel.
 *
 * Responsible for:
 * - Subscribing handlers to Vert.x EventBus (transient) or postevent (persistent)
 * - Managing subscription lifecycle
 * - Routing incoming events to EventRouter
 * - Publishing output events to their configured channels
 */
class ChannelSubscriber(
    val channelName: String,
    val channelType: ChannelType,
    private val handlers: List<HandlerRegistration>,
    private val eventRouter: EventRouter,
    private val systemDefinition: SystemDefinition,
    private val eventBus: EventBus,
    private val posteventSystem: PosteventSystem?,
    private val scope: CoroutineScope,
    private val publishEvent: suspend (String, BaseEvent) -> Unit
) {
    private val logger = LoggerFactory.getLogger(ChannelSubscriber::class.java)
    private var state: SubscriptionState = SubscriptionState.UNSUBSCRIBED
    private val consumers = mutableListOf<MessageConsumer<*>>()
    
    /**
     * Subscribe to the channel based on channel type.
     */
    suspend fun subscribe() {
        if (state != SubscriptionState.UNSUBSCRIBED) {
            logger.warn("Channel $channelName already subscribed (state: $state)")
            return
        }
        
        state = SubscriptionState.SUBSCRIBING
        
        when (channelType) {
            ChannelType.TRANSIENT -> subscribeTransient()
            ChannelType.PERSISTENT -> subscribePersistent()
        }
        
        state = SubscriptionState.SUBSCRIBED
        logger.info("Subscribed to channel: $channelName (type: $channelType)")
    }
    
    /**
     * Subscribe to transient channel via Vert.x EventBus.
     *
     * FR-004: Subscribe at addresses following {channel}.{event-type} pattern
     */
    private fun subscribeTransient() {
        // Get all event types that handlers on this channel can receive
        val eventTypes = handlers.flatMap { it.metadata.receives }.toSet()

        eventTypes.forEach { eventType ->
            val address = "$channelName.$eventType"
            val consumer = eventBus.consumer<Any>(address) { message ->
                logger.debug("Received event on $address")

                // Route event to handlers asynchronously
                scope.launch {
                    try {
                        // For now, assume message body is BaseEvent
                        // In production, this would be JSON deserialization
                        val event = message.body() as? BaseEvent
                            ?: throw IllegalArgumentException("Message body is not a BaseEvent")

                        // Create handler context
                        val context = HandlerContext(
                            requestId = UUID.randomUUID().toString(),
                            metadata = emptyMap(),
                            timestamp = System.currentTimeMillis()
                        )

                        // Route event through handlers
                        val outputEvents = eventRouter.routeEvent(event, context)

                        // Publish output events to their configured channels
                        outputEvents.forEach { outputEvent ->
                            publishOutputEvent(outputEvent)
                        }

                        logger.debug("Routed event ${event.eventId}, produced ${outputEvents.size} output events")

                        message.reply("ACK")
                    } catch (e: Exception) {
                        logger.error("Error routing event on $address: ${e.message}", e)
                        message.fail(500, e.message)
                    }
                }
            }
            consumers.add(consumer)
            logger.debug("Subscribed to transient address: $address")
        }
    }
    
    /**
     * Subscribe to persistent channel via postevent system.
     *
     * FR-005: Subscribe handlers to postevent system for persistent channels
     * T054: Implement persistent channel subscription
     */
    private fun subscribePersistent() {
        if (posteventSystem == null) {
            logger.error("Cannot subscribe to persistent channel $channelName: postevent system not initialized")
            return
        }

        // Subscribe to postevent channel
        posteventSystem.subscribe(channelName) { event, connection ->
            logger.debug("Received event ${event.eventId} on persistent channel: $channelName")

            // Create handler context with database connection
            val context = HandlerContext(
                requestId = UUID.randomUUID().toString(),
                metadata = if (connection != null) mapOf("connection" to connection) else emptyMap(),
                timestamp = System.currentTimeMillis()
            )

            // Route event through handlers
            val outputEvents = eventRouter.routeEvent(event, context)

            // Publish output events to their configured channels
            outputEvents.forEach { outputEvent ->
                publishOutputEvent(outputEvent)
            }

            logger.debug("Routed persistent event ${event.eventId}, produced ${outputEvents.size} output events")
        }

        logger.info("Subscribed to persistent channel: $channelName")
    }
    
    /**
     * Unsubscribe from the channel.
     */
    suspend fun unsubscribe() {
        if (state != SubscriptionState.SUBSCRIBED) {
            logger.warn("Channel $channelName not subscribed (state: $state)")
            return
        }
        
        state = SubscriptionState.UNSUBSCRIBING
        
        // Unregister all consumers
        consumers.forEach { it.unregister() }
        consumers.clear()
        
        state = SubscriptionState.UNSUBSCRIBED
        logger.info("Unsubscribed from channel: $channelName")
    }
    
    /**
     * Get current subscription state.
     */
    fun getState(): SubscriptionState = state

    /**
     * Publish an output event to its configured channels.
     *
     * Looks up the event type in SystemDefinition to find output channels,
     * then publishes to each channel.
     */
    private suspend fun publishOutputEvent(event: BaseEvent) {
        // Find event definition for this event type
        val eventDef = systemDefinition.events.find { it.eventType == event.type }

        if (eventDef == null) {
            logger.warn("No event definition found for output event type: ${event.type}")
            return
        }

        if (eventDef.outputChannels.isEmpty()) {
            logger.debug("Event type ${event.type} has no configured output channels")
            return
        }

        // Publish to each configured output channel
        eventDef.outputChannels.forEach { outputChannel ->
            try {
                publishEvent(outputChannel, event)
                logger.debug("Published output event ${event.eventId} (type: ${event.type}) to channel: $outputChannel")
            } catch (e: Exception) {
                logger.error("Failed to publish output event to channel $outputChannel: ${e.message}", e)
            }
        }
    }
}

/**
 * Subscription state enum.
 */
enum class SubscriptionState {
    UNSUBSCRIBED,
    SUBSCRIBING,
    SUBSCRIBED,
    UNSUBSCRIBING
}

