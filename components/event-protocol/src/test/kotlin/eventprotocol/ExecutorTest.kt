package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for Executor orchestration (User Story 1).
 * 
 * These tests verify that Executor correctly orchestrates the three-phase
 * handler pipeline: lookup → operate → write.
 */
class ExecutorTest {
    
    @Test
    fun `test Executor calls handler lookup first`() {
        // Arrange
        var lookupCalled = false
        var operateCalled = false
        var writeCalled = false
        val callOrder = mutableListOf<String>()
        
        val mockHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                lookupCalled = true
                callOrder.add("lookup")
                assertFalse(operateCalled, "lookup should be called before operate")
                assertFalse(writeCalled, "lookup should be called before write")
                return LookupData(data = mapOf("test" to "data"))
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                operateCalled = true
                callOrder.add("operate")
                assertTrue(lookupCalled, "operate should be called after lookup")
                assertFalse(writeCalled, "operate should be called before write")
                return event
            }
            
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                writeCalled = true
                callOrder.add("write")
                assertTrue(lookupCalled, "write should be called after lookup")
                assertTrue(operateCalled, "write should be called after operate")
                return event
            }
            
            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(
                    receives = setOf("test.event"),
                    returns = setOf("test.event")
                )
            }
        }
        
        val executor = Executor(mockHandler)
        val context = HandlerContext(requestId = "req-001")
        val event = BaseEvent(
            eventId = "evt-001",
            type = "test.event",
            data = emptyMap()
        )
        
        // Act
        executor.execute(context, event)
        
        // Assert
        assertTrue(lookupCalled, "lookup should have been called")
        assertTrue(operateCalled, "operate should have been called")
        assertTrue(writeCalled, "write should have been called")
        assertEquals(listOf("lookup", "operate", "write"), callOrder)
    }
    
    @Test
    fun `test Executor passes lookup data to operate`() {
        // Arrange
        val expectedLookupData = mapOf("userId" to "user-123", "role" to "admin")
        var receivedLookupData: LookupData? = null
        
        val mockHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                return LookupData(data = expectedLookupData)
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                receivedLookupData = data
                return event
            }
            
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                return event
            }
            
            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(
                    receives = setOf("test.event"),
                    returns = setOf("test.event")
                )
            }
        }
        
        val executor = Executor(mockHandler)
        val context = HandlerContext(requestId = "req-002")
        val event = BaseEvent(
            eventId = "evt-002",
            type = "test.event",
            data = emptyMap()
        )
        
        // Act
        executor.execute(context, event)
        
        // Assert
        assertNotNull(receivedLookupData)
        assertEquals(expectedLookupData, receivedLookupData?.data)
    }
    
    @Test
    fun `test Executor passes operated event to write`() {
        // Arrange
        val operatedEvent = BaseEvent(
            eventId = "evt-operated",
            type = "test.operated",
            data = mapOf("status" to "processed")
        )
        var receivedEventInWrite: BaseEvent? = null
        
        val mockHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                return LookupData()
            }
            
            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                return operatedEvent
            }
            
            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                receivedEventInWrite = event
                return event
            }
            
            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(
                    receives = setOf("test.event"),
                    returns = setOf("test.operated")
                )
            }
        }

        val executor = Executor(mockHandler)
        val context = HandlerContext(requestId = "req-003")
        val event = BaseEvent(
            eventId = "evt-003",
            type = "test.event",
            data = emptyMap()
        )

        // Act
        executor.execute(context, event)

        // Assert
        assertNotNull(receivedEventInWrite)
        assertEquals(operatedEvent.eventId, receivedEventInWrite?.eventId)
        assertEquals(operatedEvent.type, receivedEventInWrite?.type)
        assertEquals(operatedEvent.data, receivedEventInWrite?.data)
    }

    @Test
    fun `test Executor executorMeta returns handler metadata`() {
        // Arrange
        val expectedMetadata = HandlerMetadata(
            receives = setOf("user.created", "user.updated"),
            returns = setOf("user.processed"),
            description = "Test handler"
        )

        val mockHandler = object : IHandler {
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
                return expectedMetadata
            }
        }

        val executor = Executor(mockHandler)

        // Act
        val metadata = executor.executorMeta()

        // Assert
        assertEquals(expectedMetadata, metadata)
        assertEquals(expectedMetadata.receives, metadata.receives)
        assertEquals(expectedMetadata.returns, metadata.returns)
        assertEquals(expectedMetadata.description, metadata.description)
    }

    @Test
    fun `test Executor handles exceptions from handler phases`() {
        // Arrange - Test exception in lookup phase
        val lookupHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                throw IllegalStateException("Lookup failed")
            }

            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                return event
            }

            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                return event
            }

            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(receives = setOf("test"), returns = setOf("test"))
            }
        }

        val executor = Executor(lookupHandler)
        val context = HandlerContext(requestId = "req-004")
        val event = BaseEvent(eventId = "evt-004", type = "test", data = emptyMap())

        // Act & Assert
        val exception = assertThrows(IllegalStateException::class.java) {
            executor.execute(context, event)
        }
        assertEquals("Lookup failed", exception.message)
    }

    @Test
    fun `test Executor returns final event from write phase`() {
        // Arrange
        val finalEvent = BaseEvent(
            eventId = "evt-final",
            type = "test.final",
            data = mapOf("status" to "completed")
        )

        val mockHandler = object : IHandler {
            override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
                return LookupData()
            }

            override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
                return event
            }

            override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
                return finalEvent
            }

            override fun operatorMeta(): HandlerMetadata {
                return HandlerMetadata(receives = setOf("test"), returns = setOf("test.final"))
            }
        }

        val executor = Executor(mockHandler)
        val context = HandlerContext(requestId = "req-005")
        val event = BaseEvent(eventId = "evt-005", type = "test", data = emptyMap())

        // Act
        val result = executor.execute(context, event)

        // Assert
        assertEquals(finalEvent, result)
        assertEquals("evt-final", result.eventId)
        assertEquals("test.final", result.type)
    }
}


