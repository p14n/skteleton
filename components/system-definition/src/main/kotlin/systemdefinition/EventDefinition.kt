package systemdefinition

/**
 * Defines an event type and its routing configuration.
 *
 * EventDefinition captures which channels an event can be routed to,
 * and tracks which handlers produce and consume this event type.
 *
 * @property eventType Unique event type identifier (e.g., "user.created")
 * @property outputChannels Channels this event can be routed to (may be multiple)
 * @property producedBy Handler names that return this event (computed from metadata)
 * @property consumedBy Handler names that receive this event (computed from metadata)
 */
data class EventDefinition(
    val eventType: String,
    val outputChannels: Set<String> = emptySet(),
    val producedBy: Set<String> = emptySet(),
    val consumedBy: Set<String> = emptySet()
) {
    init {
        require(eventType.isNotBlank()) { "Event type must not be blank" }
    }
}

