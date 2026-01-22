package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventprotocol.*
import systemdefinition.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.extension.ExtendWith
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for transient event routing (User Story 2).
 * 
 * These tests verify that events published to transient channels are routed
 * to subscribed handlers via Vert.x EventBus.
 */
@ExtendWith(VertxExtension::class)
class TransientRoutingTest {
    
    private lateinit var vertx: Vertx
    private lateinit var runtime: EventSystemRuntime
    
    @AfterEach
    fun teardown(testContext: VertxTestContext) {
        if (::runtime.isInitialized) {
            runBlocking {
                runtime.shutdown()
            }
        }
        if (::vertx.isInitialized) {
            vertx.close().onComplete { testContext.completeNow() }
        } else {
            testContext.completeNow()
        }
    }
    
    /**
     * T022: Test single handler routing.
     * 
     * Given: An event published to a transient channel
     * When: The event bus routes it
     * Then: The handler registered for that event type is invoked
     */
    @Test
    fun `test single handler routing`(testContext: VertxTestContext) {
        // Arrange
        val handlerInvoked = AtomicInteger(0)
        val handler = createCountingHandler("user.created", "email.send", handlerInvoked)
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addEvent("user.created", setOf("user-events"))
            .addHandler(handler, "UserCreatedHandler")
            .build()
        
        runtime = initialize(systemDef, RuntimeConfig())

        // Act - Publish event via runtime
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "user.created",
            data = mapOf("userId" to "123", "email" to "test@example.com")
        )

        runBlocking {
            runtime.publish("user-events", event)
        }

        // Assert - Handler should be invoked
        testContext.verify {
            // Give some time for async processing
            Thread.sleep(100)
            assertEquals(1, handlerInvoked.get(), "Handler should be invoked once")
            testContext.completeNow()
        }
    }
    
    /**
     * T023: Test multiple handlers on same event type.
     * 
     * Given: Multiple handlers subscribed to the same event type on the same channel
     * When: An event is published
     * Then: All handlers receive and process the event
     */
    @Test
    fun `test multiple handlers on same event type`(testContext: VertxTestContext) {
        // Arrange
        val handler1Invoked = AtomicInteger(0)
        val handler2Invoked = AtomicInteger(0)
        
        val handler1 = createCountingHandler("user.created", "email.send", handler1Invoked)
        val handler2 = createCountingHandler("user.created", "notification.send", handler2Invoked)
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addEvent("user.created", setOf("user-events"))
            .addHandler(handler1, "EmailHandler")
            .addHandler(handler2, "NotificationHandler")
            .build()
        
        runtime = initialize(systemDef, RuntimeConfig())

        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "user.created",
            data = mapOf("userId" to "123")
        )

        runBlocking {
            runtime.publish("user-events", event)
        }

        // Assert
        testContext.verify {
            Thread.sleep(100)
            assertEquals(1, handler1Invoked.get(), "Handler 1 should be invoked")
            assertEquals(1, handler2Invoked.get(), "Handler 2 should be invoked")
            testContext.completeNow()
        }
    }
    
    /**
     * T024: Test output event routing to configured channels.
     * 
     * Given: A handler that processes an event and returns output events
     * When: The handler completes
     * Then: Output events are published to their configured output channels
     */
    @Test
    fun `test output event routing to configured channels`(testContext: VertxTestContext) {
        // This test will be implemented when EventRouter.routeEvent() is complete
        testContext.completeNow()
    }
    
    /**
     * T025: Test correlation ID preservation.
     * 
     * Given: An event with a correlation ID
     * When: Routed through handlers
     * Then: The correlation ID is preserved in all output events
     */
    @Test
    fun `test correlation ID preservation`(testContext: VertxTestContext) {
        // This test will be implemented when correlation ID management is complete
        testContext.completeNow()
    }
    
    // Helper function to create counting handler
    private fun createCountingHandler(receives: String, returns: String, counter: AtomicInteger): IHandler {
        return object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                return LookupData()
            }

            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                counter.incrementAndGet()
                // Create a new event with the correct output type
                return event.deriveEvent(newType = returns)
            }

            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                return event
            }

            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(
                    receives = setOf(receives),
                    returns = setOf(returns)
                )
            }
        }
    }
}

