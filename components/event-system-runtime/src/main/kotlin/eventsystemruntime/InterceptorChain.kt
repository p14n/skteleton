package eventsystemruntime

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import org.slf4j.LoggerFactory

/**
 * Interceptor function type.
 * 
 * Interceptors are invoked BEFORE handler execution.
 * They can modify the context or event before passing to the handler.
 * 
 * @param context Handler context
 * @param event Event being processed
 * @return Modified context (or original if no changes)
 */
typealias Interceptor = (HandlerContext, BaseEvent) -> HandlerContext

/**
 * Finaliser function type.
 * 
 * Finalisers are invoked AFTER handler execution (success or failure).
 * They can perform cleanup, logging, or other post-processing.
 * 
 * @param context Handler context
 * @param event Event that was processed
 * @param result Result event from handler (null if handler failed)
 * @param error Exception if handler failed (null if successful)
 */
typealias Finaliser = (HandlerContext, BaseEvent, BaseEvent?, Throwable?) -> Unit

/**
 * Manages execution interceptors and finalisers.
 * 
 * Responsible for:
 * - Registering interceptor and finaliser functions
 * - Executing interceptors before handler execution
 * - Executing finalisers after handler execution
 * - Maintaining execution order
 * 
 * T070-T075: Interceptor and finaliser support
 */
class InterceptorChain {
    private val logger = LoggerFactory.getLogger(InterceptorChain::class.java)
    private val interceptors = mutableListOf<Interceptor>()
    private val finalisers = mutableListOf<Finaliser>()
    
    /**
     * Register an interceptor function.
     * 
     * T071: FR-016 - Register interceptor functions
     * 
     * Interceptors are executed in registration order before handler execution.
     * 
     * @param interceptor Interceptor function to register
     */
    fun registerInterceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
        logger.debug("Registered interceptor (total: ${interceptors.size})")
    }
    
    /**
     * Register a finaliser function.
     * 
     * T072: FR-017 - Register finaliser functions
     * 
     * Finalisers are executed in registration order after handler execution.
     * 
     * @param finaliser Finaliser function to register
     */
    fun registerFinaliser(finaliser: Finaliser) {
        finalisers.add(finaliser)
        logger.debug("Registered finaliser (total: ${finalisers.size})")
    }
    
    /**
     * Execute all interceptors in order.
     * 
     * T073: Execute interceptors before handler
     * 
     * Each interceptor receives the context from the previous interceptor.
     * 
     * @param context Initial handler context
     * @param event Event being processed
     * @return Modified context after all interceptors
     */
    fun executeInterceptors(context: HandlerContext, event: BaseEvent): HandlerContext {
        var currentContext = context

        interceptors.forEach { interceptor ->
            try {
                currentContext = interceptor(currentContext, event)
                logger.trace("Interceptor executed successfully")
            } catch (e: Exception) {
                logger.error("Interceptor failed: ${e.message}", e)
                // Continue with current context - don't fail the entire chain
            }
        }

        return currentContext
    }
    
    /**
     * Execute all finalisers in order.
     * 
     * T074: Execute finalisers after handler
     * 
     * Finalisers are always executed, even if handler failed.
     * 
     * @param context Handler context
     * @param event Event that was processed
     * @param result Result event from handler (null if handler failed)
     * @param error Exception if handler failed (null if successful)
     */
    fun executeFinalisers(context: HandlerContext, event: BaseEvent, result: BaseEvent?, error: Throwable?) {
        finalisers.forEach { finaliser ->
            try {
                finaliser(context, event, result, error)
                logger.trace("Finaliser executed successfully")
            } catch (e: Exception) {
                logger.error("Finaliser failed: ${e.message}", e)
                // Continue with remaining finalisers - don't fail the chain
            }
        }
    }
    
    /**
     * Get count of registered interceptors.
     */
    fun getInterceptorCount(): Int = interceptors.size
    
    /**
     * Get count of registered finalisers.
     */
    fun getFinalisersCount(): Int = finalisers.size
    
    /**
     * Clear all interceptors and finalisers.
     * Useful for testing.
     */
    fun clear() {
        interceptors.clear()
        finalisers.clear()
        logger.debug("Cleared all interceptors and finalisers")
    }
}

