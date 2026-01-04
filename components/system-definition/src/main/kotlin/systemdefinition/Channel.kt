package systemdefinition

/**
 * Represents a communication channel/topic for event routing.
 *
 * Channels are used to route events between handlers in an event-driven system.
 * Each channel has a unique name and optional description.
 *
 * @property name Unique channel identifier (e.g., "user-events", "order-queue")
 * @property description Optional human-readable description of the channel's purpose
 */
data class Channel(
    val name: String,
    val description: String? = null
) {
    init {
        require(name.isNotBlank()) { "Channel name must not be blank" }
    }
}

