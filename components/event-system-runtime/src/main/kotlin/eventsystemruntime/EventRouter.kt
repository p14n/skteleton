package eventsystemruntime

import eventprotocol.*
import systemdefinition.HandlerRegistration
import systemdefinition.SystemDefinition
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Routes incoming events to appropriate handlers.
 *
 * Responsible for:
 * - Matching events to handlers based on event type
 * - Executing handlers through adapters
 * - Routing output events to configured channels
 * - Isolating handler failures
 */
class EventRouter(
    private val systemDefinition: SystemDefinition,
    private val handlers: List<HandlerRegistration>,
    private val correlationIdManager: CorrelationIdManager = CorrelationIdManager(),
    private val interceptorChain: InterceptorChain = InterceptorChain(),
    private val shutdownCoordinator: ShutdownCoordinator? = null
) {
    private val logger = LoggerFactory.getLogger(EventRouter::class.java)

    /**
     * Route an event to all matching handlers.
     *
     * T027: Match events to handlers based on event type
     * T028: Execute handlers concurrently with fault isolation
     * T029: Collect output events from handlers
     * T030: Log routing events
     *
     * @param event Event to route
     * @param context Handler execution context
     * @return List of output events from handlers
     */
    suspend fun routeEvent(event: BaseEvent, context: HandlerContext): List<BaseEvent> {
        // T027: Find handlers that can process this event type
        val matchingHandlers = handlers.filter { registration ->
            registration.metadata.receives.contains(event.type)
        }

        if (matchingHandlers.isEmpty()) {
            logger.debug("No handlers found for event type: ${event.type}")
            return emptyList()
        }

        logger.debug("Routing event ${event.eventId} (type: ${event.type}) to ${matchingHandlers.size} handler(s)")

        // T028: Execute handlers concurrently with fault isolation
        // Use supervisorScope to isolate handler failures
        val outputEvents = mutableListOf<BaseEvent>()

        supervisorScope {
            val jobs = matchingHandlers.map { registration ->
                async {
                    var outputEvent: BaseEvent? = null
                    var error: Throwable? = null

                    // Track handler execution
                    shutdownCoordinator?.handlerStarted()

                    try {
                        // T073: Execute interceptors before handler
                        val interceptedContext = interceptorChain.executeInterceptors(context, event)

                        // T028: Execute handler through adapter
                        val adapter = createHandlerAdapter(registration)
                        outputEvent = adapter.execute(interceptedContext, event)

                        // T029: Collect output event
                        logger.debug("Handler ${registration.name} produced output event: ${outputEvent.type}")
                    } catch (e: Exception) {
                        // T028: Fault isolation - log error but don't fail other handlers
                        // T087: Structured logging with correlation ID
                        val correlationId = event.correlationId ?: "none"
                        logger.error("Handler ${registration.name} failed for event ${event.eventId} (correlationId: $correlationId): ${e.message}", e)
                        error = e
                    } finally {
                        // T074: Execute finalisers after handler (always)
                        interceptorChain.executeFinalisers(context, event, outputEvent, error)

                        // Track handler completion
                        shutdownCoordinator?.handlerCompleted()
                    }

                    outputEvent
                }
            }

            // Wait for all handlers to complete
            jobs.awaitAll().filterNotNull().forEach { outputEvents.add(it) }
        }

        // T030: Log routing completion
        logger.debug("Event ${event.eventId} routing complete. Produced ${outputEvents.size} output event(s)")

        // T062: Propagate correlation ID to output events
        val correlationId = correlationIdManager.getCorrelationId(event)
        val outputEventsWithCorrelation = correlationIdManager.propagateToOutputEvents(correlationId, outputEvents)

        return outputEventsWithCorrelation
    }

    /**
     * Get the interceptor chain for registering interceptors/finalisers.
     *
     * T071, T072: Expose interceptor chain for registration
     */
    fun getInterceptorChain(): InterceptorChain = interceptorChain

    /**
     * Create appropriate adapter for handler type.
     *
     * For now, only IHandler is supported (from system-definition).
     * IExecute and plain functions will be added in User Story 4.
     */
    private fun createHandlerAdapter(registration: HandlerRegistration): HandlerAdapter {
        // Currently HandlerRegistration only supports IHandler
        return IHandlerAdapter(registration.handler)
    }
}

/**
 * Adapter for IHandler type.
 *
 * Wraps IHandler in Executor pattern: lookup → operate → write
 */
class IHandlerAdapter(private val handler: IHandler) : HandlerAdapter {
    override suspend fun execute(context: HandlerContext, event: BaseEvent): BaseEvent {
        // Execute IHandler pipeline
        val lookupData = handler.lookup(context, event)
        val operatedEvent = handler.operate(context, event, lookupData)
        val writtenEvent = handler.write(context, operatedEvent)
        return writtenEvent
    }
}

