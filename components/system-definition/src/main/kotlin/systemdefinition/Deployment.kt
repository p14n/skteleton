package systemdefinition

/**
 * Logical grouping of handlers deployed together.
 *
 * Deployments represent how handlers are organized in the runtime environment,
 * such as microservices, serverless functions, or process boundaries.
 *
 * @property name Unique deployment identifier (e.g., "user-service", "payment-processor")
 * @property handlerNames Names of handlers in this deployment
 * @property description Optional description of the deployment's purpose
 */
data class Deployment(
    val name: String,
    val handlerNames: Set<String>,
    val description: String? = null
) {
    init {
        require(name.isNotBlank()) { "Deployment name must not be blank" }
    }
}

