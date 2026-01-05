package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventprotocol.*
import systemdefinition.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.extension.ExtendWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests for graceful shutdown functionality.
 * 
 * These tests verify that:
 * - System waits for in-flight handlers to complete
 * - System forces shutdown after timeout
 * - Resources are cleaned up properly
 */
@ExtendWith(VertxExtension::class)
class ShutdownTest {
    
    /**
     * T079: Test graceful shutdown within timeout.
     * 
     * Given: A handler is processing an event
     * When: Shutdown is initiated
     * Then: The system waits for the handler to complete
     */
    @Test
    fun `test graceful shutdown within timeout`(testContext: VertxTestContext) {
        // Arrange
        val handlerCompleted = AtomicBoolean(false)
        val handlerLatch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                runBlocking {
                    // Simulate work
                    delay(500)
                }
                handlerCompleted.set(true)
                handlerLatch.countDown()
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("test.event"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("test-channel", "Test channel")
            .addEvent("test.event", setOf("test-channel"))
            .addHandler(handler, "TestHandler")
            .build()
        
        val config = RuntimeConfig(shutdownTimeoutMs = 2000) // 2 second timeout
        val runtime = initialize(systemDef, config)
        
        // Act - publish event and immediately shutdown
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "test.event",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("test-channel", event)
            
            // Give handler time to start
            delay(100)
            
            // Shutdown - should wait for handler
            val graceful = runtime.shutdown()
            
            // Assert
            assertTrue(graceful, "Shutdown should be graceful")
            assertTrue(handlerCompleted.get(), "Handler should have completed")
        }
        
        testContext.completeNow()
    }
    
    /**
     * T080: Test forced shutdown after timeout.
     * 
     * Given: A handler is taking longer than shutdown timeout
     * When: Shutdown is initiated
     * Then: The system forces shutdown after timeout
     */
    @Test
    fun `test forced shutdown after timeout`(testContext: VertxTestContext) {
        // Arrange
        val handlerCompleted = AtomicBoolean(false)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                runBlocking {
                    // Simulate long-running work (longer than shutdown timeout)
                    delay(3000)
                }
                handlerCompleted.set(true)
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("test.event"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("test-channel", "Test channel")
            .addEvent("test.event", setOf("test-channel"))
            .addHandler(handler, "TestHandler")
            .build()
        
        val config = RuntimeConfig(shutdownTimeoutMs = 500) // Short timeout
        val runtime = initialize(systemDef, config)
        
        // Act - publish event and immediately shutdown
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "test.event",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("test-channel", event)
            
            // Give handler time to start
            delay(100)
            
            // Shutdown - should timeout and force shutdown
            val startTime = System.currentTimeMillis()
            val graceful = runtime.shutdown()
            val elapsed = System.currentTimeMillis() - startTime
            
            // Assert
            assertFalse(graceful, "Shutdown should be forced (not graceful)")
            assertTrue(elapsed < 1000, "Shutdown should complete quickly after timeout")
            assertFalse(handlerCompleted.get(), "Handler should not have completed")
        }
        
        testContext.completeNow()
    }
}

