package systemdefinition

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import eventprotocol.HandlerMetadata
import eventprotocol.IHandler
import eventprotocol.LookupData
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for User Story 3: Generate D2 Diagrams
 *
 * These tests verify that the system can:
 * - Generate D2 syntax that includes all handler nodes
 * - Include event flow edges with labels
 * - Group handlers by channel context
 * - Group handlers by deployment
 * - Produce valid, renderable D2 syntax
 */
class D2GeneratorTest {

    // Test handler implementation
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
    fun `test D2 output includes all handler nodes`() {
        // Given: A system with multiple handlers
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())
        val notificationHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .addHandler(notificationHandler, "NotificationHandler")
            .build()

        // When: I generate D2 output
        val d2Output = system.toD2()

        // Then: All handler nodes are present in the output
        assertTrue(d2Output.contains("UserHandler"))
        assertTrue(d2Output.contains("EmailHandler"))
        assertTrue(d2Output.contains("NotificationHandler"))
    }

    @Test
    fun `test D2 output includes event flow edges`() {
        // Given: A system with event flows between handlers
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .build()

        // When: I generate D2 output
        val d2Output = system.toD2()

        // Then: Event flow edges are present with labels
        assertTrue(d2Output.contains("->"))
        assertTrue(d2Output.contains("email.send"))
        assertTrue(d2Output.contains("UserHandler"))
        assertTrue(d2Output.contains("EmailHandler"))
    }

    @Test
    fun `test D2 output groups handlers by channel`() {
        // Given: A system with channels and event routing
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addChannel("user-events", "User lifecycle events")
            .addChannel("email-queue", "Email delivery queue")
            .addEvent("user.created", setOf("user-events"))
            .addEvent("email.send", setOf("email-queue"))
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .build()

        // When: I generate D2 output
        val d2Output = system.toD2()

        // Then: Channel information is included
        assertTrue(d2Output.contains("user-events") || d2Output.contains("Channels"))
        assertTrue(d2Output.contains("email-queue") || d2Output.contains("Channels"))
    }

    @Test
    fun `test D2 output groups handlers by deployment`() {
        // Given: A system with deployment groupings
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .addDeployment("UserService", setOf("UserHandler"), "User management service")
            .addDeployment("EmailService", setOf("EmailHandler"), "Email delivery service")
            .build()

        // When: I generate D2 output
        val d2Output = system.toD2()

        // Then: Deployment containers are present
        assertTrue(d2Output.contains("UserService"))
        assertTrue(d2Output.contains("EmailService"))
        assertTrue(d2Output.contains("UserHandler"))
        assertTrue(d2Output.contains("EmailHandler"))
    }

    @Test
    fun `test generated D2 syntax is valid and renderable`() {
        // Given: A complete system definition
        val userHandler = TestHandler(
            setOf("user.created"),
            setOf("email.send", "notification.send"),
            "Handles user creation"
        )
        val emailHandler = TestHandler(setOf("email.send"), emptySet(), "Sends emails")
        val notificationHandler = TestHandler(setOf("notification.send"), emptySet(), "Sends notifications")

        val system = SystemDefinition.Builder()
            .addChannel("user-events")
            .addChannel("email-queue")
            .addChannel("notification-queue")
            .addEvent("user.created", setOf("user-events"))
            .addEvent("email.send", setOf("email-queue"))
            .addEvent("notification.send", setOf("notification-queue"))
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .addHandler(notificationHandler, "NotificationHandler")
            .addDeployment("UserService", setOf("UserHandler"))
            .addDeployment("MessagingService", setOf("EmailHandler", "NotificationHandler"))
            .build()

        // When: I generate D2 output
        val d2Output = system.toD2()

        // Then: The output is valid D2 syntax (basic validation)
        // Valid D2 should have:
        // - Handler definitions
        // - Edge definitions with ->
        // - Proper structure with braces
        assertTrue(d2Output.contains("{"))
        assertTrue(d2Output.contains("}"))
        assertTrue(d2Output.contains("->"))
        assertTrue(d2Output.contains("label:"))

        // All handlers should be present
        assertTrue(d2Output.contains("UserHandler"))
        assertTrue(d2Output.contains("EmailHandler"))
        assertTrue(d2Output.contains("NotificationHandler"))

        // Deployments should be present
        assertTrue(d2Output.contains("UserService"))
        assertTrue(d2Output.contains("MessagingService"))
    }
}

