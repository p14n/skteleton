package systemdefinition

/**
 * Verifies system configuration and detects misconfigurations.
 *
 * SystemVerifier analyzes a SystemDefinition to detect:
 * - Errors: Critical misconfigurations that prevent correct operation
 * - Warnings: Potential issues that should be reviewed
 */
object SystemVerifier {
    /**
     * Verify a system definition and return all errors and warnings.
     *
     * @param system The system to verify
     * @return VerificationResult containing all detected issues
     */
    fun verify(system: SystemDefinition): VerificationResult {
        val errors = mutableListOf<VerificationError>()
        val warnings = mutableListOf<VerificationWarning>()

        // Check for unhandled events (events returned by handlers but not declared)
        errors.addAll(detectUnhandledEvents(system))

        // Check for handler event mismatches
        errors.addAll(detectHandlerEventMismatches(system))

        // Check for orphaned events (declared but no handlers)
        warnings.addAll(detectOrphanedEvents(system))

        // Check for circular flows
        warnings.addAll(detectCircularFlows(system))

        return VerificationResult(errors, warnings)
    }

    /**
     * Detect events that are returned by handlers but not declared in the system.
     */
    private fun detectUnhandledEvents(system: SystemDefinition): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()
        val declaredEvents = system.events.map { it.eventType }.toSet()

        system.handlers.forEach { handler ->
            val undeclaredEvents = handler.metadata.returns - declaredEvents
            undeclaredEvents.forEach { eventType ->
                errors.add(
                    VerificationError(
                        type = VerificationError.ErrorType.UNHANDLED_EVENT,
                        message = "Handler '${handler.name}' returns event '$eventType' which is not declared in the system",
                        context = mapOf(
                            "handler" to handler.name,
                            "eventType" to eventType
                        )
                    )
                )
            }
        }

        return errors
    }

    /**
     * Detect handlers registered for events they don't declare receiving.
     */
    private fun detectHandlerEventMismatches(system: SystemDefinition): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        system.events.forEach { event ->
            event.consumedBy.forEach { handlerName ->
                val handler = system.handlers.find { it.name == handlerName }
                if (handler != null && event.eventType !in handler.metadata.receives) {
                    errors.add(
                        VerificationError(
                            type = VerificationError.ErrorType.HANDLER_EVENT_MISMATCH,
                            message = "Handler '$handlerName' is registered to consume '${event.eventType}' but doesn't declare receiving it",
                            context = mapOf(
                                "handler" to handlerName,
                                "eventType" to event.eventType
                            )
                        )
                    )
                }
            }
        }

        return errors
    }

    /**
     * Detect events that are declared but have no handlers consuming them.
     */
    private fun detectOrphanedEvents(system: SystemDefinition): List<VerificationWarning> {
        val warnings = mutableListOf<VerificationWarning>()

        system.events.forEach { event ->
            if (event.consumedBy.isEmpty()) {
                warnings.add(
                    VerificationWarning(
                        type = VerificationWarning.WarningType.ORPHANED_EVENT,
                        message = "Event '${event.eventType}' is declared but has no handlers consuming it",
                        context = mapOf("eventType" to event.eventType)
                    )
                )
            }
        }

        return warnings
    }

    /**
     * Detect circular event flows using graph analysis.
     */
    private fun detectCircularFlows(system: SystemDefinition): List<VerificationWarning> {
        val warnings = mutableListOf<VerificationWarning>()
        val graph = EventFlowGraph.fromSystemDefinition(system)
        val cycles = graph.detectCycles()

        cycles.forEach { cycle ->
            warnings.add(
                VerificationWarning(
                    type = VerificationWarning.WarningType.CIRCULAR_FLOW,
                    message = "Circular event flow detected: ${cycle.joinToString(" -> ")}",
                    context = mapOf("cycle" to cycle.joinToString(","))
                )
            )
        }

        return warnings
    }
}

/**
 * Extension function to verify a SystemDefinition.
 */
fun SystemDefinition.verify(): VerificationResult = SystemVerifier.verify(this)

