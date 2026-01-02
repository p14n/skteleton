package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests to verify that quickstart.md examples work correctly.
 * 
 * These tests validate the code examples shown in the quickstart guide.
 */
class QuickstartExamplesTest {
    
    @Test
    fun `test quickstart example 1 - create a simple event`() {
        // From quickstart.md section 1
        val event = BaseEvent(
            eventId = "evt_123",
            type = "user_registered",
            data = mapOf(
                "userId" to "user_456",
                "email" to "user@example.com"
            ),
            correlationId = "req_789"
        )
        
        assertEquals("evt_123", event.eventId)
        assertEquals("user_registered", event.type)
        assertEquals("user_456", event.data["userId"])
        assertEquals("user@example.com", event.data["email"])
        assertEquals("req_789", event.correlationId)
    }
    
    @Test
    fun `test quickstart example 2 - SimpleHandler pure transformation`() {
        // From quickstart.md section 2
        val validateEmail = SimpleHandler(
            operator = { context, event, _ ->
                val email = event.data["email"] as? String
                require(email?.contains("@") == true) { "Invalid email" }
                event
            },
            metadata = HandlerMetadata(
                receives = setOf("user_registered"),
                returns = setOf("user_registered")
            )
        )
        
        val context = HandlerContext(requestId = "req_001")
        val event = BaseEvent(
            eventId = "evt_123",
            type = "user_registered",
            data = mapOf("email" to "user@example.com")
        )
        
        val executor = Executor(validateEmail)
        val result = executor.execute(context, event)
        
        assertEquals("evt_123", result.eventId)
        assertEquals("user_registered", result.type)
    }
    
    @Test
    fun `test quickstart example 3 - LookupHandler read before processing`() {
        // From quickstart.md section 3 (simplified without real database)
        val checkDuplicateUser = LookupHandler(
            lookerUpper = { context, event ->
                val email = event.data["email"] as String
                // Simulate database lookup
                val existingUser = if (email == "existing@example.com") "user_123" else null
                LookupData(data = mapOf("existingUser" to existingUser))
            },
            operator = { context, event, data ->
                val existingUser = data.data["existingUser"]
                if (existingUser != null) {
                    throw IllegalStateException("User already exists")
                }
                event
            },
            metadata = HandlerMetadata(
                receives = setOf("user_registered"),
                returns = setOf("user_registered")
            )
        )
        
        val context = HandlerContext(requestId = "req_001")
        val newUserEvent = BaseEvent(
            eventId = "evt_new",
            type = "user_registered",
            data = mapOf("email" to "new@example.com")
        )
        
        val executor = Executor(checkDuplicateUser)
        val result = executor.execute(context, newUserEvent)
        
        assertEquals("evt_new", result.eventId)
        
        // Test duplicate detection
        val existingUserEvent = BaseEvent(
            eventId = "evt_existing",
            type = "user_registered",
            data = mapOf("email" to "existing@example.com")
        )
        
        assertThrows(IllegalStateException::class.java) {
            executor.execute(context, existingUserEvent)
        }
    }
    
    @Test
    fun `test quickstart example 4 - LookupWriterHandler full CRUD`() {
        // From quickstart.md section 4 (simplified without real database)
        data class User(val id: String, val email: String, val createdAt: Long)
        val savedUsers = mutableListOf<User>()
        
        val createUser = LookupWriterHandler(
            lookerUpper = { context, event ->
                val email = event.data["email"] as String
                val existingUser = savedUsers.find { it.email == email }
                LookupData(data = mapOf("existingUser" to existingUser))
            },
            operator = { context, event, data ->
                require(data.data["existingUser"] == null) { "User exists" }
                event.copy(
                    data = event.data + ("createdAt" to System.currentTimeMillis())
                )
            },
            writer = { context, event ->
                val user = User(
                    id = event.data["userId"] as String,
                    email = event.data["email"] as String,
                    createdAt = event.data["createdAt"] as Long
                )
                savedUsers.add(user)
                event
            },
            metadata = HandlerMetadata(
                receives = setOf("user_registered"),
                returns = setOf("user_created")
            )
        )
        
        val context = HandlerContext(requestId = "req_001")
        val event = BaseEvent(
            eventId = "evt_123",
            type = "user_registered",
            data = mapOf(
                "userId" to "user_456",
                "email" to "user@example.com"
            )
        )
        
        val executor = Executor(createUser)
        val result = executor.execute(context, event)
        
        assertEquals(1, savedUsers.size)
        assertEquals("user_456", savedUsers[0].id)
        assertEquals("user@example.com", savedUsers[0].email)
        assertTrue(result.data.containsKey("createdAt"))
    }
}

