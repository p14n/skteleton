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

