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
 * Integration test covering all three user stories together.
 *
 * This test verifies that:
 * - User Story 1: System definition and querying works
 * - User Story 2: System verification detects issues
 * - User Story 3: D2 diagram generation works
 *
 * All features work together in a realistic scenario.
 */
class IntegrationTest {

    // Test handler implementations
    class TestHandler(
        private val receives: Set<String>,
        private val returns: Set<String>,
        private val desc: String? = null
    ) : IHandler {
        override fun operatorMeta() = HandlerMetadata(receives, returns, desc)
        override fun lookup(context: HandlerContext, event: BaseEvent) = LookupData()
        override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData) = event
        override fun write(context: HandlerContext, event: BaseEvent) = event
    }

    @Test
    fun `test complete workflow - define, verify, and visualize system`() {
        // ===== USER STORY 1: Define Event-Driven System =====

        // Given: I want to define a user management system
        val userCreatedHandler = TestHandler(
            receives = setOf("http.user.post"),
            returns = setOf("user.created"),
            desc = "Creates new users"
        )

        val emailHandler = TestHandler(
            receives = setOf("user.created"),
            returns = setOf("email.send"),
            desc = "Sends welcome emails"
        )

        val emailSenderHandler = TestHandler(
            receives = setOf("email.send"),
            returns = emptySet(),
            desc = "Delivers emails via SMTP"
        )

        // When: I define the system with handlers, events, channels, and deployments
        val system = SystemDefinition.Builder()
            // Define channels
            .addChannel("http-events", "HTTP API events")
            .addChannel("user-events", "User lifecycle events")
            .addChannel("email-queue", "Email delivery queue")

            // Define events with routing
            .addEvent("http.user.post", setOf("http-events"))
            .addEvent("user.created", setOf("user-events"))
            .addEvent("email.send", setOf("email-queue"))

            // Register handlers
            .addHandler(userCreatedHandler, "UserCreatedHandler")
            .addHandler(emailHandler, "EmailHandler")
            .addHandler(emailSenderHandler, "EmailSenderHandler")

            // Define deployments
            .addDeployment("UserService", setOf("UserCreatedHandler"), "User management service")
            .addDeployment("MessagingService", setOf("EmailHandler", "EmailSenderHandler"), "Email delivery service")

            .build()

        // Then: I can query the system structure
        assertEquals(3, system.handlers.size)
        assertEquals(3, system.events.size)
        assertEquals(3, system.channels.size)
        assertEquals(2, system.deployments.size)

        // I can query handlers by event
        val userCreatedConsumers = system.getConsumersOf("user.created")
        assertEquals(1, userCreatedConsumers.size)
        assertEquals("EmailHandler", userCreatedConsumers.first().name)

        // I can query events by handler
        val emailHandlerReg = system.handlers.find { it.name == "EmailHandler" }!!
        assertTrue(emailHandlerReg.metadata.returns.contains("email.send"))

        // I can query events for a channel
        val userEventsOnChannel = system.getEventsForChannel("user-events")
        assertEquals(1, userEventsOnChannel.size)
        assertEquals("user.created", userEventsOnChannel.first().eventType)

        // ===== USER STORY 2: Verify System Configuration =====

        // When: I verify the system
        val verificationResult = system.verify()

        // Then: The system is valid (no errors)
        assertTrue(verificationResult.isValid)
        assertEquals(0, verificationResult.errors.size)
        assertEquals(0, verificationResult.warnings.size)

        // ===== USER STORY 3: Generate D2 Diagrams =====

        // When: I generate a D2 diagram
        val d2Output = system.toD2()

        // Then: The diagram includes all handlers
        assertTrue(d2Output.contains("UserCreatedHandler"))
        assertTrue(d2Output.contains("EmailHandler"))
        assertTrue(d2Output.contains("EmailSenderHandler"))

        // The diagram includes deployments
        assertTrue(d2Output.contains("UserService"))
        assertTrue(d2Output.contains("MessagingService"))

        // The diagram includes event flows
        assertTrue(d2Output.contains("->"))
        assertTrue(d2Output.contains("user.created"))
        assertTrue(d2Output.contains("email.send"))

        // The diagram includes channel information
        assertTrue(d2Output.contains("user-events") || d2Output.contains("Channels"))

        // ===== COMPLETE WORKFLOW SUCCESS =====
        // All three user stories work together seamlessly!
    }

    @Test
    fun `test edge case - empty system`() {
        // Given: An empty system
        val system = SystemDefinition.Builder().build()

        // Then: All operations work without errors
        assertEquals(0, system.handlers.size)
        assertEquals(0, system.events.size)
        assertEquals(0, system.channels.size)

        // Verification works
        val result = system.verify()
        assertTrue(result.isValid)

        // D2 generation works
        val d2 = system.toD2()
        assertTrue(d2.isNotEmpty())
    }

    @Test
    fun `test edge case - single handler system`() {
        // Given: A system with just one handler
        val handler = TestHandler(setOf("input.event"), emptySet(), "Single handler")

        val system = SystemDefinition.Builder()
            .addEvent("input.event", emptySet())
            .addHandler(handler, "SingleHandler")
            .build()

        // Then: All operations work
        assertEquals(1, system.handlers.size)
        val result = system.verify()
        assertTrue(result.isValid)

        val d2 = system.toD2()
        assertTrue(d2.contains("SingleHandler"))
    }
}

