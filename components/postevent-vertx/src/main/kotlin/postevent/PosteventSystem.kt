package postevent

import com.p14n.postevent.broker.DefaultExecutor
import com.p14n.postevent.broker.MessageSubscriber
import com.p14n.postevent.broker.TransactionalEvent
import com.p14n.postevent.data.Event
import com.p14n.postevent.vertx.VertxConsumerServer
import com.p14n.postevent.vertx.VertxPersistentConsumer
import com.p14n.postevent.vertx.adapter.EventBusMessageBroker
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eventprotocol.BaseEvent
import io.opentelemetry.api.OpenTelemetry
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Database-backed persistent event storage system using the postevent library.
 * This implementation follows the pattern from postevent.clj example.
 * 
 * The pattern is:
 * 1. Create VertxConsumerServer (server-side) - handles database schema setup
 * 2. Create VertxPersistentConsumer (client-side) - handles consuming events
 * 3. Start server first with topics
 * 4. Start client with topics
 * 5. For publishing: Use EventBusMessageBroker directly
 * 6. For subscribing: Use VertxPersistentConsumer.subscribe()
 */
class PosteventSystem(
    private val vertx: Vertx,
    private val scope: CoroutineScope,
    jdbcUrl: String,
    username: String,
    password: String,
    maxPoolSize: Int = 10,
    persistentChannels: Set<String> = emptySet()
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(PosteventSystem::class.java)

    private val dataSource: HikariDataSource
    private val executor: DefaultExecutor
    private val openTelemetry: OpenTelemetry
    private val eventBusMessageBroker: EventBusMessageBroker
    private val vertxConsumerServer: VertxConsumerServer
    private val vertxPersistentConsumer: VertxPersistentConsumer

    // Sanitize channel names (postevent requires lowercase + underscores only)
    private val sanitizedChannels: Set<String>

    init {
        logger.info("Initializing PosteventSystem with database: $jdbcUrl")

        // Initialize HikariCP connection pool
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maxPoolSize
        }
        dataSource = HikariDataSource(hikariConfig)
        logger.info("Database connection pool initialized")

        // Create executor (thread pool size = 2 as in Clojure example)
        executor = DefaultExecutor(2)

        // Create OpenTelemetry (no-op for now)
        openTelemetry = OpenTelemetry.noop()

        // Sanitize channel names
        sanitizedChannels = persistentChannels.map { sanitizeChannelName(it) }.toSet()

        // Create EventBusMessageBroker
        eventBusMessageBroker = EventBusMessageBroker(
            vertx.eventBus(),
            dataSource,
            executor,
            openTelemetry,
            "consumer_server"
        )
        logger.info("EventBusMessageBroker created")

        // Create VertxConsumerServer (server-side)
        vertxConsumerServer = VertxConsumerServer(dataSource, executor, openTelemetry)
        logger.info("VertxConsumerServer created")

        // Create VertxPersistentConsumer (client-side, batch size = 20 as in Clojure example)
        vertxPersistentConsumer = VertxPersistentConsumer(openTelemetry, executor, 20)
        logger.info("VertxPersistentConsumer created")

        // Start server with topics
        if (sanitizedChannels.isNotEmpty()) {
            logger.info("Starting VertxConsumerServer with topics: $sanitizedChannels")
            vertxConsumerServer.start(vertx.eventBus(), eventBusMessageBroker, sanitizedChannels)
            logger.info("VertxConsumerServer started successfully")

            // Start client with topics
            logger.info("Starting VertxPersistentConsumer with topics: $sanitizedChannels")
            vertxPersistentConsumer.start(sanitizedChannels, dataSource, vertx.eventBus(), eventBusMessageBroker)
            logger.info("VertxPersistentConsumer started successfully")
        } else {
            logger.info("No persistent channels configured, postevent system not started")
        }

        logger.info("PosteventSystem initialized successfully")
    }

    /**
     * Publish an event to a persistent channel.
     */
    fun publish(
        channel: String,
        event: BaseEvent,
        connection: Connection? = null
    ) {
        val sanitizedChannel = sanitizeChannelName(channel)
        logger.debug("Publishing event to persistent channel: $sanitizedChannel")

        // Convert BaseEvent to postevent Event
        val posteventEvent = convertToPosteventEvent(event, sanitizedChannel)

        // Publish using EventBusMessageBroker
        if (connection != null) {
            // Transactional publish
            val transactionalEvent = TransactionalEvent(connection, posteventEvent)
            eventBusMessageBroker.publish(sanitizedChannel, transactionalEvent)
        } else {
            // Non-transactional publish
            eventBusMessageBroker.publish(sanitizedChannel, posteventEvent)
        }

        logger.debug("Event published to channel: $sanitizedChannel")
    }

    /**
     * Subscribe to a persistent channel.
     */
    fun subscribe(
        channel: String,
        handler: suspend (BaseEvent, Connection?) -> Unit
    ) {
        val sanitizedChannel = sanitizeChannelName(channel)
        logger.info("Subscribing to persistent channel: $sanitizedChannel")

        // Subscribe using VertxPersistentConsumer
        vertxPersistentConsumer.subscribe(sanitizedChannel, MessageSubscriber { transactionalEvent ->
            scope.launch {
                try {
                    // Convert postevent Event to BaseEvent
                    val posteventEvent = transactionalEvent.event()
                    val baseEvent = convertFromPosteventEvent(posteventEvent)

                    // Call the handler with the event and connection
                    handler(baseEvent, transactionalEvent.connection())

                } catch (e: Exception) {
                    logger.error("Error processing event from channel $sanitizedChannel: ${e.message}", e)
                    throw e
                }
            }
        })

        logger.info("Subscribed to persistent channel: $sanitizedChannel")
    }

    /**
     * Convert BaseEvent to postevent Event.
     */
    private fun convertToPosteventEvent(baseEvent: BaseEvent, topic: String): Event {
        // Convert data map to JSON byte array
        val dataBytes = buildJsonString(baseEvent.data).toByteArray(Charsets.UTF_8)

        return Event.create(
            baseEvent.eventId,
            "skteleton",  // source
            baseEvent.type,
            "application/json",  // datacontenttype
            null,  // dataschema
            baseEvent.subject ?: "",
            dataBytes,
            Instant.ofEpochMilli(baseEvent.timestamp),
            null,  // idn
            topic,
            null  // traceparent
        )
    }

    /**
     * Convert postevent Event to BaseEvent.
     */
    private fun convertFromPosteventEvent(posteventEvent: Event): BaseEvent {
        val payloadString = String(posteventEvent.data(), Charsets.UTF_8)
        return BaseEvent(
            eventId = posteventEvent.id(),
            type = posteventEvent.type(),
            data = parseJsonString(payloadString),
            correlationId = null,  // postevent doesn't have correlationId
            subject = posteventEvent.subject().takeIf { it.isNotBlank() },
            timestamp = posteventEvent.time()?.toEpochMilli() ?: System.currentTimeMillis()
        )
    }

    /**
     * Sanitize channel name to match postevent requirements (lowercase + underscores only).
     */
    private fun sanitizeChannelName(channel: String): String {
        return channel.lowercase().replace("-", "_")
    }

    /**
     * Close all resources.
     */
    override fun close() {
        logger.info("Closing PosteventSystem")

        try {
            vertxPersistentConsumer.close()
        } catch (e: Exception) {
            logger.error("Error closing VertxPersistentConsumer", e)
        }

        try {
            vertxConsumerServer.close()
        } catch (e: Exception) {
            logger.error("Error closing VertxConsumerServer", e)
        }

        try {
            eventBusMessageBroker.close()
        } catch (e: Exception) {
            logger.error("Error closing EventBusMessageBroker", e)
        }

        try {
            executor.shutdownNow()
        } catch (e: Exception) {
            logger.error("Error shutting down executor", e)
        }

        try {
            dataSource.close()
        } catch (e: Exception) {
            logger.error("Error closing data source", e)
        }

        logger.info("PosteventSystem closed")
    }

    /**
     * Build a JSON string from a Map<String, Any?>.
     * This is needed because kotlinx.serialization cannot handle Map<String, Any?> directly.
     */
    private fun buildJsonString(map: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append("{")
        map.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) sb.append(",")
            sb.append("\"").append(key).append("\":")
            appendValue(sb, value)
        }
        sb.append("}")
        return sb.toString()
    }

    private fun appendValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is String -> sb.append("\"").append(value.replace("\"", "\\\"")).append("\"")
            is Number -> sb.append(value.toString())
            is Boolean -> sb.append(value.toString())
            is Map<*, *> -> {
                sb.append("{")
                @Suppress("UNCHECKED_CAST")
                val map = value as Map<String, Any?>
                map.entries.forEachIndexed { index, (k, v) ->
                    if (index > 0) sb.append(",")
                    sb.append("\"").append(k).append("\":")
                    appendValue(sb, v)
                }
                sb.append("}")
            }
            is List<*> -> {
                sb.append("[")
                value.forEachIndexed { index, item ->
                    if (index > 0) sb.append(",")
                    appendValue(sb, item)
                }
                sb.append("]")
            }
            else -> sb.append("\"").append(value.toString()).append("\"")
        }
    }

    /**
     * Parse a JSON string into a Map<String, Any?>.
     */
    private fun parseJsonString(jsonString: String): Map<String, Any?> {
        if (jsonString.isBlank()) return emptyMap()

        // Simple JSON parser for Map<String, Any?>
        val trimmed = jsonString.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw IllegalArgumentException("Invalid JSON object: $jsonString")
        }

        val result = mutableMapOf<String, Any?>()
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return result

        // This is a simplified parser - for production use a proper JSON library
        // For now, we'll just handle simple key-value pairs
        var i = 0
        while (i < content.length) {
            // Skip whitespace
            while (i < content.length && content[i].isWhitespace()) i++
            if (i >= content.length) break

            // Read key
            if (content[i] != '"') throw IllegalArgumentException("Expected quote at position $i")
            i++ // skip opening quote
            val keyStart = i
            while (i < content.length && content[i] != '"') i++
            val key = content.substring(keyStart, i)
            i++ // skip closing quote

            // Skip whitespace and colon
            while (i < content.length && (content[i].isWhitespace() || content[i] == ':')) i++

            // Read value
            val value = readValue(content, i)
            result[key] = value.first
            i = value.second

            // Skip whitespace and comma
            while (i < content.length && (content[i].isWhitespace() || content[i] == ',')) i++
        }

        return result
    }

    private fun readValue(content: String, startIndex: Int): Pair<Any?, Int> {
        var i = startIndex
        return when {
            content[i] == '"' -> {
                // String value
                i++ // skip opening quote
                val valueStart = i
                while (i < content.length && content[i] != '"') i++
                val value = content.substring(valueStart, i)
                i++ // skip closing quote
                Pair(value, i)
            }
            content.startsWith("null", i) -> Pair(null, i + 4)
            content.startsWith("true", i) -> Pair(true, i + 4)
            content.startsWith("false", i) -> Pair(false, i + 5)
            content[i].isDigit() || content[i] == '-' -> {
                // Number value
                val valueStart = i
                while (i < content.length && (content[i].isDigit() || content[i] == '.' || content[i] == '-')) i++
                val value = content.substring(valueStart, i)
                Pair(if (value.contains('.')) value.toDouble() else value.toLong(), i)
            }
            else -> throw IllegalArgumentException("Unexpected character at position $i: ${content[i]}")
        }
    }
}

