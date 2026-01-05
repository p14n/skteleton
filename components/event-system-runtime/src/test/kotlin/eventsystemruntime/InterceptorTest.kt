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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for interceptor and finaliser functionality (User Story 7).
 *
 * These tests verify that:
 * - Interceptors are invoked before handler execution
 * - Finalisers are invoked after handler execution
 * - Context can be modified by interceptors
 * - Finalisers execute even on handler failure
 */
@ExtendWith(VertxExtension::class)
class InterceptorTest {

    private lateinit var runtime: EventSystemRuntime

    @AfterEach
    fun teardown() {
        runBlocking {
            runtime.shutdown()
        }
    }

    /**
     * T066: Test interceptor invocation before handler.
     *
     * Given: An interceptor is registered
     * When: An event is processed
     * Then: The interceptor is invoked before the handler
     */
    @Test
    fun `test interceptor invocation before handler`(testContext: VertxTestContext) {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val latch = CountDownLatch(1)

        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                executionOrder.add("handler")
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

        runtime = initialize(systemDef, RuntimeConfig())

        // Register interceptor
        val eventRouter = runtime.getEventRouter()
        eventRouter.getInterceptorChain().registerInterceptor { context, event ->
            executionOrder.add("interceptor")
            context
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
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should execute within 2 seconds")
        assertEquals(listOf("interceptor", "handler"), executionOrder)
        testContext.completeNow()
    }

    /**
     * T067: Test finaliser invocation after handler.
     *
     * Given: A finaliser is registered
     * When: An event is processed
     * Then: The finaliser is invoked after the handler
     */
    @Test
    fun `test finaliser invocation after handler`(testContext: VertxTestContext) {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val handlerLatch = CountDownLatch(1)
        val finaliserLatch = CountDownLatch(1)

        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                executionOrder.add("handler")
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

        runtime = initialize(systemDef, RuntimeConfig())

        // Register finaliser
        val eventRouter = runtime.getEventRouter()
        eventRouter.getInterceptorChain().registerFinaliser { context, event, result, error ->
            executionOrder.add("finaliser")
            finaliserLatch.countDown()
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
        assertTrue(handlerLatch.await(2, TimeUnit.SECONDS), "Handler should execute within 2 seconds")
        assertTrue(finaliserLatch.await(2, TimeUnit.SECONDS), "Finaliser should execute within 2 seconds")
        assertEquals(listOf("handler", "finaliser"), executionOrder)
        testContext.completeNow()
    }

    /**
     * T068: Test context modification by interceptor.
     *
     * Given: An interceptor that modifies the context
     * When: An event is processed
     * Then: The handler receives the modified context
     */
    @Test
    fun `test context modification by interceptor`(testContext: VertxTestContext) {
        // Arrange
        var receivedContext: HandlerContext? = null
        val latch = CountDownLatch(1)

        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                receivedContext = context
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

        runtime = initialize(systemDef, RuntimeConfig())

        // Register interceptor that adds metadata
        val eventRouter = runtime.getEventRouter()
        eventRouter.getInterceptorChain().registerInterceptor { context, event ->
            context.copy(metadata = context.metadata + ("interceptor-key" to "interceptor-value"))
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
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should execute within 2 seconds")
        assertNotNull(receivedContext)
        assertEquals("interceptor-value", receivedContext!!.metadata["interceptor-key"])
        testContext.completeNow()
    }

    /**
     * T069: Test finaliser execution on handler failure.
     *
     * Given: A finaliser is registered and a handler that throws an exception
     * When: An event is processed
     * Then: The finaliser is still invoked with the error
     */
    @Test
    fun `test finaliser execution on handler failure`(testContext: VertxTestContext) {
        // Arrange
        var finaliserError: Throwable? = null
        val finaliserLatch = CountDownLatch(1)

        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                throw RuntimeException("Handler failed intentionally")
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

        runtime = initialize(systemDef, RuntimeConfig())

        // Register finaliser
        val eventRouter = runtime.getEventRouter()
        eventRouter.getInterceptorChain().registerFinaliser { context, event, result, error ->
            finaliserError = error
            finaliserLatch.countDown()
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
        assertTrue(finaliserLatch.await(2, TimeUnit.SECONDS), "Finaliser should execute within 2 seconds")
        assertNotNull(finaliserError)
        assertTrue(finaliserError is RuntimeException)
        assertEquals("Handler failed intentionally", finaliserError!!.message)
        testContext.completeNow()
    }
}

