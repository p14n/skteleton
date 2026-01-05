package eventsystemruntime.config

import systemdefinition.SystemDefinition

/**
 * Validation rules for SystemDefinition.
 * 
 * Validates that the SystemDefinition is properly configured before
 * initializing the event system runtime.
 */
object ValidationRules {
    
    /**
     * Validate a SystemDefinition.
     *
     * @param systemDefinition The system definition to validate
     * @param config Runtime configuration
     * @throws ValidationException if validation fails
     * @throws ConfigurationException if configuration is invalid
     */
    fun validate(systemDefinition: SystemDefinition, config: RuntimeConfig) {
        // FR-002: Validate SystemDefinition before registering handlers

        // Check that system has at least one handler
        if (systemDefinition.handlers.isEmpty()) {
            throw ValidationException("SystemDefinition must have at least one handler")
        }

        // Check that system has at least one channel
        if (systemDefinition.channels.isEmpty()) {
            throw ValidationException("SystemDefinition must have at least one channel")
        }

        // Check that system has at least one event
        if (systemDefinition.events.isEmpty()) {
            throw ValidationException("SystemDefinition must have at least one event")
        }

        // FR-003: Validate persistent channels have datasource configuration
        if (config.persistentChannels.isNotEmpty() && config.datasource == null) {
            throw ConfigurationException(
                "Persistent channels require datasource configuration. " +
                "Persistent channels: ${config.persistentChannels.joinToString(", ")}"
            )
        }

        // Validate that persistent channels exist in SystemDefinition
        val channelNames = systemDefinition.channels.map { it.name }.toSet()
        val invalidPersistentChannels = config.persistentChannels - channelNames
        if (invalidPersistentChannels.isNotEmpty()) {
            throw ValidationException(
                "Persistent channels not found in SystemDefinition: ${invalidPersistentChannels.joinToString(", ")}"
            )
        }

        // Validate that all event output channels exist
        systemDefinition.events.forEach { event ->
            val invalidChannels = event.outputChannels - channelNames
            if (invalidChannels.isNotEmpty()) {
                throw ValidationException(
                    "Event '${event.eventType}' references non-existent channels: ${invalidChannels.joinToString(", ")}"
                )
            }
        }

        // Validate that all handlers have valid metadata
        systemDefinition.handlers.forEach { registration ->
            if (registration.metadata.receives.isEmpty()) {
                throw ValidationException(
                    "Handler '${registration.name}' must declare at least one event type it receives"
                )
            }
        }
    }
}

/**
 * Exception thrown when SystemDefinition validation fails.
 */
class ValidationException(message: String) : Exception(message)

/**
 * Exception thrown when configuration is invalid.
 */
class ConfigurationException(message: String) : Exception(message)

