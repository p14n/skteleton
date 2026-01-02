package eventprotocol

/**
 * Executor orchestrates event processing through the three-phase handler pipeline.
 * 
 * The Executor implements the IExecute protocol and coordinates the execution
 * of IHandler implementations by calling lookup → operate → write in sequence.
 * 
 * This class is the primary entry point for executing events through the system.
 * 
 * @property handler The IHandler implementation to execute
 */
class Executor(
    private val handler: IHandler
) : IExecute {
    
    /**
     * Execute an event through the handler pipeline.
     * 
     * Orchestrates the three-phase execution:
     * 1. lookup: Fetch data needed for operation
     * 2. operate: Transform the event using lookup data
     * 3. write: Persist changes
     * 
     * @param context Execution context with request-scoped data
     * @param event Event to process
     * @return Processed event (result of write phase)
     * @throws Exception if any phase fails
     */
    override fun execute(context: HandlerContext, event: BaseEvent): BaseEvent {
        // Phase 1: Lookup
        val lookupData = handler.lookup(context, event)
        
        // Phase 2: Operate
        val operatedEvent = handler.operate(context, event, lookupData)
        
        // Phase 3: Write
        val writtenEvent = handler.write(context, operatedEvent)
        
        return writtenEvent
    }
    
    /**
     * Get metadata from the wrapped handler.
     * 
     * Delegates to the handler's operatorMeta() method to retrieve
     * information about which event types the handler receives and returns.
     * 
     * @return Handler metadata describing receives/returns event types
     */
    override fun executorMeta(): HandlerMetadata {
        return handler.operatorMeta()
    }
}

