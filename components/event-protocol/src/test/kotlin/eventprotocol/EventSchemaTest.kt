package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for BaseEvent schema validation (User Story 6).
 * 
 * These tests verify that BaseEvent enforces validation rules and
 * preserves correlation tracking across the system.
 */
class EventSchemaTest {
    
    @Test
    fun `test BaseEvent creation with valid data`() {
        // Arrange
        val eventId = "evt-123"
        val type = "user.created"
        val data = mapOf("userId" to "user-456", "name" to "Alice")
        val correlationId = "corr-789"
        val subject = "user-456"
        
        // Act
        val event = BaseEvent(
            eventId = eventId,
            type = type,
            data = data,
            correlationId = correlationId,
            subject = subject
        )
        
        // Assert
        assertEquals(eventId, event.eventId)
        assertEquals(type, event.type)
        assertEquals(data, event.data)
        assertEquals(correlationId, event.correlationId)
        assertEquals(subject, event.subject)
        assertNotNull(event.timestamp)
        assertTrue(event.timestamp > 0)
    }
    
    @Test
    fun `test BaseEvent validation rejects blank eventId`() {
        // Arrange
        val blankEventId = "   "
        val type = "user.created"
        val data = mapOf("userId" to "user-456")
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            BaseEvent(
                eventId = blankEventId,
                type = type,
                data = data
            )
        }
        assertEquals("eventId must not be blank", exception.message)
    }
    
    @Test
    fun `test BaseEvent validation rejects blank type`() {
        // Arrange
        val eventId = "evt-123"
        val blankType = ""
        val data = mapOf("userId" to "user-456")
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            BaseEvent(
                eventId = eventId,
                type = blankType,
                data = data
            )
        }
        assertEquals("type must not be blank", exception.message)
    }
    
    @Test
    fun `test BaseEvent preserves correlationId`() {
        // Arrange
        val correlationId = "corr-original-123"
        
        // Act
        val event1 = BaseEvent(
            eventId = "evt-1",
            type = "order.created",
            data = mapOf("orderId" to "order-1"),
            correlationId = correlationId
        )
        
        val event2 = BaseEvent(
            eventId = "evt-2",
            type = "order.updated",
            data = mapOf("orderId" to "order-1"),
            correlationId = event1.correlationId
        )
        
        // Assert
        assertEquals(correlationId, event1.correlationId)
        assertEquals(correlationId, event2.correlationId)
        assertEquals(event1.correlationId, event2.correlationId)
    }
    
    @Test
    fun `test BaseEvent auto-generates timestamp`() {
        // Arrange
        val beforeTimestamp = System.currentTimeMillis()
        
        // Act
        val event = BaseEvent(
            eventId = "evt-123",
            type = "test.event",
            data = emptyMap()
        )
        
        val afterTimestamp = System.currentTimeMillis()
        
        // Assert
        assertNotNull(event.timestamp)
        assertTrue(event.timestamp >= beforeTimestamp, "Timestamp should be >= before time")
        assertTrue(event.timestamp <= afterTimestamp, "Timestamp should be <= after time")
    }
    
    @Test
    fun `test BaseEvent with minimal required fields`() {
        // Act
        val event = BaseEvent(
            eventId = "evt-minimal",
            type = "minimal.event",
            data = emptyMap()
        )
        
        // Assert
        assertEquals("evt-minimal", event.eventId)
        assertEquals("minimal.event", event.type)
        assertEquals(emptyMap<String, Any?>(), event.data)
        assertNull(event.correlationId)
        assertNull(event.subject)
        assertNotNull(event.timestamp)
    }
}

