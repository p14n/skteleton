package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for SimpleHandler (User Story 2).
 * 
 * These tests verify that SimpleHandler correctly implements pure transformations
 * without database interaction (no lookup or write).
 */
class SimpleHandlerTest {
    
    @Test
    fun `test SimpleHandler lookup returns empty LookupData`() {
        // Arrange
        val operator: OperatorFunction = { _, event, _ -> event }
        val metadata = HandlerMetadata(
            receives = setOf("test.event"),
            returns = setOf("test.event")
        )
        
        val handler = SimpleHandler(operator, metadata)
        val context = HandlerContext(requestId = "req-001")
        val event = BaseEvent(
            eventId = "evt-001",
            type = "test.event",
            data = emptyMap()
        )
        
        // Act
        val result = handler.lookup(context, event)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.data.isEmpty(), "SimpleHandler lookup should return empty data")
    }
    
    @Test
    fun `test SimpleHandler operate invokes operator function`() {
        // Arrange
        var operatorCalled = false
        val enrichedData = mapOf("validated" to true, "timestamp" to 123456L)
        
        val operator: OperatorFunction = { context, event, data ->
            operatorCalled = true
            assertEquals("req-002", context.requestId)
            assertEquals("validation.event", event.type)
            assertTrue(data.data.isEmpty(), "SimpleHandler should pass empty lookup data")
            
            // Transform event
            event.copy(data = event.data + enrichedData)
        }
        
        val metadata = HandlerMetadata(
            receives = setOf("validation.event"),
            returns = setOf("validation.event")
        )
        
        val handler = SimpleHandler(operator, metadata)
        val context = HandlerContext(requestId = "req-002")
        val event = BaseEvent(
            eventId = "evt-002",
            type = "validation.event",
            data = mapOf("email" to "user@example.com")
        )
        
        // Act
        val result = handler.operate(context, event, LookupData())
        
        // Assert
        assertTrue(operatorCalled, "operator function should have been called")
        assertEquals("evt-002", result.eventId)
        assertEquals("validation.event", result.type)
        assertTrue(result.data.containsKey("email"))
        assertTrue(result.data.containsKey("validated"))
        assertTrue(result.data.containsKey("timestamp"))
    }
    
    @Test
    fun `test SimpleHandler write returns event unchanged`() {
        // Arrange
        val operator: OperatorFunction = { _, event, _ -> event }
        val metadata = HandlerMetadata(
            receives = setOf("test.event"),
            returns = setOf("test.event")
        )
        
        val handler = SimpleHandler(operator, metadata)
        val context = HandlerContext(requestId = "req-003")
        val event = BaseEvent(
            eventId = "evt-003",
            type = "test.event",
            data = mapOf("key" to "value")
        )
        
        // Act
        val result = handler.write(context, event)
        
        // Assert
        assertEquals(event, result, "SimpleHandler write should return event unchanged")
        assertEquals("evt-003", result.eventId)
        assertEquals("test.event", result.type)
        assertEquals(mapOf("key" to "value"), result.data)
    }
    
    @Test
    fun `test SimpleHandler operatorMeta returns metadata`() {
        // Arrange
        val expectedMetadata = HandlerMetadata(
            receives = setOf("email.validate", "email.format"),
            returns = setOf("email.validated"),
            description = "Email validation handler"
        )
        
        val operator: OperatorFunction = { _, event, _ -> event }
        val handler = SimpleHandler(operator, expectedMetadata)
        
        // Act
        val metadata = handler.operatorMeta()
        
        // Assert
        assertEquals(expectedMetadata, metadata)
        assertEquals(expectedMetadata.receives, metadata.receives)
        assertEquals(expectedMetadata.returns, metadata.returns)
        assertEquals(expectedMetadata.description, metadata.description)
    }
    
    @Test
    fun `test SimpleHandler full pipeline through Executor`() {
        // Arrange
        val operator: OperatorFunction = { context, event, data ->
            // Validate email format
            val email = event.data["email"] as? String
            require(email?.contains("@") == true) { "Invalid email format" }
            
            // Enrich event
            event.copy(
                data = event.data + mapOf(
                    "validated" to true,
                    "validatedAt" to System.currentTimeMillis()
                )
            )
        }
        
        val metadata = HandlerMetadata(
            receives = setOf("email.validate"),
            returns = setOf("email.validated")
        )
        
        val handler = SimpleHandler(operator, metadata)
        val executor = Executor(handler)
        val context = HandlerContext(requestId = "req-integration")
        val event = BaseEvent(
            eventId = "evt-integration",
            type = "email.validate",
            data = mapOf("email" to "test@example.com")
        )
        
        // Act
        val result = executor.execute(context, event)
        
        // Assert
        assertEquals("evt-integration", result.eventId)
        assertEquals("email.validate", result.type)
        assertEquals("test@example.com", result.data["email"])
        assertEquals(true, result.data["validated"])
        assertTrue(result.data.containsKey("validatedAt"))
    }
}

