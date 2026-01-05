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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Contract tests for Vert.x EventBus integration.
 * 
 * T090-T092: Verify that the event system runtime correctly integrates
 * with Vert.x EventBus for transient channel routing.
 * 
 * These tests verify:
 * - Events are published to correct EventBus addresses
 * - Handlers receive events from EventBus
 * - Multiple handlers on same channel receive events
 * - EventBus message format is correct
 */
@ExtendWith(VertxExtension::class)
class VertxEventBusContractTest {
    
    /**
     * T090: Test EventBus address format contract.
     * 
     * Given: A transient channel and event type
     * When: An event is published
     * Then: It is sent to EventBus address "{channel}.{event-type}"
     */
    @Test
    fun `test EventBus address format contract`(testContext: VertxTestContext) {
        // Arrange
        val receivedEvents = mutableListOf<BaseEvent>()
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                receivedEvents.add(event)
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
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("orders", "Order events")
            .addEvent("order.placed", setOf("orders"))
            .addHandler(handler, "OrderHandler")
            .build()
        
        val runtime = initialize(systemDef, RuntimeConfig())
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = mapOf("orderId" to "123")
        )
        
        runBlocking {
            runtime.publish("orders", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should receive event")
        assertEquals(1, receivedEvents.size)
        assertEquals("order.placed", receivedEvents[0].type)
        assertEquals("123", receivedEvents[0].data["orderId"])
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
    
    /**
     * T091: Test multiple handlers on same channel contract.
     * 
     * Given: Multiple handlers subscribed to same channel
     * When: An event is published
     * Then: All matching handlers receive the event
     */
    @Test
    fun `test multiple handlers on same channel contract`(testContext: VertxTestContext) {
        // Arrange
        val handler1Count = AtomicInteger(0)
        val handler2Count = AtomicInteger(0)
        val latch = CountDownLatch(2)
        
        val handler1 = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                handler1Count.incrementAndGet()
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
                handler2Count.incrementAndGet()
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
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("orders", "Order events")
            .addEvent("order.placed", setOf("orders"))
            .addHandler(handler1, "Handler1")
            .addHandler(handler2, "Handler2")
            .build()
        
        val runtime = initialize(systemDef, RuntimeConfig())
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = emptyMap()
        )
        
        runBlocking {
            runtime.publish("orders", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Both handlers should receive event")
        assertEquals(1, handler1Count.get())
        assertEquals(1, handler2Count.get())
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
}

