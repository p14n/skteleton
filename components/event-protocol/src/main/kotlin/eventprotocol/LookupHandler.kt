package eventprotocol

/**
 * Handler implementation with database lookup before operation (no write).
 * 
 * LookupHandler supports:
 * - Database lookup before processing
 * - Event transformation with lookup data
 * - No database write (read-only operations)
 * 
 * This handler type is ideal for:
 * - Duplicate detection
 * - Validation against existing data
 * - Data enrichment from database
 * - Read-only operations
 * 
 * @property lookerUpper Function to fetch data needed for operation
 * @property operator Function to transform event using lookup data
 * @property metadata Handler metadata describing receives/returns event types
 */
class LookupHandler(
    private val lookerUpper: LookerUpperFunction,
    private val operator: OperatorFunction,
    private val metadata: HandlerMetadata
) : IHandler {
    
    /**
     * Lookup phase: Fetch data needed for operation.
     * 
     * Invokes the looker-upper function to fetch data from database,
     * cache, or external services.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @return Lookup data to pass to operate phase
     */
    override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
        return lookerUpper(context, event)
    }
    
    /**
     * Operate phase: Transform the event using lookup data.
     * 
     * Invokes the operator function to transform the event based on
     * business logic and lookup data.
     * 
     * @param context Execution context
     * @param event Event being processed
     * @param data Data from lookup phase
     * @return Transformed event
     */
    override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
        return operator(context, event, data)
    }
    
    /**
     * Write phase: Returns event unchanged.
     * 
     * LookupHandler doesn't perform database writes (read-only),
     * so this returns the event as-is.
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

