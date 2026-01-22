package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventsystemruntime.config.DataSourceConfig
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

/**
 * Tests for event publishing (User Story 6).
 * 
 * These tests verify that events can be published to channels and routed
 * to subscribed handlers.
 */
@ExtendWith(VertxExtension::class)
class PublishingTest {
    
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
     * T031: Test transient channel publishing.
     * 
     * Given: A runtime with handlers subscribed to a transient channel
     * When: An event is published to that channel
     * Then: The event is delivered to subscribed handlers
     */
    @Test
    fun `test transient channel publishing`(testContext: VertxTestContext) {
        // Arrange
        val handlerInvoked = AtomicInteger(0)
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData = LookupData()
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                handlerInvoked.incrementAndGet()
                latch.countDown()
                return event
            }
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("user.created"),
                returns = setOf("email.send")
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addEvent("user.created", setOf("user-events"))
            .addHandler(handler, "UserCreatedHandler")
            .build()
        
        runtime = initialize(systemDef, RuntimeConfig())
        
        // Act - Publish event using runtime.publish()
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "user.created",
            data = mapOf("userId" to "123", "email" to "test@example.com")
        )
        
        runBlocking {
            runtime.publish("user-events", event)
        }
        
        // Assert - Handler should be invoked
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should be invoked within 2 seconds")
        assertEquals(1, handlerInvoked.get(), "Handler should be invoked once")
        testContext.completeNow()
    }
    
    /**
     * T032: Test persistent channel publishing.
     * 
     * Given: A runtime with handlers subscribed to a persistent channel
     * When: An event is published to that channel
     * Then: The event is stored in the database and delivered to handlers
     */
    @Test
    fun `test persistent channel publishing`(testContext: VertxTestContext) {
        // This test will be fully implemented when persistent channels are complete (US3)
        // For now, just verify that publish() doesn't throw for persistent channels

        // Arrange - Start embedded PostgreSQL
        val embeddedPostgres = EmbeddedPostgres.builder().start()

        try {
            val handler = object : IHandler {
                override fun lookup(context: HandlerContext, event: BaseEvent): LookupData = LookupData()
                override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent = event
                override fun write(context: HandlerContext, event: BaseEvent): BaseEvent = event
                override fun operatorMeta() = HandlerMetadata(
                    receives = setOf("order.placed"),
                    returns = setOf("order.confirmed")
                )
            }

            val systemDef = SystemDefinition.Builder()
                .addChannel("order-events", "Order events")
                .addEvent("order.placed", setOf("order-events"))
                .addHandler(handler, "OrderHandler")
                .build()

            val datasource = DataSourceConfig(
                jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres"),
                username = "postgres",
                password = "postgres"
            )

            runtime = initialize(systemDef, RuntimeConfig(
                persistentChannels = setOf("order-events"),
                datasource = datasource
            ))

            // Act - Should not throw
            val event = BaseEvent(
                eventId = UUID.randomUUID().toString(),
                type = "order.placed",
                data = mapOf("orderId" to "456")
            )

            runBlocking {
                runtime.publish("order-events", event)
            }

            testContext.completeNow()
        } finally {
            embeddedPostgres.close()
        }
    }
    
    /**
     * T033: Test JSON serialization during publishing.
     * 
     * Given: An event with complex data
     * When: Published to a channel
     * Then: The event is correctly serialized and deserialized
     */
    @Test
    fun `test JSON serialization during publishing`(testContext: VertxTestContext) {
        // For now, we're using direct object passing (not JSON)
        // This test will be implemented when JSON serialization is added
        testContext.completeNow()
    }
    
    /**
     * T034: Test non-existent channel error logging.
     * 
     * Given: A runtime initialized with specific channels
     * When: An event is published to a non-existent channel
     * Then: The error is logged but doesn't throw (best-effort delivery)
     */
    @Test
    fun `test non-existent channel error logging`(testContext: VertxTestContext) {
        // Arrange
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData = LookupData()
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent = event
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("user.created"),
                returns = setOf("email.send")
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addEvent("user.created", setOf("user-events"))
            .addHandler(handler, "UserCreatedHandler")
            .build()
        
        runtime = initialize(systemDef, RuntimeConfig())
        
        // Act - Publish to non-existent channel (should not throw)
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "user.created",
            data = mapOf("userId" to "123")
        )

        assertDoesNotThrow {
            runBlocking {
                runtime.publish("non-existent-channel", event)
            }
        }

        testContext.completeNow()
    }
}

