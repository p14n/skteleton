package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Circuit breaker state.
 */
enum class CircuitBreakerState {
    CLOSED,      // Normal operation
    OPEN,        // Failing fast, not attempting operations
    HALF_OPEN    // Testing if service recovered
}

/**
 * Manages circuit breakers for persistent channels.
 * 
 * Implements the circuit breaker pattern to prevent cascading failures
 * when database operations fail repeatedly.
 * 
 * T082-T084: Circuit breaker implementation
 * FR-013: Open circuit breaker after N consecutive failures
 * FR-014: Fail fast when circuit breaker is open
 * FR-015: Reset circuit breaker after timeout
 */
class CircuitBreakerManager(
    private val config: RuntimeConfig
) {
    private val logger = LoggerFactory.getLogger(CircuitBreakerManager::class.java)
    
    // Circuit breaker state per channel
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    
    /**
     * Execute an operation with circuit breaker protection.
     * 
     * @param channelName Channel name for circuit breaker isolation
     * @param operation Operation to execute
     * @return Result of operation
     * @throws CircuitBreakerOpenException if circuit breaker is open
     */
    suspend fun <T> executeWithCircuitBreaker(channelName: String, operation: suspend () -> T): T {
        val breaker = circuitBreakers.computeIfAbsent(channelName) {
            CircuitBreaker(channelName, config)
        }
        
        return breaker.execute(operation)
    }
    
    /**
     * Get current state of circuit breaker for a channel.
     */
    fun getState(channelName: String): CircuitBreakerState {
        return circuitBreakers[channelName]?.getState() ?: CircuitBreakerState.CLOSED
    }
    
    /**
     * Get all circuit breaker states.
     */
    fun getAllStates(): Map<String, CircuitBreakerState> {
        return circuitBreakers.mapValues { it.value.getState() }
    }
    
    /**
     * Reset circuit breaker for a channel (for testing).
     */
    fun reset(channelName: String) {
        circuitBreakers[channelName]?.reset()
    }
    
    /**
     * Reset all circuit breakers (for testing).
     */
    fun resetAll() {
        circuitBreakers.values.forEach { it.reset() }
    }
}

/**
 * Individual circuit breaker for a channel.
 */
private class CircuitBreaker(
    private val channelName: String,
    private val config: RuntimeConfig
) {
    private val logger = LoggerFactory.getLogger(CircuitBreaker::class.java)
    
    @Volatile
    private var state: CircuitBreakerState = CircuitBreakerState.CLOSED
    
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    
    suspend fun <T> execute(operation: suspend () -> T): T {
        // Check if circuit breaker should transition states
        checkStateTransition()
        
        // FR-014: Fail fast if circuit breaker is open
        if (state == CircuitBreakerState.OPEN) {
            logger.warn("Circuit breaker OPEN for channel $channelName - failing fast")
            throw CircuitBreakerOpenException("Circuit breaker open for channel: $channelName")
        }
        
        return try {
            val result = operation()
            
            // Success - reset failure count
            if (state == CircuitBreakerState.HALF_OPEN) {
                logger.info("Circuit breaker HALF_OPEN -> CLOSED for channel $channelName (operation succeeded)")
                state = CircuitBreakerState.CLOSED
            }
            failureCount.set(0)
            
            result
        } catch (e: Exception) {
            handleFailure(e)
            throw e
        }
    }
    
    private fun handleFailure(e: Exception) {
        val failures = failureCount.incrementAndGet()
        lastFailureTime.set(System.currentTimeMillis())
        
        logger.error("Operation failed for channel $channelName (failure $failures/${config.circuitBreakerMaxFailures}): ${e.message}")
        
        // FR-013: Open circuit breaker after N consecutive failures
        if (failures >= config.circuitBreakerMaxFailures && state == CircuitBreakerState.CLOSED) {
            logger.warn("Circuit breaker CLOSED -> OPEN for channel $channelName (max failures reached)")
            state = CircuitBreakerState.OPEN
        }
    }
    
    private fun checkStateTransition() {
        if (state == CircuitBreakerState.OPEN) {
            val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get()
            
            // FR-015: Reset circuit breaker after timeout
            if (timeSinceLastFailure >= config.circuitBreakerResetTimeoutMs) {
                logger.info("Circuit breaker OPEN -> HALF_OPEN for channel $channelName (reset timeout elapsed)")
                state = CircuitBreakerState.HALF_OPEN
                failureCount.set(0)
            }
        }
    }
    
    fun getState(): CircuitBreakerState = state
    
    fun reset() {
        state = CircuitBreakerState.CLOSED
        failureCount.set(0)
        lastFailureTime.set(0)
    }
}

/**
 * Exception thrown when circuit breaker is open.
 */
class CircuitBreakerOpenException(message: String) : RuntimeException(message)

