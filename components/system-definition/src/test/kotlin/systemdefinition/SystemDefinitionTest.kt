package systemdefinition

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import eventprotocol.HandlerMetadata
import eventprotocol.IHandler
import eventprotocol.LookupData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for User Story 1: Define Event-Driven System Structure
 *
 * These tests verify that developers can:
 * - Define event-driven systems using the type-safe Kotlin DSL
 * - Query the system structure programmatically
 * - Access handlers by event, events by handler, channels, and event routing
 */
class SystemDefinitionTest {

    // Test handler implementations
    class TestHandler(
        private val receives: Set<String>,
        private val returns: Set<String>
    ) : IHandler {
        override fun operatorMeta() = HandlerMetadata(receives, returns, "Test handler")
        override fun lookup(context: HandlerContext, event: BaseEvent) = LookupData()
        override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
        override fun write(context: HandlerContext, event: BaseEvent) = event
    }

    @Test
    fun `test system definition creation`() {
        // Given: Handlers with metadata
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        // When: I define a system with events, channels, and handler mappings
        val system = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addChannel("email-queue", "Email delivery queue")
            .addEvent("user.created", setOf("user-events"))
            .addEvent("email.send", setOf("email-queue"))
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .build()

        // Then: The system structure is captured in a queryable Kotlin object
        assertEquals(2, system.handlers.size)
        assertEquals(2, system.events.size)
        assertEquals(2, system.channels.size)
    }

    @Test
    fun `test querying handlers by event`() {
        // Given: I have defined a system
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .build()

        // When: I query for handlers that receive a specific event
        val userCreatedConsumers = system.getConsumersOf("user.created")
        val emailSendConsumers = system.getConsumersOf("email.send")

        // Then: I get the correct list of handlers
        assertEquals(1, userCreatedConsumers.size)
        assertEquals("UserHandler", userCreatedConsumers.first().name)

        assertEquals(1, emailSendConsumers.size)
        assertEquals("EmailHandler", emailSendConsumers.first().name)
    }

    @Test
    fun `test querying events by handler`() {
        // Given: I have defined a system
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send", "notification.send"))

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addEvent("notification.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .build()

        // When: I query for events returned by a handler
        val handler = system.handlers.first { it.name == "UserHandler" }
        val returnedEvents = handler.metadata.returns

        // Then: I get the correct set of events
        assertEquals(2, returnedEvents.size)
        assertTrue(returnedEvents.contains("email.send"))
        assertTrue(returnedEvents.contains("notification.send"))
    }

    @Test
    fun `test querying all channels`() {
        // Given: I have defined a system with multiple channels
        val system = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addChannel("email-queue", "Email delivery queue")
            .addChannel("notification-queue", "Notification delivery")
            .build()

        // When: I query for all channels in the system
        val allChannels = system.channels

        // Then: I get the complete set of channels
        assertEquals(3, allChannels.size)
        val channelNames = allChannels.map { it.name }.toSet()
        assertTrue(channelNames.contains("user-events"))
        assertTrue(channelNames.contains("email-queue"))
        assertTrue(channelNames.contains("notification-queue"))
    }

    @Test
    fun `test querying event routing to channels`() {
        // Given: I have defined event routing
        val system = SystemDefinition.Builder()
            .addChannel("user-events")
            .addChannel("email-queue")
            .addEvent("user.created", setOf("user-events"))
            .addEvent("user.updated", setOf("user-events"))
            .addEvent("email.send", setOf("email-queue"))
            .build()

        // When: I query for the output channels of a specific event
        val userEventsOnChannel = system.getEventsForChannel("user-events")
        val emailEventsOnChannel = system.getEventsForChannel("email-queue")

        // Then: I get all configured channel mappings
        assertEquals(2, userEventsOnChannel.size)
        val userEventTypes = userEventsOnChannel.map { it.eventType }.toSet()
        assertTrue(userEventTypes.contains("user.created"))
        assertTrue(userEventTypes.contains("user.updated"))

        assertEquals(1, emailEventsOnChannel.size)
        assertEquals("email.send", emailEventsOnChannel.first().eventType)
    }
}

