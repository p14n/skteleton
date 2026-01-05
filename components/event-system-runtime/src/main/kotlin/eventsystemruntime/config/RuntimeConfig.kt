package eventsystemruntime.config

/**
 * Runtime configuration for Event System Runtime.
 *
 * All fields have sensible defaults. Immutable after initialization.
 *
 * @property shutdownTimeoutMs Graceful shutdown timeout in milliseconds (default: 30 seconds)
 * @property workerPoolSize Handler thread pool size (default: CPU count * 2)
 * @property circuitBreakerMaxFailures Circuit breaker failure threshold (default: 5)
 * @property circuitBreakerResetTimeoutMs Circuit breaker reset timeout in milliseconds (default: 30 seconds)
 * @property circuitBreakerTimeoutMs Circuit breaker operation timeout in milliseconds (default: 10 seconds)
 * @property enableCorrelationIdGeneration Auto-generate correlation IDs if missing (default: true)
 * @property persistentChannels Set of channel names that should use persistent storage (default: empty)
 * @property datasource Optional datasource configuration for persistent channels
 */
data class RuntimeConfig(
    val shutdownTimeoutMs: Long = 30000,
    val workerPoolSize: Int = Runtime.getRuntime().availableProcessors() * 2,
    val circuitBreakerMaxFailures: Int = 5,
    val circuitBreakerResetTimeoutMs: Long = 30000,
    val circuitBreakerTimeoutMs: Long = 10000,
    val enableCorrelationIdGeneration: Boolean = true,
    val persistentChannels: Set<String> = emptySet(),
    val datasource: DataSourceConfig? = null
) {
    init {
        require(shutdownTimeoutMs > 0) { "shutdownTimeoutMs must be positive" }
        require(workerPoolSize >= 1) { "workerPoolSize must be >= 1" }
        require(circuitBreakerMaxFailures > 0) { "circuitBreakerMaxFailures must be positive" }
        require(circuitBreakerResetTimeoutMs > 0) { "circuitBreakerResetTimeoutMs must be positive" }
        require(circuitBreakerTimeoutMs > 0) { "circuitBreakerTimeoutMs must be positive" }

        // Validate that persistent channels have datasource configuration
        if (persistentChannels.isNotEmpty()) {
            require(datasource != null) { "Persistent channels require datasource configuration" }
        }
    }
}

/**
 * Datasource configuration for persistent channels.
 * 
 * @property jdbcUrl JDBC connection URL
 * @property username Database username
 * @property password Database password
 * @property maxPoolSize Maximum connection pool size (default: 10)
 */
data class DataSourceConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10
) {
    init {
        require(jdbcUrl.isNotBlank()) { "jdbcUrl cannot be blank" }
        require(username.isNotBlank()) { "username cannot be blank" }
        require(maxPoolSize > 0) { "maxPoolSize must be positive" }
    }
}

