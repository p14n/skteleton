package eventsystemruntime

import eventsystemruntime.config.RuntimeConfig
import eventsystemruntime.config.ValidationException
import eventsystemruntime.config.ConfigurationException
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

/**
 * Tests for EventSystemRuntime initialization (User Story 1).
 * 
 * These tests verify that the event system can be initialized from a SystemDefinition,
 * subscribing handlers to appropriate channels (transient or persistent).
 */
@ExtendWith(VertxExtension::class)
class EventSystemRuntimeTest {
    
    private lateinit var vertx: Vertx
    
    @BeforeEach
    fun setup() {
        vertx = Vertx.vertx()
    }
    
    @AfterEach
    fun teardown(testContext: VertxTestContext) {
        vertx.close().onComplete { testContext.completeNow() }
    }
    
    /**
     * T011: Test transient channel subscription.
     * 
     * Given: A SystemDefinition with handlers configured for transient channels
     * When: The event system is initialized
     * Then: Handlers are subscribed to the Vert.x event bus
     */
    @Test
    fun `test transient channel subscription`(testContext: VertxTestContext) {
        // Arrange
        val handler = createTestHandler("user.created", "email.send")
        val systemDef = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addEvent("user.created", setOf("user-events"))
            .addHandler(handler, "UserCreatedHandler")
            .build()
        
        val config = RuntimeConfig()
        
        // Act
        val runtime = initialize(systemDef, config)
        
        // Assert
        val status = runtime.getStatus()
        assertEquals(RuntimeState.RUNNING, status.state)
        assertTrue(status.subscribedChannels.contains("user-events"))
        
        testContext.completeNow()
    }
    
    /**
     * T012: Test persistent channel subscription.
     * 
     * Given: A SystemDefinition with handlers configured for persistent channels
     * When: The event system is initialized with datasource config
     * Then: Handlers are subscribed to the postevent system
     */
    @Test
    fun `test persistent channel subscription`(testContext: VertxTestContext) {
        // Arrange
        val handler = createTestHandler("order.placed", "order.confirmed")
        val systemDef = SystemDefinition.Builder()
            .addChannel("order-events", "Order events")
            .addEvent("order.placed", setOf("order-events"))
            .addHandler(handler, "OrderHandler")
            .build()
        
        val datasource = DataSourceConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5432/testdb",
            username = "test",
            password = "test"
        )
        val config = RuntimeConfig(
            persistentChannels = setOf("order-events"),
            datasource = datasource
        )
        
        // Act
        val runtime = initialize(systemDef, config)
        
        // Assert
        val status = runtime.getStatus()
        assertEquals(RuntimeState.RUNNING, status.state)
        assertTrue(status.subscribedChannels.contains("order-events"))
        
        testContext.completeNow()
    }
    
    /**
     * T013: Test SystemDefinition validation failure.
     * 
     * Given: A SystemDefinition with validation errors
     * When: Initialization is attempted
     * Then: The system throws a ValidationException
     */
    @Test
    fun `test SystemDefinition validation failure`() {
        // Arrange - empty system definition (no handlers)
        val systemDef = SystemDefinition.Builder().build()
        val config = RuntimeConfig()
        
        // Act & Assert
        assertThrows(ValidationException::class.java) {
            initialize(systemDef, config)
        }
    }
    
    /**
     * T014: Test missing datasource configuration error.
     *
     * Given: A SystemDefinition specifying persistent channels without datasource
     * When: RuntimeConfig is created
     * Then: The system throws an IllegalArgumentException
     */
    @Test
    fun `test missing datasource configuration error`() {
        // Arrange
        val handler = createTestHandler("order.placed", "order.confirmed")
        val systemDef = SystemDefinition.Builder()
            .addChannel("order-events", "Order events")
            .addEvent("order.placed", setOf("order-events"))
            .addHandler(handler, "OrderHandler")
            .build()

        // Act & Assert - RuntimeConfig constructor should throw
        assertThrows(IllegalArgumentException::class.java) {
            RuntimeConfig(
                persistentChannels = setOf("order-events")
                // Missing datasource!
            )
        }
    }
    
    // Helper function to create test handlers
    private fun createTestHandler(receives: String, returns: String): IHandler {
        return object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                return LookupData()
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                return event
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

