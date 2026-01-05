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
import java.util.concurrent.ConcurrentHashMap

/**
 * End-to-end integration tests.
 * 
 * These tests verify complete workflows through the event system runtime,
 * including multi-step event flows, correlation ID propagation, and
 * interceptor/finaliser integration.
 */
@ExtendWith(VertxExtension::class)
class EndToEndIntegrationTest {
    
    /**
     * Test complete order processing workflow.
     * 
     * Given: A multi-step order processing system
     * When: An order.placed event is published
     * Then: All handlers execute in sequence with correlation ID propagation
     */
    @Test
    fun `test complete order processing workflow`(testContext: VertxTestContext) {
        // Arrange
        val executionLog = ConcurrentHashMap<String, MutableList<String>>()
        val latch = CountDownLatch(3) // 3 handlers in the flow
        
        // Handler 1: Process order placement
        val orderHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                executionLog.getOrPut("order") { mutableListOf() }.add("lookup")
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                executionLog.getOrPut("order") { mutableListOf() }.add("operate")
                return event
            }
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                executionLog.getOrPut("order") { mutableListOf() }.add("write")
                latch.countDown()
                // Produce payment.requested event
                return BaseEvent(
                    eventId = UUID.randomUUID().toString(),
                    type = "payment.requested",
                    data = mapOf("orderId" to event.data["orderId"], "amount" to 99.99),
                    correlationId = event.correlationId
                )
            }
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("order.placed"),
                returns = setOf("payment.requested")
            )
        }
        
        // Handler 2: Process payment
        val paymentHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                executionLog.getOrPut("payment") { mutableListOf() }.add("lookup")
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                executionLog.getOrPut("payment") { mutableListOf() }.add("operate")
                return event
            }
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                executionLog.getOrPut("payment") { mutableListOf() }.add("write")
                latch.countDown()
                // Produce notification.requested event
                return BaseEvent(
                    eventId = UUID.randomUUID().toString(),
                    type = "notification.requested",
                    data = mapOf("orderId" to event.data["orderId"], "message" to "Payment processed"),
                    correlationId = event.correlationId
                )
            }
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("payment.requested"),
                returns = setOf("notification.requested")
            )
        }
        
        // Handler 3: Send notification
        val notificationHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                executionLog.getOrPut("notification") { mutableListOf() }.add("lookup")
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                executionLog.getOrPut("notification") { mutableListOf() }.add("operate")
                return event
            }
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                executionLog.getOrPut("notification") { mutableListOf() }.add("write")
                latch.countDown()
                return event
            }
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("notification.requested"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("orders", "Order events")
            .addChannel("payments", "Payment events")
            .addChannel("notifications", "Notification events")
            .addEvent("order.placed", setOf("orders"))
            .addEvent("payment.requested", setOf("payments"))
            .addEvent("notification.requested", setOf("notifications"))
            .addHandler(orderHandler, "OrderHandler")
            .addHandler(paymentHandler, "PaymentHandler")
            .addHandler(notificationHandler, "NotificationHandler")
            .build()
        
        val runtime = initialize(systemDef, RuntimeConfig(enableCorrelationIdGeneration = true))
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = mapOf("orderId" to "ORD-123", "customerId" to "CUST-456")
        )
        
        runBlocking {
            runtime.publish("orders", event)
        }
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All handlers should complete")
        
        // Verify all handlers executed
        assertTrue(executionLog.containsKey("order"))
        assertTrue(executionLog.containsKey("payment"))
        assertTrue(executionLog.containsKey("notification"))
        
        // Verify execution order for each handler
        assertEquals(listOf("lookup", "operate", "write"), executionLog["order"])
        assertEquals(listOf("lookup", "operate", "write"), executionLog["payment"])
        assertEquals(listOf("lookup", "operate", "write"), executionLog["notification"])
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
    
    /**
     * Test interceptor and finaliser integration in workflow.
     * 
     * Given: Interceptors and finalisers are registered
     * When: Events flow through the system
     * Then: Interceptors modify context and finalisers execute for all handlers
     */
    @Test
    fun `test interceptor and finaliser integration`(testContext: VertxTestContext) {
        // Arrange
        val interceptorExecutions = mutableListOf<String>()
        val finaliserExecutions = mutableListOf<String>()
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                // Verify interceptor modified context
                assertTrue(context.metadata.containsKey("trace-id"))
                return LookupData()
            }
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
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
        
        // Register interceptor
        runtime.getEventRouter().getInterceptorChain().registerInterceptor { context, event ->
            interceptorExecutions.add("interceptor-${event.eventId}")
            context.copy(metadata = context.metadata + ("trace-id" to UUID.randomUUID().toString()))
        }
        
        // Register finaliser
        runtime.getEventRouter().getInterceptorChain().registerFinaliser { context, event, output, error ->
            finaliserExecutions.add("finaliser-${event.eventId}")
        }
        
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
        assertEquals(1, interceptorExecutions.size)
        assertEquals(1, finaliserExecutions.size)
        assertTrue(interceptorExecutions[0].startsWith("interceptor-"))
        assertTrue(finaliserExecutions[0].startsWith("finaliser-"))
        
        runBlocking { runtime.shutdown() }
        testContext.completeNow()
    }
}

