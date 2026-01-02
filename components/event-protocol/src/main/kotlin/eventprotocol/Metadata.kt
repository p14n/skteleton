package eventprotocol

/**
 * Metadata describing which events a handler receives and returns.
 *
 * Used for automatic routing table generation and handler discovery.
 *
 * @property receives Set of event types this handler can receive
 * @property returns Set of event types this handler can return
 * @property description Optional human-readable description of handler purpose
 */
data class HandlerMetadata(
    val receives: Set<String>,
    val returns: Set<String>,
    val description: String? = null
) {
    init {
        require(receives.isNotEmpty()) { "receives must not be empty" }
    }
}

/**
 * Build a routing table from a list of handlers.
 *
 * Creates a map from event type to handler, allowing automatic routing
 * of events to the appropriate handler based on event type.
 *
 * @param handlers List of handlers to build routing table from
 * @return Map from event type to handler
 */
fun buildRoutingTable(handlers: List<IHandler>): Map<String, IHandler> {
    val routingTable = mutableMapOf<String, IHandler>()

    for (handler in handlers) {
        val metadata = handler.operatorMeta()
        for (eventType in metadata.receives) {
            routingTable[eventType] = handler
        }
    }

    return routingTable
}
