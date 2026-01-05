package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventprotocol.*
import systemdefinition.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.extension.ExtendWith
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for correlation ID tracking (User Story 5).
 * 
 * These tests verify that correlation IDs are:
 * - Auto-generated when missing
 * - Propagated to output events
 * - Maintained across multi-step event flows
 */
@ExtendWith(VertxExtension::class)
class CorrelationIdTest {
    
    private lateinit var runtime: EventSystemRuntime
    
    @AfterEach
    fun teardown() {
        runBlocking {
            runtime.shutdown()
        }
    }
    
    /**
     * T059: Test correlation ID auto-generation.
     * 
     * Given: An event published without a correlation ID
     * When: The event is published
     * Then: A correlation ID is automatically generated
     */
    @Test
    fun `test correlation ID auto-generation`(testContext: VertxTestContext) {
        // Arrange
        var receivedEvent: BaseEvent? = null
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                receivedEvent = event
                latch.countDown()
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
        
        runtime = initialize(systemDef, RuntimeConfig(enableCorrelationIdGeneration = true))
        
        // Act - publish event WITHOUT correlation ID
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "test.event",
            data = emptyMap(),
            correlationId = null  // No correlation ID
        )
        
        runBlocking {
            runtime.publish("test-channel", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should execute within 2 seconds")
        assertNotNull(receivedEvent)
        assertNotNull(receivedEvent!!.correlationId, "Correlation ID should be auto-generated")
        testContext.completeNow()
    }
    
    /**
     * T060: Test correlation ID propagation to output events.
     * 
     * Given: A handler that produces output events
     * When: The handler executes with an input event containing a correlation ID
     * Then: The output events inherit the correlation ID
     */
    @Test
    fun `test correlation ID propagation to output events`(testContext: VertxTestContext) {
        // Arrange
        val inputCorrelationId = UUID.randomUUID().toString()
        var outputEvent: BaseEvent? = null
        val latch = CountDownLatch(1)
        
        // Producer handler
        val producer = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent) = LookupData()
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                // Produce output event WITHOUT correlation ID
                return BaseEvent(
                    eventId = UUID.randomUUID().toString(),
                    type = "output.event",
                    data = emptyMap(),
                    correlationId = null  // No correlation ID set
                )
            }
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("input.event"),
                returns = setOf("output.event")
            )
        }
        
        // Consumer handler
        val consumer = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                outputEvent = event
                latch.countDown()
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("output.event"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("input-channel", "Input channel")
            .addChannel("output-channel", "Output channel")
            .addEvent("input.event", setOf("input-channel"))
            .addEvent("output.event", setOf("output-channel"))
            .addHandler(producer, "Producer")
            .addHandler(consumer, "Consumer")
            .build()
        
        runtime = initialize(systemDef, RuntimeConfig())
        
        // Act - publish event WITH correlation ID
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "input.event",
            data = emptyMap(),
            correlationId = inputCorrelationId
        )
        
        runBlocking {
            runtime.publish("input-channel", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Consumer should execute within 2 seconds")
        assertNotNull(outputEvent)
        assertEquals(inputCorrelationId, outputEvent!!.correlationId, 
            "Output event should inherit correlation ID from input event")
        testContext.completeNow()
    }
}

