package systemdefinition

/**
 * Represents a critical configuration error in the system.
 *
 * Errors indicate misconfigurations that prevent the system from
 * functioning correctly and must be fixed before deployment.
 *
 * @property type The category of error
 * @property message Human-readable description of the error
 * @property context Additional context (handler name, event type, etc.)
 */
data class VerificationError(
    val type: ErrorType,
    val message: String,
    val context: Map<String, String> = emptyMap()
) {
    enum class ErrorType {
        /**
         * Handler returns an event that is not declared in the system.
         */
        UNHANDLED_EVENT,

        /**
         * Handler is registered for an event it doesn't declare receiving.
         */
        HANDLER_EVENT_MISMATCH
    }
}

