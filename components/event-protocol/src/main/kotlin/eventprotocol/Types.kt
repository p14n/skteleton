package eventprotocol

/**
 * Function type aliases for handler composition.
 * 
 * These type aliases enable functional programming patterns and make
 * handler signatures more readable.
 */

/**
 * Operator function type: transforms event with context and lookup data.
 * 
 * This is the core transformation function used in handlers.
 * 
 * @param context Execution context
 * @param event Event to transform
 * @param data Lookup data from lookup phase
 * @return Transformed event
 */
typealias OperatorFunction = (context: HandlerContext, event: BaseEvent, data: LookupData) -> BaseEvent

/**
 * Looker-upper function type: fetches data for operation.
 * 
 * Used in handlers that need to fetch data before processing.
 * 
 * @param context Execution context
 * @param event Event being processed
 * @return Lookup data to pass to operate phase
 */
typealias LookerUpperFunction = (context: HandlerContext, event: BaseEvent) -> LookupData

/**
 * Writer function type: persists event changes.
 * 
 * Used in handlers that need to persist changes after processing.
 * 
 * @param context Execution context
 * @param event Event to persist
 * @return Event (may be enriched with write results)
 */
typealias WriterFunction = (context: HandlerContext, event: BaseEvent) -> BaseEvent

/**
 * Entity update handler function type: updates entity based on event.
 * 
 * Used in polymorphic dispatch for entity-specific update logic.
 * 
 * @param context Execution context
 * @param event Event triggering the update
 * @param entity Entity to update
 * @return Updated entity
 */
typealias EntityUpdateHandler = (context: HandlerContext, event: BaseEvent, entity: Any) -> Any

