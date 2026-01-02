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

// ============================================================================
// Factory Methods and Validation Helpers
// ============================================================================

/**
 * Create a BaseEvent with automatic ID generation.
 *
 * @param type Event type identifier
 * @param data Event payload data
 * @param correlationId Optional correlation ID for tracking
 * @param subject Optional subject identifier
 * @return New BaseEvent with generated eventId
 */
fun createEvent(
    type: String,
    data: Map<String, Any?>,
    correlationId: String? = null,
    subject: String? = null
): BaseEvent {
    val eventId = "evt_${System.currentTimeMillis()}_${(0..999).random()}"
    return BaseEvent(
        eventId = eventId,
        type = type,
        data = data,
        correlationId = correlationId,
        subject = subject
    )
}

/**
 * Validate a BaseEvent and return a Result.
 *
 * @return Result.success if valid, Result.failure with exception if invalid
 */
fun BaseEvent.validate(): Result<BaseEvent> = runCatching {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(type.isNotBlank()) { "type must not be blank" }
    this
}

/**
 * Create a derived event from an existing event, preserving correlation tracking.
 *
 * @param newType New event type
 * @param newData New event data
 * @return New BaseEvent with preserved correlationId and new eventId
 */
fun BaseEvent.deriveEvent(
    newType: String,
    newData: Map<String, Any?> = this.data
): BaseEvent {
    return createEvent(
        type = newType,
        data = newData,
        correlationId = this.correlationId,
        subject = this.subject
    )
}

