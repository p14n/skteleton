# Quickstart: System Definition Component

**Date**: 2026-01-03  
**Feature**: 002-system-definition

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":components:system-definition"))
    implementation(project(":components:event-protocol"))
}
```

## Basic Usage

### 1. Define Your Handlers

First, create handlers using the event-protocol component:

```kotlin
import eventprotocol.*

class UserCreatedHandler : IHandler {
    override fun operatorMeta() = HandlerMetadata(
        receives = setOf("user.created"),
        returns = setOf("email.send"),
        description = "Sends welcome email when user is created"
    )
    
    override fun lookup(context: HandlerContext, event: BaseEvent): LookupData {
        // Implementation
    }
    
    override fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent {
        // Implementation
    }
    
    override fun write(context: HandlerContext, event: BaseEvent): BaseEvent {
        // Implementation
    }
}

class EmailSendHandler : IHandler {
    override fun operatorMeta() = HandlerMetadata(
        receives = setOf("email.send"),
        returns = emptySet(), // Terminal handler
        description = "Sends emails via SMTP"
    )
    
    // ... implementation
}
```

### 2. Build Your System Definition

```kotlin
import systemdefinition.*

val system = SystemDefinition.Builder()
    .addChannel("user-events", "User lifecycle events")
    .addChannel("email-queue", "Email delivery queue")
    .addEvent("user.created", setOf("user-events"))
    .addEvent("email.send", setOf("email-queue"))
    .addHandler(UserCreatedHandler(), "UserCreatedHandler")
    .addHandler(EmailSendHandler(), "EmailSendHandler")
    .addDeployment("user-service", setOf("UserCreatedHandler"))
    .addDeployment("email-service", setOf("EmailSendHandler"))
    .build()
```

### 3. Verify Your System

```kotlin
val result = system.verify()

if (!result.isValid) {
    println("System has configuration errors:")
    result.errors.forEach { error ->
        println("  [ERROR] ${error.message}")
    }
}

if (result.warnings.isNotEmpty()) {
    println("System has warnings:")
    result.warnings.forEach { warning ->
        println("  [WARN] ${warning.message}")
    }
}
```

### 4. Generate D2 Diagram

```kotlin
val d2Syntax = system.toD2()
println(d2Syntax)

// Save to file
File("system-diagram.d2").writeText(d2Syntax)

// Render with D2 CLI (requires d2 installed)
// $ d2 system-diagram.d2 system-diagram.svg
```

## Example Output

### Verification Result

```
System has warnings:
  [WARN] Circular flow detected: UserCreatedHandler -> email.send -> EmailSendHandler -> user.updated -> UserCreatedHandler
```

### D2 Diagram Syntax

```d2
# Handlers
UserCreatedHandler {
  shape: rectangle
  label: "UserCreatedHandler\nReceives: user.created\nReturns: email.send"
}

EmailSendHandler {
  shape: rectangle
  label: "EmailSendHandler\nReceives: email.send\nReturns: (none)"
}

# Event Flows
UserCreatedHandler -> EmailSendHandler: email.send

# Deployments
user-service {
  UserCreatedHandler
}

email-service {
  EmailSendHandler
}
```

## Advanced Usage

### Querying Event Flows

```kotlin
// Find all handlers that produce a specific event
val producers = system.getProducersOf("email.send")
println("Handlers producing email.send: ${producers.map { it.name }}")

// Find all handlers that consume a specific event
val consumers = system.getConsumersOf("user.created")
println("Handlers consuming user.created: ${consumers.map { it.name }}")

// Get all events routed to a channel
val channelEvents = system.getEventsForChannel("user-events")
println("Events on user-events: ${channelEvents.map { it.eventType }}")
```

### Custom Verification

```kotlin
// Verify only specific aspects
val hasOrphanedEvents = system.verify().warnings.any { 
    it.type == WarningType.ORPHANED_EVENT 
}

if (hasOrphanedEvents) {
    println("Warning: System has events with no handlers")
}
```

### Programmatic D2 Customization

```kotlin
// Generate D2 with custom styling
val d2Syntax = system.toD2(
    handlerShape = "cylinder",
    eventLabelFormat = { event -> "Event: ${event.eventType}" },
    includeDeployments = true
)
```

## Testing Example

```kotlin
import kotlin.test.*

class SystemDefinitionTest {
    @Test
    fun `system with valid configuration should verify successfully`() {
        val system = SystemDefinition.Builder()
            .addChannel("test-channel")
            .addEvent("test.event", setOf("test-channel"))
            .addHandler(TestHandler(), "TestHandler")
            .build()
        
        val result = system.verify()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `system with orphaned event should produce warning`() {
        val system = SystemDefinition.Builder()
            .addChannel("test-channel")
            .addEvent("orphaned.event", setOf("test-channel"))
            // No handler produces or consumes this event
            .build()
        
        val result = system.verify()
        
        assertTrue(result.isValid) // Still valid, just a warning
        assertTrue(result.warnings.any { it.type == WarningType.ORPHANED_EVENT })
    }
}
```

## Next Steps

- See `data-model.md` for complete entity reference
- See `research.md` for design decisions and alternatives
- See test files for comprehensive examples

