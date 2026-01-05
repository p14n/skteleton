package postevent

import eventprotocol.BaseEvent
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Stub implementation of postevent system for persistent event storage.
 * 
 * This is a minimal in-memory implementation to satisfy the contract.
 * A production implementation would use actual database storage.
 * 
 * Features:
 * - Publish events to persistent channels
 * - Subscribe to persistent channels with event handlers
 * - Transactional context support (stub)
 */
class PosteventSystem(
    private val vertx: Vertx,
    private val scope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(PosteventSystem::class.java)
    
    // Channel name -> List of subscribers
    private val subscribers = ConcurrentHashMap<String, MutableList<suspend (BaseEvent, Connection?) -> Unit>>()
    
    // In-memory event storage (stub for database)
    private val eventStore = ConcurrentHashMap<String, MutableList<BaseEvent>>()
    
    /**
     * Publish an event to a persistent channel.
     * 
     * @param channel Channel name
     * @param event Event to publish
     * @param connection Optional database connection for transactional publishing
     */
    suspend fun publish(
        channel: String,
        event: BaseEvent,
        connection: Connection? = null
    ) {
        logger.debug("Publishing event ${event.eventId} to persistent channel: $channel")
        
        // Store event (in production, this would be database insert)
        eventStore.getOrPut(channel) { mutableListOf() }.add(event)
        
        // Deliver to subscribers
        val channelSubscribers = subscribers[channel] ?: emptyList()
        channelSubscribers.forEach { handler ->
            scope.launch {
                try {
                    handler(event, connection)
                } catch (e: Exception) {
                    logger.error("Error delivering event ${event.eventId} to subscriber: ${e.message}", e)
                }
            }
        }
        
        logger.debug("Event ${event.eventId} published to ${channelSubscribers.size} subscriber(s)")
    }
    
    /**
     * Subscribe to a persistent channel.
     * 
     * @param channel Channel name
     * @param handler Event handler that receives event and optional database connection
     */
    fun subscribe(
        channel: String,
        handler: suspend (BaseEvent, Connection?) -> Unit
    ) {
        subscribers.getOrPut(channel) { mutableListOf() }.add(handler)
        logger.info("Subscribed to persistent channel: $channel")
    }
    
    /**
     * Unsubscribe from a persistent channel.
     * 
     * @param channel Channel name
     */
    fun unsubscribe(channel: String) {
        subscribers.remove(channel)
        logger.info("Unsubscribed from persistent channel: $channel")
    }
    
    /**
     * Get all events for a channel (for testing).
     * 
     * @param channel Channel name
     * @return List of events published to the channel
     */
    fun getEvents(channel: String): List<BaseEvent> {
        return eventStore[channel]?.toList() ?: emptyList()
    }
    
    /**
     * Clear all events (for testing).
     */
    fun clearEvents() {
        eventStore.clear()
    }
    
    /**
     * Get subscriber count for a channel (for testing).
     */
    fun getSubscriberCount(channel: String): Int {
        return subscribers[channel]?.size ?: 0
    }
}

