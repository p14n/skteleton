package eventprotocol

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
     * Orchestrates the three-phase execution:
     * 1. lookup: Fetch data needed for operation
     * 2. operate: Transform event using lookup data
     * 3. write: Persist changes
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

