package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventsystemruntime.config.DataSourceConfig
import eventprotocol.*
import systemdefinition.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.extension.ExtendWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

/**
 * Tests for persistent channel functionality (User Story 3).
 *
 * These tests verify that events published to persistent channels are:
 * - Stored durably (via postevent system)
 * - Delivered to subscribed handlers
 * - Processed with transactional context
 *
 * Uses embedded PostgreSQL for testing database-backed persistent events.
 */
@ExtendWith(VertxExtension::class)
class PersistentChannelTest {

    private lateinit var runtime: EventSystemRuntime
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var datasourceConfig: DataSourceConfig

    @BeforeEach
    fun setup() {
        // Start embedded PostgreSQL
        embeddedPostgres = EmbeddedPostgres.builder().start()

        // Create datasource configuration
        datasourceConfig = DataSourceConfig(
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres"),
            username = "postgres",
            password = "postgres"
        )
    }

    @AfterEach
    fun teardown() {
        runBlocking {
            if (::runtime.isInitialized) {
                runtime.shutdown()
            }
        }

        // Stop embedded PostgreSQL
        if (::embeddedPostgres.isInitialized) {
            embeddedPostgres.close()
        }
    }
    
    /**
     * T051: Test persistent channel publishing and delivery.
     * 
     * Given: A runtime with handlers subscribed to a persistent channel
     * When: An event is published to that channel
     * Then: The event is delivered to all subscribed handlers
     */
    @Test
    fun `test persistent channel publishing and delivery`(testContext: VertxTestContext) {
        // Arrange
        val handlerExecuted = AtomicInteger(0)
        val latch = CountDownLatch(1)
        
        val handler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent) = LookupData()
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                handlerExecuted.incrementAndGet()
                latch.countDown()
                return event
            }
            override fun write(context: HandlerContext, event: BaseEvent) = event
            override fun operatorMeta() = HandlerMetadata(
                receives = setOf("order.placed"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("order_events", "Order events")
            .addEvent("order.placed", setOf("order_events"))
            .addHandler(handler, "OrderHandler")
            .build()

        val config = RuntimeConfig(
            persistentChannels = setOf("order_events"),
            datasource = datasourceConfig
        )
        
        runtime = initialize(systemDef, config)
        
        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = mapOf("orderId" to "order-123", "amount" to 99.99)
        )
        
        runBlocking {
            runtime.publish("order_events", event)
        }
        
        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Handler should execute within 2 seconds")
        assertEquals(1, handlerExecuted.get())
        testContext.completeNow()
    }
    
    /**
     * T052: Test transactional context in persistent handlers.
     * 
     * Given: A handler subscribed to a persistent channel
     * When: An event is delivered
     * Then: The handler receives a context with database connection metadata
     */
    @Test
    fun `test transactional context in persistent handlers`(testContext: VertxTestContext) {
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
                receives = setOf("order.placed"),
                returns = setOf()
            )
        }
        
        val systemDef = SystemDefinition.Builder()
            .addChannel("order_events", "Order events")
            .addEvent("order.placed", setOf("order_events"))
            .addHandler(handler, "OrderHandler")
            .build()

        val config = RuntimeConfig(
            persistentChannels = setOf("order_events"),
            datasource = datasourceConfig
        )

        runtime = initialize(systemDef, config)

        // Act
        val event = BaseEvent(
            eventId = UUID.randomUUID().toString(),
            type = "order.placed",
            data = mapOf("orderId" to "order-123")
        )

        runBlocking {
            runtime.publish("order_events", event)
        }
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Handler should execute within 5 seconds")
        assertNotNull(receivedContext)
        // With database-backed implementation, connection should be present in metadata
        assertTrue(receivedContext!!.metadata.containsKey("connection"), "Context should contain database connection")
        testContext.completeNow()
    }
}

