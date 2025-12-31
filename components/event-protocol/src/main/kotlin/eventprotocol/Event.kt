package eventprotocol

/**
 * Base event schema for all events in the system.
 *
 * All events must conform to this structure to ensure consistency
 * and enable correlation tracking across the system.
 *
 * @property eventId Unique identifier for this event instance
 * @property type Event type identifier (e.g., "user.created", "order.updated")
 * @property data Event payload as key-value map
 * @property correlationId Optional correlation ID for tracking related events
 * @property subject Optional subject identifier (e.g., user ID, order ID)
 * @property timestamp Event timestamp in milliseconds since epoch
 */
data class BaseEvent(
    val eventId: String,
    val type: String,
    val data: Map<String, Any?>,
    val correlationId: String? = null,
    val subject: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(type.isNotBlank()) { "type must not be blank" }
    }
}

/**
 * Execution context passed to handlers containing request-scoped data.
 *
 * @property requestId Unique identifier for this execution request
 * @property metadata Request-scoped metadata (e.g., user info, trace context)
 * @property timestamp Request timestamp in milliseconds since epoch
 */
data class HandlerContext(
    val requestId: String,
    val metadata: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank" }
    }
}

/**
 * Data returned from lookup phase, passed to operate phase.
 *
 * @property data Lookup results as key-value map
 */
data class LookupData(
    val data: Map<String, Any?> = emptyMap()
)

