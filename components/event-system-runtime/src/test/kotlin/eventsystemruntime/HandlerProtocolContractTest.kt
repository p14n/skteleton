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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Contract tests for handler protocol integration.
 * 
 * T093-T095: Verify that the event system runtime correctly integrates
 * with the handler protocol (IHandler interface).
 * 
 * These tests verify:
 * - IHandler methods are called in correct order (lookup → operate → write)
 * - HandlerContext is properly constructed and passed
 * - Output events are correctly routed
 * - Handler metadata is respected
 */
@ExtendWith(VertxExtension::class)
class HandlerProtocolContractTest {
    
    /**
     * T093: Test IHandler method invocation order contract.
     * 
     * Given: An IHandler implementation
     * When: An event is routed to the handler
     * Then: Methods are called in order: lookup → operate → write
     */
    @Test
    fun `test IHandler method invocation order contract`(testContext: VertxTestContext) {
        // Arrange
        val invocationOrder = mutableListOf<String>()
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                invocationOrder.add("lookup")
                return LookupData()
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                invocationOrder.add("operate")
                return event
            }
            
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                invocationOrder.add("write")
                latch.countDown()
                return event
            }
            
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
        
        val runtime = initialize(systemDef, RuntimeConfig())
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "test.event",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("test-channel", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should complete")
        assertEquals(listOf("lookup", "operate", "write"), invocationOrder)
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
    
    /**
     * T094: Test HandlerContext contract.
     * 
     * Given: An IHandler implementation
     * When: Handler methods are invoked
     * Then: HandlerContext contains correct metadata and is consistent across methods
     */
    @Test
    fun `test HandlerContext contract`(testContext: VertxTestContext) {
        // Arrange
        val contexts = mutableListOf<HandlerContext>()
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                contexts.add(context)
                return LookupData()
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                contexts.add(context)
                return event
            }
            
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                contexts.add(context)
                latch.countDown()
                return event
            }
            
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
        
        val runtime = initialize(systemDef, RuntimeConfig())
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "test.event",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("test-channel", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should complete")
        assertEquals(3, contexts.size, "Should have 3 contexts (lookup, operate, write)")
        
        // All contexts should have same metadata
        val firstContext = contexts[0]
        contexts.forEach { context ->
            assertEquals(firstContext.metadata, context.metadata)
        }
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
    
    /**
     * T095: Test handler metadata contract.
     * 
     * Given: Handlers with specific metadata (receives/returns)
     * When: Events are published
     * Then: Only handlers that receive the event type are invoked
     */
    @Test
    fun `test handler metadata contract`(testContext: VertxTestContext) {
        // Arrange
        var handler1Invoked = false
        var handler2Invoked = false
        val latch = CountDownLatch(1)
        
        val handler1 = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                handler1Invoked = true
                latch.countDown()
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("order.placed"),
                returns = setOf()
            )
        }
        
        val handler2 = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                handler2Invoked = true
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("order.cancelled"), // Different event type
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("orders", "Order events")
            .addEvent("order.placed", setOf("orders"))
            .addEvent("order.cancelled", setOf("orders"))
            .addHandler(handler1, "PlacedHandler")
            .addHandler(handler2, "CancelledHandler")
            .build()
        
        val runtime = initialize(systemDef, RuntimeConfig())
        
        // Act - publish order.placed event
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("orders", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler1 should be invoked")
        assertTrue(handler1Invoked, "Handler1 should be invoked (receives order.placed)")
        assertFalse(handler2Invoked, "Handler2 should NOT be invoked (receives order.cancelled)")
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
}

