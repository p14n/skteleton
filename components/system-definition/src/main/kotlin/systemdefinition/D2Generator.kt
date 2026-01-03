package systemdefinition

/**
 * Generates D2 diagram syntax from SystemDefinition.
 *
 * D2 is a declarative diagram language that can be rendered to various formats.
 * This generator creates D2 syntax representing the event-driven system architecture.
 *
 * Generated diagrams include:
 * - Handler nodes
 * - Event flow edges between handlers
 * - Channel groupings
 * - Deployment containers
 */
object D2Generator {
    /**
     * Generate D2 diagram syntax from a system definition.
     *
     * @param system The system to visualize
     * @return D2 syntax as a string
     */
    fun toD2(system: SystemDefinition): String {
        val lines = mutableListOf<String>()

        // Add title
        lines.add("# Event-Driven System Architecture")
        lines.add("")

        // Generate deployment containers if any
        if (system.deployments.isNotEmpty()) {
            system.deployments.forEach { deployment ->
                lines.add("${deployment.name}: {")
                lines.add("  label: \"${deployment.description ?: deployment.name}\"")
                lines.add("  style.stroke: \"#4A90E2\"")
                lines.add("")

                // Add handlers in this deployment
                deployment.handlerNames.forEach { handlerName ->
                    val handler = system.handlers.find { it.name == handlerName }
                    if (handler != null) {
                        lines.add("  $handlerName: {")
                        lines.add("    label: \"$handlerName\"")
                        lines.add("    shape: rectangle")
                        if (handler.metadata.description != null) {
                            lines.add("    tooltip: \"${handler.metadata.description}\"")
                        }
                        lines.add("  }")
                    }
                }

                lines.add("}")
                lines.add("")
            }
        } else {
            // No deployments - add handlers directly
            system.handlers.forEach { handler ->
                lines.add("${handler.name}: {")
                lines.add("  label: \"${handler.name}\"")
                lines.add("  shape: rectangle")
                if (handler.metadata.description != null) {
                    lines.add("  tooltip: \"${handler.metadata.description}\"")
                }
                lines.add("}")
            }
            lines.add("")
        }

        // Generate event flow edges
        system.events.forEach { event ->
            event.producedBy.forEach { producer ->
                event.consumedBy.forEach { consumer ->
                    val producerPath = getHandlerPath(system, producer)
                    val consumerPath = getHandlerPath(system, consumer)
                    lines.add("$producerPath -> $consumerPath: {")
                    lines.add("  label: \"${event.eventType}\"")
                    lines.add("  style.stroke: \"#7ED321\"")
                    lines.add("}")
                }
            }
        }

        // Add channel groupings as notes if channels exist
        if (system.channels.isNotEmpty()) {
            lines.add("")
            lines.add("# Channels")
            system.channels.forEach { channel ->
                val events = system.getEventsForChannel(channel.name)
                if (events.isNotEmpty()) {
                    lines.add("# ${channel.name}: ${events.map { it.eventType }.joinToString(", ")}")
                }
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Get the full path to a handler in the D2 diagram.
     * If handler is in a deployment, returns "deployment.handler", otherwise just "handler".
     */
    private fun getHandlerPath(system: SystemDefinition, handlerName: String): String {
        val deployment = system.deployments.find { handlerName in it.handlerNames }
        return if (deployment != null) {
            "${deployment.name}.$handlerName"
        } else {
            handlerName
        }
    }
}

/**
 * Extension function to generate D2 diagram from a SystemDefinition.
 */
fun SystemDefinition.toD2(): String = D2Generator.toD2(this)

