package eventsystemruntime

import eventprotocol.BaseEvent
import eventprotocol.HandlerContext
import systemdefinition.SystemDefinition

/**
 * API Contract for Event System Runtime
 * 
 * This file defines the public API surface for the event-system-runtime component.
 * All public functions must maintain backward compatibility according to semantic versioning.
 * 
 * Version: 0.1.0
 * Status: Draft
 */

/**
 * Main entry point for event system runtime.
 * 
 * Initializes event routing from a SystemDefinition, subscribing handlers to
 * appropriate channels (transient or persistent) and enabling event publishing.
 * 
 * @param systemDefinition Immutable system configuration
 * @param config Runtime configuration (optional, uses defaults if not provided)
 * @return Initialized EventSystemRuntime instance
 * @throws ValidationException if SystemDefinition is invalid
 * @throws ConfigurationException if persistent channels lack datasource configuration
 * 
 * Maps to: FR-001, FR-002, FR-003, User Story 1
 */
fun initialize(
    systemDefinition: SystemDefinition,
    config: RuntimeConfig = RuntimeConfig()
): EventSystemRuntime

/**
 * Publish an event to a channel.
 * 
 * Routes the event to the appropriate event bus (transient or persistent) based
 * on channel configuration. Generates correlation ID if not present.
 * 
 * @param channelName Target channel name
 * @param event Event to publish
 * @param context Optional handler context for transactional publishing
 * @throws ChannelNotFoundException if channel doesn't exist in SystemDefinition
 * 
 * Maps to: FR-019, User Story 6
 */
suspend fun EventSystemRuntime.publish(
    channelName: String,
    event: BaseEvent,
    context: HandlerContext? = null
)

/**
 * Gracefully shutdown the event system.
 * 
 * Waits for in-flight handlers to complete up to configured timeout, then
 * forces shutdown. Unsubscribes all channels and releases resources.
 * 
 * @param timeoutMs Override shutdown timeout (uses config default if not provided)
 * @return true if all handlers completed within timeout, false if forced shutdown
 * 
 * Maps to: FR-020, FR-021, FR-022, SC-009
 */
suspend fun EventSystemRuntime.shutdown(timeoutMs: Long? = null): Boolean

/**
 * Get current runtime status.
 * 
 * @return Runtime status including state, in-flight handler count, circuit breaker states
 */
fun EventSystemRuntime.getStatus(): RuntimeStatus

/**
 * Runtime configuration.
 * 
 * All fields have sensible defaults. Immutable after initialization.
 * 
 * Maps to: FR-018, FR-021, FR-013-015
 */
data class RuntimeConfig(
    val shutdownTimeoutMs: Long = 30000,
    val workerPoolSize: Int = Runtime.getRuntime().availableProcessors() * 2,
    val circuitBreakerMaxFailures: Int = 5,
    val circuitBreakerResetTimeoutMs: Long = 30000,
    val circuitBreakerTimeoutMs: Long = 10000,
    val enableCorrelationIdGeneration: Boolean = true,
    val datasource: DataSourceConfig? = null
)

/**
 * Datasource configuration for persistent channels.
 * 
 * Required if SystemDefinition contains persistent channels.
 */
data class DataSourceConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10
)

/**
 * Runtime status information.
 */
data class RuntimeStatus(
    val state: RuntimeState,
    val inFlightHandlers: Int,
    val circuitBreakerStates: Map<String, CircuitBreakerState>,
    val subscribedChannels: Set<String>
)

/**
 * Runtime state enum.
 */
enum class RuntimeState {
    UNINITIALIZED,
    INITIALIZING,
    RUNNING,
    SHUTTING_DOWN,
    SHUTDOWN
}

/**
 * Circuit breaker state enum.
 */
enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

/**
 * Execution interceptor hook.
 * 
 * Invoked before handler execution to modify context.
 * 
 * Maps to: FR-016, User Story 7
 */
typealias ExecutionInterceptor = (context: HandlerContext, event: BaseEvent) -> HandlerContext

/**
 * Execution finaliser hook.
 * 
 * Invoked after handler execution to modify output event.
 * 
 * Maps to: FR-017, User Story 7
 */
typealias ExecutionFinaliser = (context: HandlerContext, event: BaseEvent) -> BaseEvent

/**
 * Register execution interceptor.
 * 
 * Interceptors are invoked in registration order before each handler execution.
 * 
 * @param interceptor Function to modify context before handler execution
 */
fun EventSystemRuntime.registerInterceptor(interceptor: ExecutionInterceptor)

/**
 * Register execution finaliser.
 * 
 * Finalisers are invoked in registration order after each handler execution.
 * 
 * @param finaliser Function to modify output event after handler execution
 */
fun EventSystemRuntime.registerFinaliser(finaliser: ExecutionFinaliser)

/**
 * Exceptions
 */
class ValidationException(message: String, val errors: List<String>) : Exception(message)
class ConfigurationException(message: String) : Exception(message)
class ChannelNotFoundException(channelName: String) : Exception("Channel not found: $channelName")

