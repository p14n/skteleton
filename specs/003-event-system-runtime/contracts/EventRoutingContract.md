# Event Routing Contract

**Version**: 0.1.0  
**Date**: 2026-01-04  
**Purpose**: Define event routing behavior and integration contracts

## Vert.x EventBus Integration Contract

### Transient Channel Address Pattern

**Contract**: Events published to transient channels MUST use the address pattern `{channel}.{event-type}`

**Example**:
- Channel: `user-events`
- Event Type: `user.created`
- Address: `user-events.user.created`

**Rationale**: Enables type-specific subscriptions while maintaining channel isolation (FR-004)

**Test Verification**:
```kotlin
@Test
fun `transient channel uses correct address pattern`() {
    val channel = "user-events"
    val eventType = "user.created"
    val expectedAddress = "user-events.user.created"
    
    // Verify subscription address
    verify(vertxEventBus).consumer(expectedAddress, any())
}
```

---

### Event Serialization Contract

**Contract**: Events MUST be serialized to JSON using kotlinx.serialization before publishing to Vert.x EventBus

**Format**:
```json
{
  "eventId": "evt_123",
  "type": "user.created",
  "data": {
    "userId": "user_456",
    "email": "user@example.com"
  },
  "correlationId": "trace_789",
  "subject": "user_456"
}
```

**Rationale**: Vert.x EventBus requires serializable messages for cross-JVM communication (FR-010)

**Test Verification**:
```kotlin
@Test
fun `events are serialized to JSON`() {
    val event = BaseEvent(
        eventId = "evt_123",
        type = "user.created",
        data = mapOf("userId" to "user_456"),
        correlationId = "trace_789"
    )
    
    val json = Json.encodeToString(event)
    val deserialized = Json.decodeFromString<BaseEvent>(json)
    
    assertEquals(event, deserialized)
}
```

---

## Postevent Integration Contract

### Persistent Channel Publishing

**Contract**: Events published to persistent channels MUST use the postevent system's publish API with transactional context

**API Signature** (assumed):
```kotlin
suspend fun PosteventSystem.publish(
    channel: String,
    event: BaseEvent,
    connection: Connection? = null
)
```

**Behavior**:
- If `connection` is provided, publish transactionally
- If `connection` is null, publish in auto-commit mode
- Event is durably stored in database before delivery

**Rationale**: Enables transactional event publishing with database consistency (FR-011)

**Test Verification**:
```kotlin
@Test
fun `persistent channel publishes via postevent`() {
    val event = BaseEvent(type = "order.placed", data = mapOf())
    
    runBlocking {
        runtime.publish("order-events", event)
    }
    
    verify(posteventSystem).publish("order-events", event, null)
}
```

---

### Persistent Channel Subscription

**Contract**: Handlers subscribed to persistent channels MUST receive `HandlerContext` with database connection

**Context Fields**:
- `context.connection: Connection` - Database connection for transactional operations
- `context.eventBus: EventBus` - Vert.x EventBus for publishing output events
- `context.data: Map<String, Any>` - Request-scoped data

**Rationale**: Enables handlers to perform database operations within the same transaction as event processing (FR-011)

**Test Verification**:
```kotlin
@Test
fun `persistent handler receives database connection`() {
    var receivedContext: HandlerContext? = null
    
    val handler = object : IExecute {
        override fun execute(context: HandlerContext, event: BaseEvent): BaseEvent {
            receivedContext = context
            return event
        }
    }
    
    // Trigger event processing
    // ...
    
    assertNotNull(receivedContext?.connection)
}
```

---

## Handler Execution Contract

### Handler Type Adaptation

**Contract**: Runtime MUST support three handler types with consistent execution semantics

**Supported Types**:

1. **IHandler** - Three-phase pipeline
   ```kotlin
   interface IHandler {
       fun lookup(context: HandlerContext, event: BaseEvent): LookupData
       fun operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent
       fun write(context: HandlerContext, event: BaseEvent): BaseEvent
       fun operatorMeta(): HandlerMetadata
   }
   ```
   Execution: Wrapped in `Executor` from event-protocol component

2. **IExecute** - Single execute method
   ```kotlin
   interface IExecute {
       fun execute(context: HandlerContext, event: BaseEvent): BaseEvent
       fun operatorMeta(): HandlerMetadata
   }
   ```
   Execution: Invoked directly

3. **Plain Function** - Kotlin function
   ```kotlin
   typealias HandlerFunction = (HandlerContext, BaseEvent) -> BaseEvent
   ```
   Execution: Invoked directly

**Rationale**: Developer flexibility while maintaining consistent execution model (FR-006)

---

### Handler Failure Isolation

**Contract**: Handler exceptions MUST NOT prevent other handlers from processing the same event

**Behavior**:
1. Handler throws exception
2. Exception is logged with correlation ID and handler name
3. Other handlers subscribed to the same event continue execution
4. Event is NOT marked as failed (best-effort delivery)

**Rationale**: Fault isolation prevents cascading failures (FR-024)

**Test Verification**:
```kotlin
@Test
fun `handler exception does not prevent other handlers`() {
    val failingHandler = mockk<IExecute> {
        every { execute(any(), any()) } throws RuntimeException("Handler failed")
    }
    val successHandler = mockk<IExecute> {
        every { execute(any(), any()) } returns event
    }
    
    // Both handlers subscribed to same event
    // Publish event
    // Verify successHandler was invoked despite failingHandler exception
    
    verify { successHandler.execute(any(), any()) }
}
```

---

## Correlation ID Contract

### Correlation ID Propagation

**Contract**: Correlation IDs MUST be preserved across all event flows

**Behavior**:
1. Extract correlation ID from incoming event
2. If missing, generate new UUID
3. Propagate to all output events from handlers
4. Store in CoroutineContext for async propagation

**Format**: UUID v4 string (e.g., `"550e8400-e29b-41d4-a716-446655440000"`)

**Rationale**: Enables distributed tracing and debugging (FR-009)

**Test Verification**:
```kotlin
@Test
fun `correlation ID is preserved across handlers`() {
    val correlationId = UUID.randomUUID().toString()
    val inputEvent = BaseEvent(
        type = "user.created",
        data = mapOf(),
        correlationId = correlationId
    )
    
    // Publish event, verify output events have same correlation ID
    // ...
    
    assertEquals(correlationId, outputEvent.correlationId)
}
```

---

## Circuit Breaker Contract

### Circuit Breaker State Transitions

**Contract**: Circuit breakers MUST transition states based on failure count and timeout

**States**:
- `CLOSED`: Normal operation, requests pass through
- `OPEN`: Failure threshold exceeded, requests fail fast
- `HALF_OPEN`: Reset timeout elapsed, testing if service recovered

**Transitions**:
```
CLOSED --[N failures]--> OPEN
OPEN --[reset timeout]--> HALF_OPEN
HALF_OPEN --[success]--> CLOSED
HALF_OPEN --[failure]--> OPEN
```

**Configuration**:
- `maxFailures`: 5 (default)
- `resetTimeout`: 30000ms (default)

**Rationale**: Prevents resource exhaustion during database failures (FR-013-015)

**Test Verification**:
```kotlin
@Test
fun `circuit breaker opens after max failures`() {
    // Simulate 5 consecutive database failures
    repeat(5) {
        // Trigger database operation that fails
    }
    
    val status = runtime.getStatus()
    assertEquals(CircuitBreakerState.OPEN, status.circuitBreakerStates["order-events"])
}
```

---

## Shutdown Contract

### Graceful Shutdown Behavior

**Contract**: Shutdown MUST wait for in-flight handlers up to timeout, then force shutdown

**Behavior**:
1. Set shutdown flag (reject new events)
2. Wait for in-flight handlers to complete
3. If timeout exceeded, interrupt remaining handlers
4. Unsubscribe all channels
5. Close worker pool and circuit breakers
6. Return true if all handlers completed, false if forced

**Default Timeout**: 30000ms (30 seconds)

**Rationale**: Prevents data loss while ensuring bounded shutdown time (FR-021-022)

**Test Verification**:
```kotlin
@Test
fun `shutdown waits for in-flight handlers`() = runBlocking {
    // Start long-running handler
    launch {
        runtime.publish("test-channel", event)
    }
    
    val startTime = System.currentTimeMillis()
    val completed = runtime.shutdown()
    val duration = System.currentTimeMillis() - startTime
    
    assertTrue(completed)
    assertTrue(duration < 30000) // Completed before timeout
}
```

