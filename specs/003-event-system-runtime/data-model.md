# Data Model: Event System Runtime

**Date**: 2026-01-04  
**Feature**: 003-event-system-runtime  
**Purpose**: Define core entities, relationships, and state transitions

## Core Entities

### 1. EventSystemRuntime

**Purpose**: Main orchestrator for event routing and handler subscription

**Fields**:
- `systemDefinition: SystemDefinition` - Immutable configuration (from system-definition component)
- `vertxEventBus: EventBus` - Vert.x EventBus instance for transient channels
- `posteventSystem: PosteventSystem?` - Postevent system for persistent channels (nullable if no persistent channels)
- `channelSubscribers: Map<String, ChannelSubscriber>` - Active subscriptions by channel name
- `circuitBreakers: Map<String, CircuitBreaker>` - Circuit breakers per persistent channel
- `workerPool: WorkerExecutor` - Thread pool for handler execution
- `config: RuntimeConfig` - Runtime configuration (timeouts, pool sizes, circuit breaker settings)
- `shutdownCoordinator: ShutdownCoordinator` - Manages graceful shutdown
- `correlationIdManager: CorrelationIdManager` - Tracks correlation IDs
- `interceptorChain: InterceptorChain?` - Optional execution interceptors

**Relationships**:
- Contains 1 SystemDefinition (immutable)
- Contains 0..N ChannelSubscribers (one per channel)
- Contains 0..N CircuitBreakers (one per persistent channel)
- Contains 1 ShutdownCoordinator
- Contains 1 CorrelationIdManager

**State Transitions**:
```
UNINITIALIZED → INITIALIZING → RUNNING → SHUTTING_DOWN → SHUTDOWN
```

**Validation Rules** (FR-002, FR-003):
- SystemDefinition must be valid (no configuration errors)
- Persistent channels must have datasource configuration
- All handlers must have valid metadata (receives/returns sets)
- Event type to channel mappings must be complete

---

### 2. RuntimeConfig

**Purpose**: Configuration for runtime behavior

**Fields**:
- `shutdownTimeoutMs: Long = 30000` - Graceful shutdown timeout (FR-021)
- `workerPoolSize: Int = Runtime.getRuntime().availableProcessors() * 2` - Handler thread pool size
- `circuitBreakerMaxFailures: Int = 5` - Circuit breaker failure threshold
- `circuitBreakerResetTimeoutMs: Long = 30000` - Circuit breaker reset timeout
- `circuitBreakerTimeoutMs: Long = 10000` - Circuit breaker operation timeout
- `enableCorrelationIdGeneration: Boolean = true` - Auto-generate correlation IDs if missing

**Validation Rules**:
- All timeout values must be positive
- Worker pool size must be >= 1

---

### 3. ChannelSubscriber

**Purpose**: Manages subscriptions for a single channel

**Fields**:
- `channelName: String` - Channel identifier
- `channelType: ChannelType` - TRANSIENT or PERSISTENT
- `handlers: List<HandlerRegistration>` - Handlers subscribed to this channel
- `eventRouter: EventRouter` - Routes events to handlers
- `subscriptionId: String?` - Vert.x or postevent subscription ID

**Relationships**:
- Contains 1..N HandlerRegistrations
- Contains 1 EventRouter

**State Transitions**:
```
UNSUBSCRIBED → SUBSCRIBING → SUBSCRIBED → UNSUBSCRIBING → UNSUBSCRIBED
```

---

### 4. ChannelType (Enum)

**Purpose**: Distinguish transient vs persistent channels

**Values**:
- `TRANSIENT` - In-memory Vert.x EventBus
- `PERSISTENT` - Database-backed postevent system

---

### 5. EventRouter

**Purpose**: Routes incoming events to appropriate handlers

**Fields**:
- `handlers: List<HandlerRegistration>` - Handlers for this channel
- `handlerAdapter: HandlerAdapter` - Adapts different handler types
- `correlationIdManager: CorrelationIdManager` - Manages correlation IDs
- `interceptorChain: InterceptorChain?` - Optional interceptors

**Behavior**:
- Receives event from channel
- Ensures correlation ID exists (FR-009)
- Invokes all matching handlers concurrently (FR-025)
- Isolates handler failures (FR-024)
- Publishes output events to configured channels (FR-008)

---

### 6. HandlerAdapter

**Purpose**: Adapt different handler types to common execution interface

**Supported Types** (FR-006):
- `IHandler` - Three-phase pipeline (lookup → operate → write)
- `IExecute` - Single execute method
- `Function` - Plain Kotlin function `(HandlerContext, BaseEvent) -> BaseEvent`

**Behavior**:
- Wraps IHandler in Executor (from event-protocol component)
- Invokes IExecute.execute directly
- Invokes plain functions directly
- Returns output event for routing

---

### 7. CircuitBreakerManager

**Purpose**: Manages circuit breakers for persistent channels

**Fields**:
- `breakers: Map<String, CircuitBreaker>` - Circuit breaker per channel
- `config: RuntimeConfig` - Circuit breaker configuration

**Behavior**:
- Creates circuit breaker for each persistent channel
- Wraps database operations in circuit breaker
- Logs state transitions (CLOSED → OPEN → HALF_OPEN)
- Fails fast when circuit is open (FR-014)
- Retries periodically when circuit is half-open (FR-015)

**State Transitions** (per circuit breaker):
```
CLOSED → OPEN → HALF_OPEN → CLOSED (or back to OPEN)
```

---

### 8. ShutdownCoordinator

**Purpose**: Orchestrate graceful shutdown

**Fields**:
- `inFlightHandlers: AtomicInteger` - Count of currently executing handlers
- `shutdownRequested: AtomicBoolean` - Shutdown flag
- `shutdownLatch: CountDownLatch` - Synchronization for shutdown completion

**Behavior**:
- Tracks in-flight handler count
- Waits for handlers to complete up to timeout (FR-021)
- Forces shutdown if timeout exceeded (FR-022)
- Unsubscribes all channels
- Closes worker pool and circuit breakers

**State Transitions**:
```
RUNNING → SHUTDOWN_REQUESTED → WAITING_FOR_HANDLERS → SHUTDOWN_COMPLETE
```

---

### 9. CorrelationIdManager

**Purpose**: Manage correlation IDs across event flows

**Behavior**:
- Extract correlation ID from incoming event
- Generate new UUID if missing (FR-009)
- Propagate correlation ID to output events
- Store correlation ID in CoroutineContext for async propagation

---

### 10. InterceptorChain

**Purpose**: Execute interceptors and finalisers around handler execution

**Fields**:
- `interceptors: List<ExecutionInterceptor>` - Pre-execution hooks (FR-016)
- `finalisers: List<ExecutionFinaliser>` - Post-execution hooks (FR-017)

**Behavior**:
- Invoke interceptors before handler execution
- Allow interceptors to modify context
- Invoke finalisers after handler execution
- Allow finalisers to modify output events

---

## Relationships Diagram

```
EventSystemRuntime
├── SystemDefinition (1)
├── ChannelSubscriber (0..N)
│   ├── EventRouter (1)
│   │   ├── HandlerAdapter (1)
│   │   ├── CorrelationIdManager (1)
│   │   └── InterceptorChain (0..1)
│   └── HandlerRegistration (1..N)
├── CircuitBreakerManager (1)
│   └── CircuitBreaker (0..N, one per persistent channel)
├── ShutdownCoordinator (1)
└── RuntimeConfig (1)
```

---

## State Transition Summary

| Entity | States | Triggers |
|--------|--------|----------|
| EventSystemRuntime | UNINITIALIZED → INITIALIZING → RUNNING → SHUTTING_DOWN → SHUTDOWN | initialize(), shutdown() |
| ChannelSubscriber | UNSUBSCRIBED → SUBSCRIBING → SUBSCRIBED → UNSUBSCRIBING → UNSUBSCRIBED | subscribe(), unsubscribe() |
| CircuitBreaker | CLOSED → OPEN → HALF_OPEN → CLOSED | failure count, timeout, success |
| ShutdownCoordinator | RUNNING → SHUTDOWN_REQUESTED → WAITING_FOR_HANDLERS → SHUTDOWN_COMPLETE | shutdown(), timeout |

---

## Validation Rules Summary

**SystemDefinition Validation** (FR-002, FR-003):
- All handlers have valid metadata
- All event types have channel mappings
- Persistent channels have datasource configuration
- No circular event routing (optional check)

**RuntimeConfig Validation**:
- All timeouts > 0
- Worker pool size >= 1
- Circuit breaker thresholds > 0

**Event Validation**:
- Event type is non-empty
- Event data is valid JSON
- Correlation ID is valid UUID (if present)

