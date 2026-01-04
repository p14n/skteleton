package systemdefinition

import eventprotocol.HandlerMetadata
import eventprotocol.IHandler

/**
 * Associates a handler instance with a name in the system.
 *
 * HandlerRegistration captures the relationship between a named handler
 * and its implementation, along with cached metadata about what events
 * it receives and returns.
 *
 * @property name Unique identifier for the handler in the system
 * @property handler The actual handler instance from event-protocol
 * @property metadata Cached metadata from handler.operatorMeta()
 */
data class HandlerRegistration(
    val name: String,
    val handler: IHandler,
    val metadata: HandlerMetadata = handler.operatorMeta()
) {
    init {
        require(name.isNotBlank()) { "Handler name must not be blank" }
    }
}

