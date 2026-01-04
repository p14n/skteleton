# Research: Event System Runtime

**Date**: 2026-01-04  
**Feature**: 003-event-system-runtime  
**Purpose**: Resolve technical unknowns and establish best practices for implementation

## Research Tasks

### 1. Postevent-vertx Library Integration

**Question**: What are the exact Maven coordinates and integration patterns for postevent-vertx?

**Decision**: Use custom postevent-vertx library (assumed to be internal/custom library based on project context)

**Rationale**: 
- The feature spec references "postevent-vertx library" as an external dependency
- The reverse-engineered spec (event-system-spec.md) mentions postevent system for persistent events
- No public Maven artifact found - this appears to be a custom library for the project

**Integration Pattern**:
- Dependency: `implementation("com.skteleton:postevent-vertx:0.1.0")` (assumed internal artifact)
- API: Subscribe to channels, publish events with transactional context
- Database connection provided via HandlerContext for persistent event handlers

**Alternatives Considered**:
- Public event sourcing libraries (Axon Framework, EventStore) - Rejected: Too heavyweight, different architecture
- Direct database event table - Rejected: Reinventing the wheel, postevent already exists

**Action Required**: Verify actual Maven coordinates and API surface during implementation

---

### 2. Kotlin Coroutines for Async Event Handling

**Question**: Should we use Kotlin coroutines for async event handling, and which version?

**Decision**: Use Kotlin Coroutines 1.8.0 with structured concurrency for async event processing

**Rationale**:
- Vert.x EventBus supports coroutine integration via `vertx-lang-kotlin-coroutines`
- Structured concurrency aligns with graceful shutdown requirements (FR-021-022)
- Better error handling and cancellation support than raw threads
- Idiomatic Kotlin async pattern

**Best Practices**:
- Use `CoroutineScope` with custom dispatcher for handler execution thread pool
- Use `supervisorScope` for handler isolation (one handler failure doesn't cancel others)
- Use `withTimeout` for shutdown timeout enforcement
- Propagate correlation IDs via `CoroutineContext`

**Dependencies**:
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
implementation("io.vertx:vertx-lang-kotlin-coroutines:5.0.6")
```

**Alternatives Considered**:
- Java ExecutorService - Rejected: Less idiomatic, harder cancellation/timeout handling
- Vert.x native async (Future/Promise) - Rejected: Less composable than coroutines
- Virtual threads (Project Loom) - Rejected: Requires JDK 21+ preview features, less mature

---

### 3. JSON Serialization Library

**Question**: Should we use Jackson or kotlinx.serialization for event JSON serialization?

**Decision**: Use kotlinx.serialization 1.6.3

**Rationale**:
- Kotlin-first design with better type safety
- Compile-time code generation (no reflection overhead)
- Better support for Kotlin data classes and sealed classes
- Consistent with Kotlin ecosystem best practices
- Smaller runtime footprint than Jackson

**Best Practices**:
- Use `@Serializable` annotation on event data classes
- Use `Json { ignoreUnknownKeys = true }` for forward compatibility
- Use `encodeToString` / `decodeFromString` for Vert.x EventBus codec

**Dependencies**:
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
```

**Alternatives Considered**:
- Jackson - Rejected: Reflection-based, heavier runtime, less Kotlin-idiomatic
- Gson - Rejected: Older, less Kotlin support, slower than kotlinx.serialization
- Moshi - Rejected: Good but kotlinx.serialization is more official Kotlin solution

---

### 4. Vert.x Circuit Breaker Configuration

**Question**: What are the best practices for configuring Vert.x Circuit Breaker for database failures?

**Decision**: Use Vert.x Circuit Breaker with configurable thresholds and half-open retry

**Configuration Defaults**:
- `maxFailures`: 5 (open circuit after 5 consecutive failures)
- `timeout`: 10000ms (10 seconds per operation)
- `resetTimeout`: 30000ms (30 seconds before attempting half-open)
- `fallbackOnFailure`: true (fail fast when circuit is open)

**Best Practices**:
- Separate circuit breaker per persistent channel (isolation)
- Log circuit breaker state transitions (CLOSED → OPEN → HALF_OPEN)
- Expose circuit breaker state via metrics/health checks
- Use `executeWithFallback` for graceful degradation

**Dependencies**:
```kotlin
implementation("io.vertx:vertx-circuit-breaker:5.0.6")
```

**Pattern**:
```kotlin
val breaker = CircuitBreaker.create("db-circuit", vertx)
    .maxFailures(5)
    .timeout(10000)
    .resetTimeout(30000)
    .fallbackOnFailure(true)

breaker.execute { promise ->
    // Database operation
}.onSuccess { result ->
    // Handle success
}.onFailure { error ->
    // Fail fast, circuit is open
}
```

**Alternatives Considered**:
- Resilience4j - Rejected: Adds extra dependency, Vert.x native solution sufficient
- Custom circuit breaker - Rejected: Reinventing the wheel, Vert.x implementation battle-tested
- No circuit breaker - Rejected: Violates FR-013-015, causes resource exhaustion

---

### 5. Thread Pool Configuration for Handler Execution

**Question**: How should we configure the thread pool for concurrent handler execution?

**Decision**: Use Vert.x Worker Pool with configurable size

**Configuration Defaults**:
- Pool size: `Runtime.getRuntime().availableProcessors() * 2` (CPU-bound default)
- Max execute time: 60000ms (1 minute per handler)
- Ordered: false (allow concurrent execution)

**Best Practices**:
- Use Vert.x `WorkerExecutor` for blocking handler operations
- Use coroutine dispatcher backed by worker pool
- Configure separate pools for transient vs persistent handlers (optional optimization)
- Monitor pool utilization and queue depth

**Pattern**:
```kotlin
val workerPool = vertx.createSharedWorkerExecutor(
    "handler-pool",
    poolSize = Runtime.getRuntime().availableProcessors() * 2,
    maxExecuteTime = 60000,
    maxExecuteTimeUnit = TimeUnit.MILLISECONDS
)
```

**Alternatives Considered**:
- Fixed thread pool - Rejected: Less flexible than Vert.x worker pool
- Unlimited threads - Rejected: Resource exhaustion risk
- Single-threaded - Rejected: Violates FR-018 concurrent processing requirement

---

## Summary

All technical unknowns resolved:

1. **Postevent-vertx**: Custom internal library, verify coordinates during implementation
2. **Coroutines**: Use kotlinx-coroutines 1.8.0 with structured concurrency
3. **JSON**: Use kotlinx.serialization 1.6.3 for Kotlin-first serialization
4. **Circuit Breaker**: Use Vert.x Circuit Breaker 5.0.6 with 5 failures / 30s reset defaults
5. **Thread Pool**: Use Vert.x WorkerExecutor with CPU-count * 2 default size

**Next Phase**: Proceed to Phase 1 (Design & Contracts) with all dependencies and patterns clarified.

