package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for LookupWriterHandler (User Story 4).
 * 
 * These tests verify that LookupWriterHandler correctly implements the full
 * three-phase pipeline with lookup, operate, and write functionality.
 */
class LookupWriterHandlerTest {
    
    @Test
    fun `test LookupWriterHandler lookup invokes looker-upper function`() {
        // Arrange
        var lookerUpperCalled = false
        val expectedLookupData = mapOf("user" to "user-123")
        
        val lookerUpper: LookerUpperFunction = { context, event ->
            lookerUpperCalled = true
            assertEquals("req-001", context.requestId)
            assertEquals("user.created", event.type)
            LookupData(data = expectedLookupData)
        }
        
        val operator: OperatorFunction = { _, event, _ -> event }
        val writer: WriterFunction = { _, event -> event }
        val metadata = HandlerMetadata(
            receives = setOf("user.created"),
            returns = setOf("user.created")
        )
        
        val handler = LookupWriterHandler(lookerUpper, operator, writer, metadata)
        val context = HandlerContext(requestId = "req-001")
        val event = BaseEvent(
            eventId = "evt-001",
            type = "user.created",
            data = emptyMap()
        )
        
        // Act
        val result = handler.lookup(context, event)
        
        // Assert
        assertTrue(lookerUpperCalled, "looker-upper function should have been called")
        assertEquals(expectedLookupData, result.data)
    }
    
    @Test
    fun `test LookupWriterHandler operate receives lookup data`() {
        // Arrange
        val lookupData = mapOf("existingUser" to "user-456")
        var receivedLookupData: LookupData? = null
        
        val lookerUpper: LookerUpperFunction = { _, _ ->
            LookupData(data = lookupData)
        }
        
        val operator: OperatorFunction = { context, event, data ->
            receivedLookupData = data
            assertEquals("req-002", context.requestId)
            event
        }
        
        val writer: WriterFunction = { _, event -> event }
        val metadata = HandlerMetadata(
            receives = setOf("user.created"),
            returns = setOf("user.created")
        )
        
        val handler = LookupWriterHandler(lookerUpper, operator, writer, metadata)
        val context = HandlerContext(requestId = "req-002")
        val event = BaseEvent(
            eventId = "evt-002",
            type = "user.created",
            data = emptyMap()
        )
        
        // Act
        val lookupResult = handler.lookup(context, event)
        handler.operate(context, event, lookupResult)
        
        // Assert
        assertNotNull(receivedLookupData)
        assertEquals(lookupData, receivedLookupData?.data)
    }
    
    @Test
    fun `test LookupWriterHandler write invokes writer function`() {
        // Arrange
        var writerCalled = false
        val enrichedEvent = BaseEvent(
            eventId = "evt-enriched",
            type = "user.created",
            data = mapOf("userId" to "user-789", "status" to "active")
        )
        
        val lookerUpper: LookerUpperFunction = { _, _ -> LookupData() }
        val operator: OperatorFunction = { _, event, _ -> event }
        val writer: WriterFunction = { context, event ->
            writerCalled = true
            assertEquals("req-003", context.requestId)
            assertEquals("evt-enriched", event.eventId)
            enrichedEvent
        }
        
        val metadata = HandlerMetadata(
            receives = setOf("user.created"),
            returns = setOf("user.created")
        )
        
        val handler = LookupWriterHandler(lookerUpper, operator, writer, metadata)
        val context = HandlerContext(requestId = "req-003")
        
        // Act
        val result = handler.write(context, enrichedEvent)
        
        // Assert
        assertTrue(writerCalled, "writer function should have been called")
        assertEquals(enrichedEvent, result)
    }
    
    @Test
    fun `test LookupWriterHandler operatorMeta returns metadata`() {
        // Arrange
        val expectedMetadata = HandlerMetadata(
            receives = setOf("order.placed", "order.updated"),
            returns = setOf("order.processed"),
            description = "Process orders"
        )
        
        val lookerUpper: LookerUpperFunction = { _, _ -> LookupData() }
        val operator: OperatorFunction = { _, event, _ -> event }
        val writer: WriterFunction = { _, event -> event }
        
        val handler = LookupWriterHandler(lookerUpper, operator, writer, expectedMetadata)
        
        // Act
        val metadata = handler.operatorMeta()
        
        // Assert
        assertEquals(expectedMetadata, metadata)
        assertEquals(expectedMetadata.receives, metadata.receives)
        assertEquals(expectedMetadata.returns, metadata.returns)
        assertEquals(expectedMetadata.description, metadata.description)
    }

    @Test
    fun `test full pipeline lookup to operate to write integration`() {
        // Arrange
        val callOrder = mutableListOf<String>()
        val lookupData = mapOf("existingUser" to null)

        val lookerUpper: LookerUpperFunction = { context, event ->
            callOrder.add("lookup")
            assertEquals("user.created", event.type)
            LookupData(data = lookupData)
        }

        val operator: OperatorFunction = { context, event, data ->
            callOrder.add("operate")
            assertEquals(lookupData, data.data)
            // Enrich event with lookup data
            event.copy(
                data = event.data + mapOf("validated" to true)
            )
        }

        val writer: WriterFunction = { context, event ->
            callOrder.add("write")
            assertTrue(event.data.containsKey("validated"))
            assertEquals(true, event.data["validated"])
            // Return event with write confirmation
            event.copy(
                data = event.data + mapOf("persisted" to true)
            )
        }

        val metadata = HandlerMetadata(
            receives = setOf("user.created"),
            returns = setOf("user.created")
        )

        val handler = LookupWriterHandler(lookerUpper, operator, writer, metadata)
        val context = HandlerContext(requestId = "req-integration")
        val event = BaseEvent(
            eventId = "evt-integration",
            type = "user.created",
            data = mapOf("email" to "user@example.com")
        )

        // Act - Execute full pipeline through Executor
        val executor = Executor(handler)
        val result = executor.execute(context, event)

        // Assert
        assertEquals(listOf("lookup", "operate", "write"), callOrder)
        assertEquals("evt-integration", result.eventId)
        assertEquals("user.created", result.type)
        assertTrue(result.data.containsKey("email"))
        assertTrue(result.data.containsKey("validated"))
        assertTrue(result.data.containsKey("persisted"))
        assertEquals("user@example.com", result.data["email"])
        assertEquals(true, result.data["validated"])
        assertEquals(true, result.data["persisted"])
    }
}


