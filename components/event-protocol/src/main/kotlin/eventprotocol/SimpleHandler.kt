package eventprotocol

/**
 * Handler implementation for pure transformations without database interaction.
 * 
 * SimpleHandler is the simplest handler type, supporting only the operate phase.
 * It's ideal for:
 * - Event validation
 * - Data enrichment
 * - Filtering
 * - Pure business logic transformations
 * 
 * The lookup phase returns empty data, and the write phase returns the event unchanged.
 * 
 * @property operator Function to transform event
 * @property metadata Handler metadata describing receives/returns event types
 */
class SimpleHandler(
    private val operator: OperatorFunction,
    private val metadata: HandlerMetadata
) : IHandler {
    
    /**
     * Lookup phase: Returns empty LookupData.
     * 
     * SimpleHandler doesn't perform database lookups, so this always
     * returns empty data.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @return Empty LookupData
     */
    override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
        return LookupData()
    }
    
    /**
     * Operate phase: Transform the event.
     * 
     * Invokes the operator function to transform the event based on
     * pure business logic without database interaction.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @param data Lookup data (always empty for SimpleHandler)
     * @return Transformed event
     */
    override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
        return operator(context, event, data)
    }
    
    /**
     * Write phase: Returns event unchanged.
     * 
     * SimpleHandler doesn't perform database writes, so this returns
     * the event as-is.
     * 
     * @param context Execution context
     * @param event Event to persist
     * @return Event unchanged
     */
    override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
        return event
    }
    
    /**
     * Get handler metadata.
     * 
     * Returns metadata describing which event types this handler
     * receives and returns, used for automatic routing.
     * 
     * @return Handler metadata
     */
    override fun operatorMeta(): HandlerMetadata {
        return metadata
    }
}

