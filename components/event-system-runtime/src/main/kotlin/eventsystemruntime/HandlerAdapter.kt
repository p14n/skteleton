package eventsystemruntime

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext

/**
 * Adapter interface for different handler types.
 * 
 * Provides a unified execution interface for IHandler, IExecute, and plain function handlers.
 */
interface HandlerAdapter {
    /**
     * Execute the handler with the given context and event.
     * 
     * @param context Handler execution context
     * @param event Event to process
     * @return Result event from handler execution
     */
    suspend fun execute(context: HandlerContext, event: BaseEvent): BaseEvent
}

