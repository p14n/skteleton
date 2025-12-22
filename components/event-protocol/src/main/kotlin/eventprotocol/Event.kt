package eventprotocol

/**
 * Base interface for all events in the system.
 */
interface Event {
    val id: String
    val timestamp: Long
    val type: String
}

/**
 * A simple data event implementation.
 */
data class DataEvent(
    override val id: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val type: String = "DataEvent",
    val payload: Map<String, Any?> = emptyMap()
) : Event

