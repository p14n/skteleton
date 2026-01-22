package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import io.vertx.core.Vertx
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Coordinates graceful shutdown of the event system runtime.
 * 
 * Responsible for:
 * - Tracking in-flight handlers
 * - Waiting for handlers to complete within timeout
 * - Forcing shutdown after timeout
 * - Cleaning up resources
 * 
 * T083, T085: Graceful shutdown implementation
 * FR-020: Wait for in-flight handlers to complete
 * FR-021: Force shutdown after timeout
 * FR-022: Clean up resources
 */
class ShutdownCoordinator(
    private val config: RuntimeConfig,
    private val vertx: Vertx,
    private val scope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(ShutdownCoordinator::class.java)
    
    private val inFlightHandlers = AtomicInteger(0)
    
    @Volatile
    private var isShuttingDown = false
    
    /**
     * Track handler execution start.
     */
    fun handlerStarted() {
        inFlightHandlers.incrementAndGet()
    }
    
    /**
     * Track handler execution completion.
     */
    fun handlerCompleted() {
        inFlightHandlers.decrementAndGet()
    }
    
    /**
     * Get count of in-flight handlers.
     */
    fun getInFlightCount(): Int = inFlightHandlers.get()
    
    /**
     * Check if system is shutting down.
     */
    fun isShuttingDown(): Boolean = isShuttingDown
    
    /**
     * Perform graceful shutdown.
     * 
     * T085: FR-020-022 - Graceful shutdown with timeout
     * 
     * @return true if shutdown completed gracefully, false if forced
     */
    suspend fun shutdown(): Boolean {
        logger.info("Initiating graceful shutdown (timeout: ${config.shutdownTimeoutMs}ms)...")
        isShuttingDown = true
        
        // FR-020: Wait for in-flight handlers to complete
        val startTime = System.currentTimeMillis()
        var graceful = true
        
        while (inFlightHandlers.get() > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            
            // FR-021: Force shutdown after timeout
            if (elapsed >= config.shutdownTimeoutMs) {
                logger.warn("Shutdown timeout reached with ${inFlightHandlers.get()} handlers still in-flight - forcing shutdown")
                graceful = false
                break
            }
            
            logger.debug("Waiting for ${inFlightHandlers.get()} in-flight handlers to complete...")
            delay(100) // Check every 100ms
        }
        
        if (graceful) {
            logger.info("All handlers completed - proceeding with graceful shutdown")
        }
        
        // FR-022: Clean up resources
        cleanupResources()
        
        logger.info("Shutdown complete (graceful: $graceful)")
        return graceful
    }
    
    /**
     * Clean up resources.
     */
    private suspend fun cleanupResources() {
        logger.debug("Cleaning up resources...")
        
        // Cancel coroutine scope
        scope.cancel()
        
        // Close Vert.x instance
        withContext(Dispatchers.IO) {
            try {
                vertx.close().await()
                logger.debug("Vert.x instance closed")
            } catch (e: Exception) {
                logger.error("Error closing Vert.x: ${e.message}", e)
            }
        }
    }
}

/**
 * Extension function to await Vert.x Future.
 */
private suspend fun <T> io.vertx.core.Future<T>.await(): T = suspendCancellableCoroutine { cont ->
    this.onComplete { result ->
        if (result.succeeded()) {
            cont.resume(result.result()) {}
        } else {
            cont.resumeWithException(result.cause())
        }
    }
}

