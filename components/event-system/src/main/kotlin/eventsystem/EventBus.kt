package eventsystem

import eventprotocol.Event

/**
 * A simple event bus for publishing and subscribing to events.
 */
class EventBus {
    private val subscribers = mutableMapOf<String, MutableList<(Event) -> Unit>>()

    /**
     * Subscribe to events of a specific type.
     */
    fun subscribe(eventType: String, handler: (Event) -> Unit) {
        subscribers.getOrPut(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * Publish an event to all subscribers.
     */
    fun publish(event: Event) {
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

