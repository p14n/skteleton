package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Tests for circuit breaker functionality.
 * 
 * These tests verify that:
 * - Circuit breaker opens after N consecutive failures
 * - Circuit breaker fails fast when open
 * - Circuit breaker resets after timeout
 * - Circuit breaker transitions through states correctly
 */
class CircuitBreakerTest {
    
    private lateinit var circuitBreakerManager: CircuitBreakerManager
    
    @BeforeEach
    fun setup() {
        val config = RuntimeConfig(
            circuitBreakerMaxFailures = 3,
            circuitBreakerResetTimeoutMs = 1000,
            circuitBreakerTimeoutMs = 500
        )
        circuitBreakerManager = CircuitBreakerManager(config)
    }
    
    /**
     * T076: Test circuit breaker opening after N failures.
     * 
     * Given: A circuit breaker with maxFailures = 3
     * When: 3 consecutive operations fail
     * Then: The circuit breaker opens
     */
    @Test
    fun `test circuit breaker opens after N failures`() = runBlocking {
        val channelName = "test-channel"
        
        // Execute 3 failing operations
        repeat(3) { attempt ->
            try {
                circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                    throw RuntimeException("Simulated failure")
                }
                fail("Should have thrown exception")
            } catch (e: RuntimeException) {
                if (attempt < 2) {
                    // First 2 failures - circuit should still be closed
                    assertEquals(CircuitBreakerState.CLOSED, circuitBreakerManager.getState(channelName))
                }
            }
        }
        
        // After 3 failures, circuit breaker should be open
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerManager.getState(channelName))
    }
    
    /**
     * T078: Test fail-fast when circuit breaker is open.
     * 
     * Given: A circuit breaker that is open
     * When: An operation is attempted
     * Then: It fails immediately with CircuitBreakerOpenException
     */
    @Test
    fun `test fail-fast when circuit breaker is open`() = runBlocking {
        val channelName = "test-channel"
        
        // Open the circuit breaker by causing 3 failures
        repeat(3) {
            try {
                circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                    throw RuntimeException("Simulated failure")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerManager.getState(channelName))
        
        // Now attempt an operation - should fail fast
        try {
            circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                "This should not execute"
            }
            fail("Should have thrown CircuitBreakerOpenException")
        } catch (e: CircuitBreakerOpenException) {
            assertTrue(e.message!!.contains("Circuit breaker open"))
        }
    }
    
    /**
     * T077: Test circuit breaker reset after timeout.
     * 
     * Given: A circuit breaker that is open
     * When: The reset timeout elapses
     * Then: The circuit breaker transitions to HALF_OPEN
     * And: A successful operation closes it
     */
    @Test
    fun `test circuit breaker reset after timeout`() = runBlocking {
        val channelName = "test-channel"
        
        // Open the circuit breaker
        repeat(3) {
            try {
                circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                    throw RuntimeException("Simulated failure")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerManager.getState(channelName))
        
        // Wait for reset timeout (1000ms)
        Thread.sleep(1100)
        
        // Execute a successful operation - should transition OPEN -> HALF_OPEN -> CLOSED
        val result = circuitBreakerManager.executeWithCircuitBreaker(channelName) {
            "Success"
        }
        
        assertEquals("Success", result)
        assertEquals(CircuitBreakerState.CLOSED, circuitBreakerManager.getState(channelName))
    }
    
    /**
     * Test circuit breaker state transitions.
     * 
     * Given: A circuit breaker
     * When: Operations succeed and fail
     * Then: State transitions are correct: CLOSED -> OPEN -> HALF_OPEN -> CLOSED
     */
    @Test
    fun `test circuit breaker state transitions`() = runBlocking {
        val channelName = "test-channel"
        
        // Initial state: CLOSED
        assertEquals(CircuitBreakerState.CLOSED, circuitBreakerManager.getState(channelName))
        
        // Successful operation - stays CLOSED
        circuitBreakerManager.executeWithCircuitBreaker(channelName) { "Success" }
        assertEquals(CircuitBreakerState.CLOSED, circuitBreakerManager.getState(channelName))
        
        // 3 failures - transitions to OPEN
        repeat(3) {
            try {
                circuitBreakerManager.executeWithCircuitBreaker(channelName) {
                    throw RuntimeException("Failure")
                }
            } catch (e: Exception) { }
        }
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerManager.getState(channelName))
        
        // Wait for reset timeout
        Thread.sleep(1100)
        
        // Successful operation - transitions HALF_OPEN -> CLOSED
        circuitBreakerManager.executeWithCircuitBreaker(channelName) { "Success" }
        assertEquals(CircuitBreakerState.CLOSED, circuitBreakerManager.getState(channelName))
    }
}

