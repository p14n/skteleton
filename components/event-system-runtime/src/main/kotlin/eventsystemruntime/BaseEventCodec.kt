package eventsystemruntime

import eventprotocol.BaseEvent
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Vert.x MessageCodec for BaseEvent serialization/deserialization.
 * 
 * This codec uses kotlinx.serialization to convert BaseEvent objects to/from JSON
 * for transmission over the Vert.x EventBus.
 * 
 * The codec must be registered with the EventBus before publishing BaseEvent objects:
 * ```kotlin
 * eventBus.registerDefaultCodec(BaseEvent::class.java, BaseEventCodec())
 * ```
 */
class BaseEventCodec : MessageCodec<BaseEvent, BaseEvent> {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Encode BaseEvent to Buffer for transmission.
     */
    override fun encodeToWire(buffer: Buffer, event: BaseEvent) {
        val jsonString = json.encodeToString(event.toSerializable())
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        buffer.appendInt(bytes.size)
        buffer.appendBytes(bytes)
    }
    
    /**
     * Decode BaseEvent from Buffer after transmission.
     */
    override fun decodeFromWire(pos: Int, buffer: Buffer): BaseEvent {
        var position = pos
        val length = buffer.getInt(position)
        position += 4
        val bytes = buffer.getBytes(position, position + length)
        val jsonString = String(bytes, Charsets.UTF_8)
        val serializable = json.decodeFromString<SerializableBaseEvent>(jsonString)
        return serializable.toBaseEvent()
    }
    
    /**
     * Transform for local delivery (no serialization needed).
     */
    override fun transform(event: BaseEvent): BaseEvent = event
    
    /**
     * Codec name for registration.
     */
    override fun name(): String = "BaseEventCodec"
    
    /**
     * System codec flag (false = user codec).
     */
    override fun systemCodecID(): Byte = -1
}

/**
 * Serializable version of BaseEvent.
 * 
 * BaseEvent contains Map<String, Any?> which is not directly serializable by kotlinx.serialization.
 * This class uses Map<String, String> for JSON serialization, converting values to/from strings.
 */
@Serializable
private data class SerializableBaseEvent(
    val eventId: String,
    val type: String,
    val data: Map<String, String>,
    val correlationId: String? = null,
    val subject: String? = null,
    val timestamp: Long
)

/**
 * Convert BaseEvent to SerializableBaseEvent for JSON encoding.
 */
private fun BaseEvent.toSerializable(): SerializableBaseEvent {
    return SerializableBaseEvent(
        eventId = this.eventId,
        type = this.type,
        data = this.data.mapValues { (_, value) -> value?.toString() ?: "" },
        correlationId = this.correlationId,
        subject = this.subject,
        timestamp = this.timestamp
    )
}

/**
 * Convert SerializableBaseEvent back to BaseEvent after JSON decoding.
 */
private fun SerializableBaseEvent.toBaseEvent(): BaseEvent {
    return BaseEvent(
        eventId = this.eventId,
        type = this.type,
        data = this.data.mapValues { (_, value) -> value as Any? },
        correlationId = this.correlationId,
        subject = this.subject,
        timestamp = this.timestamp
    )
}

