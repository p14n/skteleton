/**
 * Event Protocol Contracts
 * 
 * This file defines the API contracts for the Platform Event Protocol.
 * These interfaces and data classes form the public API that domain developers
 * will use to implement event handlers.
 * 
 * Feature: 001-event-protocol
 * Date: 2025-12-22
 */

package eventprotocol

// ============================================================================
// Core Data Types
// ============================================================================

/**
 * Base event schema for all events in the system.
 * 
 * All events must conform to this structure to ensure consistency
 * and enable correlation tracking across the system.
 */
data class BaseEvent(
    val eventId: String,
    val type: String,
    val data: Map<String, Any?>,
    val correlationId: String? = null,
    val subject: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(type.isNotBlank()) { "type must not be blank" }
    }
}

/**
 * Execution context passed to handlers containing request-scoped data.
 */
data class HandlerContext(
    val requestId: String,
    val metadata: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank" }
    }
}

/**
 * Data returned from lookup phase, passed to operate phase.
 */
data class LookupData(
    val data: Map<String, Any?> = emptyMap()
)

/**
 * Metadata describing which events a handler receives and returns.
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

// ============================================================================
// Protocol Interfaces
// ============================================================================

/**
 * Protocol for executing events through a handler pipeline.
 * 
 * Implementations orchestrate the execution of handlers by coordinating
 * the lookup → operate → write phases.
 */
interface IExecute {
    /**
     * Execute an event through the handler pipeline.
     * 
     * @param context Execution context with request-scoped data
     * @param event Event to process
     * @return Processed event (may be transformed)
     */
    fun execute(context: HandlerContext, event: BaseEvent): BaseEvent
    
    /**
     * Get metadata from the wrapped handler.
     * 
     * @return Handler metadata describing receives/returns event types
     */
    fun executorMeta(): HandlerMetadata
}

/**
 * Protocol defining the three-phase handler interface.
 * 
 * Handlers implement lookup → operate → write phases to process events
 * consistently across the system.
 */
interface IHandler {
    /**
     * Lookup phase: Fetch data needed for operation.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @return Lookup data to pass to operate phase
     */
    fun lookup(context: HandlerContext, event: BaseEvent): LookupData
    
    /**
     * Operate phase: Transform the event using lookup data.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @param data Data from lookup phase
     * @return Transformed event
     */
    fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent
    
    /**
     * Write phase: Persist changes.
     * 
     * @param context Execution context
     * @param event Event to persist
     * @return Event (may be unchanged or enriched with write results)
     */
    fun write(context: HandlerContext, event: BaseEvent): BaseEvent
    
    /**
     * Get handler metadata.
     * 
     * @return Metadata describing receives/returns event types
     */
    fun operatorMeta(): HandlerMetadata
}

// ============================================================================
// Function Type Aliases
// ============================================================================

/**
 * Operator function type: transforms event with context and lookup data.
 */
typealias OperatorFunction = (context: HandlerContext, event: BaseEvent, data: LookupData) -> BaseEvent

/**
 * Looker-upper function type: fetches data for operation.
 */
typealias LookerUpperFunction = (context: HandlerContext, event: BaseEvent) -> LookupData

/**
 * Writer function type: persists event changes.
 */
typealias WriterFunction = (context: HandlerContext, event: BaseEvent) -> BaseEvent

/**
 * Entity update handler function type: updates entity based on event.
 */
typealias EntityUpdateHandler = (context: HandlerContext, event: BaseEvent, entity: Any) -> Any

