package eventprotocol

/**
 * Protocol defining the three-phase handler interface.
 * 
 * Handlers implement lookup → operate → write phases to process events
 * consistently across the system.
 * 
 * The three phases enable:
 * - lookup: Fetch data from database/cache before processing
 * - operate: Pure transformation logic with all needed data
 * - write: Persist changes to database/external systems
 */
interface IHandler {
    /**
     * Lookup phase: Fetch data needed for operation.
     * 
     * This phase runs first and fetches any data needed for the operate phase.
     * For handlers that don't need lookup, return empty LookupData.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @return Lookup data to pass to operate phase
     */
    fun lookup(context: HandlerContext, event: BaseEvent): LookupData
    
    /**
     * Operate phase: Transform the event using lookup data.
     * 
     * This phase contains the core business logic. It receives the event
     * and any data from the lookup phase, and returns a transformed event.
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
     * This phase runs last and persists any changes to database or external systems.
     * For handlers that don't need persistence, return the event unchanged.
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

