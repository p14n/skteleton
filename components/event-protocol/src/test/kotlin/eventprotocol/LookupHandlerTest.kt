package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for LookupHandler (User Story 3).
 * 
 * These tests verify that LookupHandler correctly implements event processing
 * with database lookup before operation (no write phase).
 */
class LookupHandlerTest {
    
    @Test
    fun `test LookupHandler lookup invokes looker-upper function`() {
        // Arrange
        var lookerUpperCalled = false
        val expectedLookupData = mapOf("existingUser" to "user-123", "role" to "admin")
        
        val lookerUpper: LookerUpperFunction = { context, event ->
            lookerUpperCalled = true
            assertEquals("req-001", context.requestId)
            assertEquals("user.check", event.type)
            LookupData(data = expectedLookupData)
        }
        
        val operator: OperatorFunction = { _, event, _ -> event }
        val metadata = HandlerMetadata(
            receives = setOf("user.check"),
            returns = setOf("user.checked")
        )
        
        val handler = LookupHandler(lookerUpper, operator, metadata)
        val context = HandlerContext(requestId = "req-001")
        val event = BaseEvent(
            eventId = "evt-001",
            type = "user.check",
            data = mapOf("email" to "user@example.com")
        )
        
        // Act
        val result = handler.lookup(context, event)
        
        // Assert
        assertTrue(lookerUpperCalled, "looker-upper function should have been called")
        assertEquals(expectedLookupData, result.data)
    }
    
    @Test
    fun `test LookupHandler operate receives lookup data`() {
        // Arrange
        val lookupData = mapOf("existingUser" to "user-456", "status" to "active")
        var receivedLookupData: LookupData? = null
        
        val lookerUpper: LookerUpperFunction = { _, _ ->
            LookupData(data = lookupData)
        }
        
        val operator: OperatorFunction = { context, event, data ->
            receivedLookupData = data
            assertEquals("req-002", context.requestId)
            
            // Use lookup data to enrich event
            val existingUser = data.data["existingUser"]
            event.copy(
                data = event.data + mapOf("userExists" to (existingUser != null))
            )
        }
        
        val metadata = HandlerMetadata(
            receives = setOf("user.check"),
            returns = setOf("user.checked")
        )
        
        val handler = LookupHandler(lookerUpper, operator, metadata)
        val context = HandlerContext(requestId = "req-002")
        val event = BaseEvent(
            eventId = "evt-002",
            type = "user.check",
            data = mapOf("email" to "user@example.com")
        )
        
        // Act
        val lookupResult = handler.lookup(context, event)
        val operateResult = handler.operate(context, event, lookupResult)
        
        // Assert
        assertNotNull(receivedLookupData)
        assertEquals(lookupData, receivedLookupData?.data)
        assertTrue(operateResult.data.containsKey("userExists"))
        assertEquals(true, operateResult.data["userExists"])
    }
    
    @Test
    fun `test LookupHandler write returns event unchanged`() {
        // Arrange
        val lookerUpper: LookerUpperFunction = { _, _ -> LookupData() }
        val operator: OperatorFunction = { _, event, _ -> event }
        val metadata = HandlerMetadata(
            receives = setOf("test.event"),
            returns = setOf("test.event")
        )
        
        val handler = LookupHandler(lookerUpper, operator, metadata)
        val context = HandlerContext(requestId = "req-003")
        val event = BaseEvent(
            eventId = "evt-003",
            type = "test.event",
            data = mapOf("key" to "value", "status" to "processed")
        )
        
        // Act
        val result = handler.write(context, event)
        
        // Assert
        assertEquals(event, result, "LookupHandler write should return event unchanged")
        assertEquals("evt-003", result.eventId)
        assertEquals("test.event", result.type)
        assertEquals(mapOf("key" to "value", "status" to "processed"), result.data)
    }
    
    @Test
    fun `test LookupHandler operatorMeta returns metadata`() {
        // Arrange
        val expectedMetadata = HandlerMetadata(
            receives = setOf("duplicate.check", "validation.check"),
            returns = setOf("check.result"),
            description = "Duplicate detection handler"
        )
        
        val lookerUpper: LookerUpperFunction = { _, _ -> LookupData() }
        val operator: OperatorFunction = { _, event, _ -> event }
        val handler = LookupHandler(lookerUpper, operator, expectedMetadata)
        
        // Act
        val metadata = handler.operatorMeta()
        
        // Assert
        assertEquals(expectedMetadata, metadata)
        assertEquals(expectedMetadata.receives, metadata.receives)
        assertEquals(expectedMetadata.returns, metadata.returns)
        assertEquals(expectedMetadata.description, metadata.description)
    }

    @Test
    fun `test LookupHandler full pipeline through Executor`() {
        // Arrange
        val callOrder = mutableListOf<String>()

        // Simulate database lookup
        val lookerUpper: LookerUpperFunction = { context, event ->
            callOrder.add("lookup")
            val email = event.data["email"] as String

            // Simulate checking if user exists in database
            val existingUser = if (email == "existing@example.com") {
                mapOf("id" to "user-123", "email" to email)
            } else {
                null
            }

            LookupData(data = mapOf("existingUser" to existingUser))
        }

        val operator: OperatorFunction = { context, event, data ->
            callOrder.add("operate")
            val existingUser = data.data["existingUser"]

            // Enrich event with duplicate check result
            event.copy(
                data = event.data + mapOf(
                    "isDuplicate" to (existingUser != null),
                    "checkedAt" to System.currentTimeMillis()
                )
            )
        }

        val metadata = HandlerMetadata(
            receives = setOf("user.register"),
            returns = setOf("user.checked")
        )

        val handler = LookupHandler(lookerUpper, operator, metadata)
        val executor = Executor(handler)
        val context = HandlerContext(requestId = "req-integration")

        // Test with existing user
        val existingUserEvent = BaseEvent(
            eventId = "evt-existing",
            type = "user.register",
            data = mapOf("email" to "existing@example.com")
        )

        // Act
        val result1 = executor.execute(context, existingUserEvent)

        // Assert
        assertEquals(listOf("lookup", "operate"), callOrder)
        assertEquals("evt-existing", result1.eventId)
        assertEquals("user.register", result1.type)
        assertEquals(true, result1.data["isDuplicate"])
        assertTrue(result1.data.containsKey("checkedAt"))

        // Test with new user
        callOrder.clear()
        val newUserEvent = BaseEvent(
            eventId = "evt-new",
            type = "user.register",
            data = mapOf("email" to "new@example.com")
        )

        val result2 = executor.execute(context, newUserEvent)

        // Assert
        assertEquals(listOf("lookup", "operate"), callOrder)
        assertEquals("evt-new", result2.eventId)
        assertEquals(false, result2.data["isDuplicate"])
    }
}


