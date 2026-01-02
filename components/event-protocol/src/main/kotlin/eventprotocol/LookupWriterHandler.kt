package eventprotocol

/**
 * Handler implementation with full lookup, operate, and write phases.
 * 
 * LookupWriterHandler is the most complete handler type, supporting:
 * - Database lookup before processing
 * - Event transformation with lookup data
 * - Database write after processing
 * 
 * This handler type is ideal for CRUD operations, event sourcing,
 * and any workflow requiring database interaction.
 * 
 * @property lookerUpper Function to fetch data needed for operation
 * @property operator Function to transform event using lookup data
 * @property writer Function to persist changes
 * @property metadata Handler metadata describing receives/returns event types
 */
class LookupWriterHandler(
    private val lookerUpper: LookerUpperFunction,
    private val operator: OperatorFunction,
    private val writer: WriterFunction,
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
     * Write phase: Persist changes.
     * 
     * Invokes the writer function to persist event changes to database,
     * message queue, or external services.
     * 
     * @param context Execution context
     * @param event Event to persist
     * @return Event (may be enriched with write results like generated IDs)
     */
    override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
        return writer(context, event)
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

