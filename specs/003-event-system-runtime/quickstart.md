# Quickstart: Event System Runtime

**Component**: event-system-runtime  
**Version**: 0.1.0  
**Purpose**: Get started with event-driven handler orchestration in 5 minutes

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":components:event-system-runtime"))
    implementation(project(":components:event-protocol"))
    implementation(project(":components:system-definition"))
}
```

## Basic Usage

### 1. Define Your Handlers

Create handlers using the event-protocol component:

```kotlin
import eventprotocol.*

class UserCreatedHandler : IExecute {
    override fun execute(context: HandlerContext, event: BaseEvent): BaseEvent {
        val userId = event.data["userId"] as String
        println("User created: $userId")
        
        // Return output event
        return BaseEvent(
            type = "email.send",
            data = mapOf(
                "to" to event.data["email"],
                "template" to "welcome"
            ),
            correlationId = event.correlationId
        )
    }
    
    override fun operatorMeta() = HandlerMetadata(
        receives = setOf("user.created"),
        returns = setOf("email.send")
    )
}

class EmailSendHandler : IExecute {
    override fun execute(context: HandlerContext, event: BaseEvent): BaseEvent {
        val to = event.data["to"] as String
        println("Sending email to: $to")
        return event
    }
    
    override fun operatorMeta() = HandlerMetadata(
        receives = setOf("email.send"),
        returns = emptySet()
    )
}
```

### 2. Build Your System Definition

```kotlin
import systemdefinition.*

val systemDef = SystemDefinition.Builder()
    // Define channels
    .addChannel("user-events", "User lifecycle events")
    .addChannel("email-queue", "Email delivery queue")
    
    // Define event types and their channels
    .addEvent("user.created", setOf("user-events"))
    .addEvent("email.send", setOf("email-queue"))
    
    // Register handlers
    .addHandler(UserCreatedHandler(), "UserCreatedHandler")
    .addHandler(EmailSendHandler(), "EmailSendHandler")
    
    // Define deployments (optional)
    .addDeployment("user-service", setOf("UserCreatedHandler"))
    .addDeployment("email-service", setOf("EmailSendHandler"))
    
    .build()
```

### 3. Initialize the Runtime

```kotlin
import eventsystemruntime.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Initialize with default configuration
    val runtime = initialize(systemDef)
    
    // Publish an event
    runtime.publish(
        channelName = "user-events",
        event = BaseEvent(
            type = "user.created",
            data = mapOf(
                "userId" to "user_123",
                "email" to "user@example.com"
            )
        )
    )
    
    // Wait a bit for async processing
    delay(1000)
    
    // Graceful shutdown
    runtime.shutdown()
}
```

**Output**:
```
User created: user_123
Sending email to: user@example.com
```

---

## Advanced Usage

### Custom Configuration

Configure timeouts, thread pools, and circuit breakers:

```kotlin
val config = RuntimeConfig(
    shutdownTimeoutMs = 60000,  // 60 seconds
    workerPoolSize = 8,          // 8 worker threads
    circuitBreakerMaxFailures = 3,
    circuitBreakerResetTimeoutMs = 15000,
    enableCorrelationIdGeneration = true
)

val runtime = initialize(systemDef, config)
```

### Persistent Channels

Configure persistent channels with database backing:

```kotlin
val datasource = DataSourceConfig(
    jdbcUrl = "jdbc:postgresql://localhost:5432/events",
    username = "eventuser",
    password = "secret",
    maxPoolSize = 10
)

val config = RuntimeConfig(datasource = datasource)

val systemDef = SystemDefinition.Builder()
    .addChannel("order-events", "Order events", persistent = true)
    .addEvent("order.placed", setOf("order-events"))
    .addHandler(OrderHandler(), "OrderHandler")
    .build()

val runtime = initialize(systemDef, config)
```

**Note**: Persistent channels require datasource configuration. Handlers receive database connection in context.

### Execution Interceptors

Add cross-cutting concerns like logging and tracing:

```kotlin
// Pre-execution interceptor
runtime.registerInterceptor { context, event ->
    println("[TRACE] Processing event: ${event.type} (${event.correlationId})")
    context.copy(
        data = context.data + ("startTime" to System.currentTimeMillis())
    )
}

// Post-execution finaliser
runtime.registerFinaliser { context, event ->
    val startTime = context.data["startTime"] as Long
    val duration = System.currentTimeMillis() - startTime
    println("[TRACE] Completed in ${duration}ms")
    event
}
```

### Correlation ID Tracking

Correlation IDs are automatically generated and propagated:

```kotlin
// Publish event without correlation ID
runtime.publish(
    channelName = "user-events",
    event = BaseEvent(
        type = "user.created",
        data = mapOf("userId" to "user_123")
        // correlationId will be auto-generated
    )
)

// All downstream events will have the same correlation ID
```

### Runtime Status Monitoring

Check runtime health and circuit breaker states:

```kotlin
val status = runtime.getStatus()

println("Runtime state: ${status.state}")
println("In-flight handlers: ${status.inFlightHandlers}")
println("Subscribed channels: ${status.subscribedChannels}")

status.circuitBreakerStates.forEach { (channel, state) ->
    println("Circuit breaker [$channel]: $state")
}
```

---

## Testing

### Unit Testing Handlers

Test handlers in isolation:

```kotlin
import kotlin.test.*

class UserCreatedHandlerTest {
    @Test
    fun `processes user created event`() {
        val handler = UserCreatedHandler()
        val context = HandlerContext(data = emptyMap())
        val event = BaseEvent(
            type = "user.created",
            data = mapOf(
                "userId" to "user_123",
                "email" to "test@example.com"
            )
        )
        
        val result = handler.execute(context, event)
        
        assertEquals("email.send", result.type)
        assertEquals("test@example.com", result.data["to"])
    }
}
```

### Integration Testing Runtime

Test event routing end-to-end:

```kotlin
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class EventSystemRuntimeTest {
    @Test
    fun `routes events through handler chain`() = runBlocking {
        val systemDef = SystemDefinition.Builder()
            .addChannel("test-channel", "Test")
            .addEvent("test.event", setOf("test-channel"))
            .addHandler(TestHandler(), "TestHandler")
            .build()
        
        val runtime = initialize(systemDef)
        
        runtime.publish(
            channelName = "test-channel",
            event = BaseEvent(type = "test.event", data = emptyMap())
        )
        
        delay(100) // Wait for async processing
        
        // Verify handler was invoked
        assertTrue(TestHandler.invoked)
        
        runtime.shutdown()
    }
}
```

---

## Next Steps

- Read the [Data Model](./data-model.md) for entity relationships
- Review [API Contracts](./contracts/EventSystemRuntimeAPI.kt) for full API surface
- Check [Event Routing Contract](./contracts/EventRoutingContract.md) for integration details
- See [Implementation Plan](./plan.md) for architecture decisions

