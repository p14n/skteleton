package systemdefinition

/**
 * Represents a non-critical issue that should be reviewed.
 *
 * Warnings indicate potential problems or architectural patterns
 * that may be intentional but should be verified by developers.
 *
 * @property type The category of warning
 * @property message Human-readable description of the warning
 * @property context Additional context (event type, handler names, etc.)
 */
data class VerificationWarning(
    val type: WarningType,
    val message: String,
    val context: Map<String, String> = emptyMap()
) {
    enum class WarningType {
        /**
         * Event is declared but has no handlers that consume it.
         */
        ORPHANED_EVENT,

        /**
         * Circular event flow detected (handler A -> event X -> handler B -> event Y -> handler A).
         */
        CIRCULAR_FLOW
    }
}

