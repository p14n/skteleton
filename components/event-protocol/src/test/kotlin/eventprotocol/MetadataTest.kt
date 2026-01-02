package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for metadata extraction utilities (User Story 5).
 *
 * These tests verify that metadata can be extracted from IHandler and IExecute
 * implementations for automatic routing table generation.
 */
class MetadataTest {

    @Test
    fun `test extracting metadata from IHandler implementations`() {
        // Arrange
        val simpleHandler = SimpleHandler(
            operator = { _, event, _ -> event },
            metadata = HandlerMetadata(
                receives = setOf("user.created", "user.updated"),
                returns = setOf("user.processed"),
                description = "Process user events"
            )
        )

        val lookupHandler = LookupHandler(
            lookerUpper = { _, _ -> LookupData() },
            operator = { _, event, _ -> event },
            metadata = HandlerMetadata(
                receives = setOf("order.placed"),
                returns = setOf("order.confirmed")
            )
        )

        // Act
        val simpleMetadata = simpleHandler.operatorMeta()
        val lookupMetadata = lookupHandler.operatorMeta()

        // Assert
        assertEquals(setOf("user.created", "user.updated"), simpleMetadata.receives)
        assertEquals(setOf("user.processed"), simpleMetadata.returns)
        assertEquals("Process user events", simpleMetadata.description)

        assertEquals(setOf("order.placed"), lookupMetadata.receives)
        assertEquals(setOf("order.confirmed"), lookupMetadata.returns)
    }

    @Test
    fun `test extracting metadata from IExecute implementations`() {
        // Arrange
        val handler = LookupWriterHandler(
            lookerUpper = { _, _ -> LookupData() },
            operator = { _, event, _ -> event },
            writer = { _, event -> event },
            metadata = HandlerMetadata(
                receives = setOf("payment.process"),
                returns = setOf("payment.processed"),
                description = "Process payments"
            )
        )

        val executor = Executor(handler)

        // Act
        val metadata = executor.executorMeta()

        // Assert
        assertEquals(setOf("payment.process"), metadata.receives)
        assertEquals(setOf("payment.processed"), metadata.returns)
        assertEquals("Process payments", metadata.description)
    }

    @Test
    fun `test buildRoutingTable from multiple handlers`() {
        // Arrange
        val handlers = listOf(
            SimpleHandler(
                operator = { _, event, _ -> event },
                metadata = HandlerMetadata(
                    receives = setOf("user.created"),
                    returns = setOf("user.processed")
                )
            ),
            LookupHandler(
                lookerUpper = { _, _ -> LookupData() },
                operator = { _, event, _ -> event },
                metadata = HandlerMetadata(
                    receives = setOf("order.placed", "order.updated"),
                    returns = setOf("order.confirmed")
                )
            ),
            LookupWriterHandler(
                lookerUpper = { _, _ -> LookupData() },
                operator = { _, event, _ -> event },
                writer = { _, event -> event },
                metadata = HandlerMetadata(
                    receives = setOf("payment.process"),
                    returns = setOf("payment.completed")
                )
            )
        )

        // Act
        val routingTable = buildRoutingTable(handlers)

        // Assert
        assertEquals(4, routingTable.size)
        assertTrue(routingTable.containsKey("user.created"))
        assertTrue(routingTable.containsKey("order.placed"))
        assertTrue(routingTable.containsKey("order.updated"))
        assertTrue(routingTable.containsKey("payment.process"))

        assertEquals(handlers[0], routingTable["user.created"])
        assertEquals(handlers[1], routingTable["order.placed"])
        assertEquals(handlers[1], routingTable["order.updated"])
        assertEquals(handlers[2], routingTable["payment.process"])
    }
}

