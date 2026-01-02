package eventsystem

import eventprotocol.BaseEvent

/**
 * A simple event bus for publishing and subscribing to events.
 */
class EventBus {
    private val subscribers = mutableMapOf<String, MutableList<(BaseEvent) -> Unit>>()

    /**
     * Subscribe to events of a specific type.
     */
    fun subscribe(eventType: String, handler: (BaseEvent) -> Unit) {
        subscribers.getOrPut(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * Publish an event to all subscribers.
     */
    fun publish(event: BaseEvent) {
        subscribers[event.type]?.forEach { handler ->
            handler(event)
        }
    }

    /**
     * Clear all subscribers.
     */
    fun clear() {
        subscribers.clear()
    }
}

