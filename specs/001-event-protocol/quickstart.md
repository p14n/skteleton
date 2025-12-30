# Quickstart Guide: Platform Event Protocol

**Feature**: 001-event-protocol  
**Date**: 2025-12-22  
**Audience**: Domain developers implementing event handlers

## Overview

The Platform Event Protocol provides a standardized way to process events through a three-phase pipeline:
1. **Lookup**: Fetch data needed for processing
2. **Operate**: Transform the event using lookup data
3. **Write**: Persist changes

Three handler types cover all use cases:
- **SimpleHandler**: Pure transformations (no database interaction)
- **LookupHandler**: Read from database before processing
- **LookupWriterHandler**: Full CRUD operations (read, process, write)

## Installation

The event-protocol component is available in the `components/` directory:

```kotlin
// In your Mill build.sc
object myComponent extends CommonKotlinModule {
  def moduleDeps = Seq(components.`event-protocol`)
}
```

## Basic Usage

### 1. Create a Simple Event

```kotlin
import eventprotocol.*

val event = BaseEvent(
    eventId = "evt_123",
    type = "user_registered",
    data = mapOf(
        "userId" to "user_456",
        "email" to "user@example.com"
    ),
    correlationId = "req_789"  // Optional: for tracing
)
```

### 2. SimpleHandler - Pure Transformation

Use when you don't need database access:

```kotlin
val validateEmail = SimpleHandler(
    operator = { context, event, _ ->
        val email = event.data["email"] as? String
        require(email?.contains("@") == true) { "Invalid email" }
        event  // Return unchanged or create new event
    },
    metadata = HandlerMetadata(
        receives = setOf("user_registered"),
        returns = setOf("user_registered")
    )
)

// Execute the handler
val context = HandlerContext(requestId = "req_001")
val executor = Executor(validateEmail)
val result = executor.execute(context, event)
```

### 3. LookupHandler - Read Before Processing

Use when you need to check existing data:

```kotlin
val checkDuplicateUser = LookupHandler(
    lookerUpper = { context, event ->
        val email = event.data["email"] as String
        val existingUser = database.findUserByEmail(email)
        LookupData(data = mapOf("existingUser" to existingUser))
    },
    operator = { context, event, data ->
        val existingUser = data.data["existingUser"]
        if (existingUser != null) {
            throw IllegalStateException("User already exists")
        }
        event
    },
    metadata = HandlerMetadata(
        receives = setOf("user_registered"),
        returns = setOf("user_registered")
    )
)
```

### 4. LookupWriterHandler - Full CRUD

Use for complete operations that read, process, and write:

```kotlin
val createUser = LookupWriterHandler(
    lookerUpper = { context, event ->
        // Check if user exists
        val email = event.data["email"] as String
        val existingUser = database.findUserByEmail(email)
        LookupData(data = mapOf("existingUser" to existingUser))
    },
    operator = { context, event, data ->
        // Validate and enrich event
        require(data.data["existingUser"] == null) { "User exists" }
        event.copy(
            data = event.data + ("createdAt" to System.currentTimeMillis())
        )
    },
    writer = { context, event ->
        // Persist to database
        val user = User(
            id = event.data["userId"] as String,
            email = event.data["email"] as String,
            createdAt = event.data["createdAt"] as Long
        )
        database.save(user)
        event
    },
    metadata = HandlerMetadata(
        receives = setOf("user_registered"),
        returns = setOf("user_created")
    )
)
```

## Advanced Usage

### Correlation Tracking

Preserve correlation IDs for distributed tracing:

```kotlin
val event = BaseEvent(
    eventId = "evt_123",
    type = "order_placed",
    data = mapOf("orderId" to "order_456"),
    correlationId = "trace_789"  // Preserved through pipeline
)

// Correlation ID is automatically preserved through all handler phases
```

### Handler Metadata Extraction

Build routing tables from handler metadata:

```kotlin
val handlers = listOf(validateEmail, checkDuplicateUser, createUser)

val routingTable = handlers.associate { handler ->
    val meta = handler.operatorMeta()
    meta.receives to handler
}

// Route event to appropriate handler
val handler = routingTable[setOf(event.type)]
```

### Entity Update Dispatch

Register polymorphic entity update handlers:

```kotlin
// Register handlers for different entity types
EntityDispatch.register("user", "user_created") { context, event, entity ->
    (entity as User).copy(status = "active")
}

EntityDispatch.register("order", "order_placed") { context, event, entity ->
    (entity as Order).copy(status = "pending")
}

// Dispatch based on entity type and event type
val updatedUser = EntityDispatch.dispatch(
    entityName = "user",
    eventType = "user_created",
    context = context,
    event = event,
    entity = user
)
```

## Testing

### Unit Testing Handlers

```kotlin
@Test
fun `SimpleHandler validates email format`() {
    val handler = SimpleHandler(
        operator = { _, event, _ ->
            val email = event.data["email"] as? String
            require(email?.contains("@") == true) { "Invalid email" }
            event
        },
        metadata = HandlerMetadata(
            receives = setOf("user_registered"),
            returns = setOf("user_registered")
        )
    )
    
    val context = HandlerContext(requestId = "test_001")
    val validEvent = BaseEvent(
        eventId = "evt_1",
        type = "user_registered",
        data = mapOf("email" to "user@example.com")
    )
    
    val result = handler.operate(context, validEvent, LookupData())
    assertEquals(validEvent, result)
}
```

## Next Steps

- See [data-model.md](./data-model.md) for detailed entity definitions
- See [contracts/event-protocol.kt](./contracts/event-protocol.kt) for API contracts
- See [spec.md](./spec.md) for complete feature specification
- Run tests: `mill components.event-protocol.test`

