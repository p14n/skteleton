package eventsystemruntime

import eventprotocol.BaseEvent
import java.util.UUID

/**
 * Manages correlation IDs across event flows.
 * 
 * Responsible for:
 * - Generating correlation IDs when missing
 * - Propagating correlation IDs to output events
 * - Storing correlation IDs in CoroutineContext
 */
class CorrelationIdManager(
    private val enableGeneration: Boolean = true
) {
    /**
     * Ensure event has a correlation ID, generating one if missing.
     *
     * T061: Generate correlation ID if missing
     *
     * @param event Event to check
     * @return Event with correlation ID
     */
    fun ensureCorrelationId(event: BaseEvent): BaseEvent {
        if (!enableGeneration) {
            return event
        }

        return if (event.correlationId == null) {
            event.copy(correlationId = UUID.randomUUID().toString())
        } else {
            event
        }
    }

    /**
     * Propagate correlation ID to output events.
     *
     * T062: Propagate correlation ID from input event to output events
     *
     * @param correlationId Correlation ID to propagate
     * @param outputEvents Output events to update
     * @return Output events with correlation ID
     */
    fun propagateToOutputEvents(correlationId: String?, outputEvents: List<BaseEvent>): List<BaseEvent> {
        if (correlationId == null) {
            return outputEvents
        }

        return outputEvents.map { outputEvent ->
            if (outputEvent.correlationId == null) {
                outputEvent.copy(correlationId = correlationId)
            } else {
                outputEvent
            }
        }
    }

    /**
     * Get current correlation ID from event.
     *
     * @param event Event to extract correlation ID from
     * @return Correlation ID or null if not present
     */
    fun getCorrelationId(event: BaseEvent): String? {
        return event.correlationId
    }
}

