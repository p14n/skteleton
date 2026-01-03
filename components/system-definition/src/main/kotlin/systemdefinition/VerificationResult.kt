package systemdefinition

/**
 * Result of system verification containing errors and warnings.
 *
 * VerificationResult captures all configuration issues found during
 * system validation. Errors represent critical misconfigurations that
 * prevent the system from functioning correctly, while warnings indicate
 * potential issues that should be reviewed.
 *
 * @property errors Critical configuration errors that must be fixed
 * @property warnings Non-critical issues that should be reviewed
 */
data class VerificationResult(
    val errors: List<VerificationError>,
    val warnings: List<VerificationWarning>
) {
    /**
     * Check if verification passed (no errors).
     * Warnings are allowed.
     */
    val isValid: Boolean
        get() = errors.isEmpty()

    /**
     * Check if verification found any issues (errors or warnings).
     */
    val hasIssues: Boolean
        get() = errors.isNotEmpty() || warnings.isNotEmpty()
}

