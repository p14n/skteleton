package systemdefinition

import eventprotocol.IHandler

/**
 * Immutable representation of a complete event-driven system.
 *
 * SystemDefinition captures the structure of an event-driven system including
 * handlers, events, channels, and deployment groupings. Once created, it is
 * immutable and can be queried to understand the system architecture.
 *
 * Use the Builder to construct instances:
 * ```
 * val system = SystemDefinition.Builder()
 *     .addHandler(myHandler, "MyHandler")
 *     .addEvent("user.created", setOf("user-channel"))
 *     .build()
 * ```
 *
 * @property handlers All registered handlers in the system
 * @property events All event types defined in the system
 * @property channels All communication channels/topics
 * @property deployments Logical groupings of handlers
 */
data class SystemDefinition(
    val handlers: Set<HandlerRegistration>,
    val events: Set<EventDefinition>,
    val channels: Set<Channel>,
    val deployments: Set<Deployment>
) {
    /**
     * Builder for constructing immutable SystemDefinition instances.
     *
     * Provides a fluent API for defining event-driven systems.
     */
    class Builder {
        private val handlers = mutableSetOf<HandlerRegistration>()
        private val events = mutableMapOf<String, EventDefinition>()
        private val channels = mutableSetOf<Channel>()
        private val deployments = mutableSetOf<Deployment>()

        /**
         * Register a handler in the system.
         *
         * @param handler The handler instance implementing IHandler
         * @param name Unique name for this handler in the system
         * @return This builder for chaining
         */
        fun addHandler(handler: IHandler, name: String) = apply {
            handlers.add(HandlerRegistration(name, handler))
        }

        /**
         * Define an event type and its routing configuration.
         *
         * @param eventType Unique event type identifier
         * @param outputChannels Channels this event can be routed to
         * @return This builder for chaining
         */
        fun addEvent(eventType: String, outputChannels: Set<String> = emptySet()) = apply {
            events[eventType] = EventDefinition(eventType, outputChannels)
        }

        /**
         * Add a communication channel to the system.
         *
         * @param name Unique channel identifier
         * @param description Optional description of the channel
         * @return This builder for chaining
         */
        fun addChannel(name: String, description: String? = null) = apply {
            channels.add(Channel(name, description))
        }

        /**
         * Define a deployment grouping of handlers.
         *
         * @param name Unique deployment identifier
         * @param handlerNames Names of handlers in this deployment
         * @param description Optional description
         * @return This builder for chaining
         */
        fun addDeployment(name: String, handlerNames: Set<String>, description: String? = null) = apply {
            deployments.add(Deployment(name, handlerNames, description))
        }

        /**
         * Build the immutable SystemDefinition.
         *
         * Validates that:
         * - All handler names referenced in deployments exist
         * - All channel names referenced in events exist
         *
         * @return Immutable SystemDefinition instance
         * @throws IllegalStateException if validation fails
         */
        fun build(): SystemDefinition {
            // Validate deployment handler references
            val handlerNames = handlers.map { it.name }.toSet()
            deployments.forEach { deployment ->
                val invalidHandlers = deployment.handlerNames - handlerNames
                require(invalidHandlers.isEmpty()) {
                    "Deployment '${deployment.name}' references non-existent handlers: $invalidHandlers"
                }
            }

            // Validate event channel references
            val channelNames = channels.map { it.name }.toSet()
            events.values.forEach { event ->
                val invalidChannels = event.outputChannels - channelNames
                require(invalidChannels.isEmpty()) {
                    "Event '${event.eventType}' references non-existent channels: $invalidChannels"
                }
            }

            // Compute producedBy and consumedBy for each event from handler metadata
            val enrichedEvents = events.values.map { event ->
                val producers = handlers.filter { event.eventType in it.metadata.returns }.map { it.name }.toSet()
                val consumers = handlers.filter { event.eventType in it.metadata.receives }.map { it.name }.toSet()
                event.copy(producedBy = producers, consumedBy = consumers)
            }.toSet()

            return SystemDefinition(
                handlers = handlers.toSet(),
                events = enrichedEvents,
                channels = channels.toSet(),
                deployments = deployments.toSet()
            )
        }
    }

    /**
     * Get all handlers that produce a specific event type.
     *
     * @param eventType The event type to query
     * @return Set of handler registrations that return this event
     */
    fun getProducersOf(eventType: String): Set<HandlerRegistration> {
        return handlers.filter { eventType in it.metadata.returns }.toSet()
    }

    /**
     * Get all handlers that consume a specific event type.
     *
     * @param eventType The event type to query
     * @return Set of handler registrations that receive this event
     */
    fun getConsumersOf(eventType: String): Set<HandlerRegistration> {
        return handlers.filter { eventType in it.metadata.receives }.toSet()
    }

    /**
     * Get all events routed to a specific channel.
     *
     * @param channelName The channel name to query
     * @return Set of event definitions that route to this channel
     */
    fun getEventsForChannel(channelName: String): Set<EventDefinition> {
        return events.filter { channelName in it.outputChannels }.toSet()
    }
}

