package systemdefinition

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import eventprotocol.HandlerMetadata
import eventprotocol.IHandler
import eventprotocol.LookupData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for User Story 2: Verify System Configuration
 *
 * These tests verify that the system can:
 * - Detect unhandled events (returned by handlers but not declared)
 * - Detect handler event mismatches
 * - Detect orphaned events (warning)
 * - Detect circular flows (warning)
 * - Return comprehensive error/warning lists
 */
class SystemVerifierTest {

    // Test handler implementation
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
    fun `test detecting unhandled events`() {
        // Given: A handler returns an event not declared in the system
        val handler = TestHandler(setOf("user.created"), setOf("email.send", "notification.send"))

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            // notification.send is NOT declared
            .addHandler(handler, "UserHandler")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: I get an error for the unhandled event
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(VerificationError.ErrorType.UNHANDLED_EVENT, result.errors[0].type)
        assertTrue(result.errors[0].message.contains("notification.send"))
    }

    @Test
    fun `test detecting handler event mismatches`() {
        // Given: Event definition says handler consumes it, but handler metadata doesn't
        val handler = TestHandler(setOf("user.created"), setOf("email.send"))

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(handler, "UserHandler")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: The system is valid (handler metadata matches event definition)
        assertTrue(result.isValid)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `test detecting orphaned events`() {
        // Given: An event is declared but no handlers consume it
        val handler1 = TestHandler(setOf("user.created"), setOf("email.send"))
        val handler2 = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addEvent("orphaned.event", emptySet())  // No handler consumes this
            .addHandler(handler1, "UserHandler")
            .addHandler(handler2, "EmailHandler")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: I get a warning for the orphaned event
        assertTrue(result.isValid)  // Warnings don't make system invalid
        assertEquals(1, result.warnings.size)
        assertEquals(VerificationWarning.WarningType.ORPHANED_EVENT, result.warnings[0].type)
        assertTrue(result.warnings[0].message.contains("orphaned.event"))
    }

    @Test
    fun `test detecting circular flows`() {
        // Given: Handlers form a circular event flow (A -> B -> A)
        val handlerA = TestHandler(setOf("event.b"), setOf("event.a"))
        val handlerB = TestHandler(setOf("event.a"), setOf("event.b"))

        val system = SystemDefinition.Builder()
            .addEvent("event.a", emptySet())
            .addEvent("event.b", emptySet())
            .addHandler(handlerA, "HandlerA")
            .addHandler(handlerB, "HandlerB")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: I get a warning for the circular flow
        assertTrue(result.isValid)  // Warnings don't make system invalid
        assertEquals(1, result.warnings.size)
        assertEquals(VerificationWarning.WarningType.CIRCULAR_FLOW, result.warnings[0].type)
    }

    @Test
    fun `test valid system returns no errors`() {
        // Given: A properly configured system
        val userHandler = TestHandler(setOf("user.created"), setOf("email.send"))
        val emailHandler = TestHandler(setOf("email.send"), emptySet())

        val system = SystemDefinition.Builder()
            .addEvent("user.created", emptySet())
            .addEvent("email.send", emptySet())
            .addHandler(userHandler, "UserHandler")
            .addHandler(emailHandler, "EmailHandler")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: No errors or warnings are returned
        assertTrue(result.isValid)
        assertEquals(0, result.errors.size)
        assertEquals(0, result.warnings.size)
        assertFalse(result.hasIssues)
    }

    @Test
    fun `test comprehensive error list with multiple issues`() {
        // Given: A system with multiple configuration errors
        val handler1 = TestHandler(setOf("event.a"), setOf("event.b", "undeclared.event"))
        val handler2 = TestHandler(setOf("event.c"), setOf("event.d"))

        val system = SystemDefinition.Builder()
            .addEvent("event.a", emptySet())
            .addEvent("event.b", emptySet())
            .addEvent("event.c", emptySet())
            // event.d and undeclared.event are not declared
            .addHandler(handler1, "Handler1")
            .addHandler(handler2, "Handler2")
            .build()

        // When: I verify the system
        val result = system.verify()

        // Then: All errors are reported
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)  // Two unhandled events
        assertTrue(result.errors.all { it.type == VerificationError.ErrorType.UNHANDLED_EVENT })
    }
}

