# Research: Platform Event Protocol

**Feature**: 001-event-protocol  
**Date**: 2025-12-22  
**Purpose**: Resolve technical unknowns and establish best practices for implementation

## Research Tasks

### 1. Schema Validation in Kotlin (Malli Equivalent)

**Question**: The spec mentions "Malli schema" for BaseEvent validation. What is the Kotlin equivalent for runtime schema validation?

**Research Findings**:

Malli is a Clojure data validation library. For Kotlin, we have several options:

1. **Kotlin Data Classes with Validation** (Recommended)
   - Use Kotlin's type system and data classes for compile-time validation
   - Add runtime validation via `init` blocks or validation functions
   - Leverage Kotlin's null-safety for required/optional fields

2. **kotlinx.serialization with validation**
   - Built-in serialization framework for Kotlin
   - Can add custom validators via serializers
   - Good for JSON/data interchange

3. **Konform** (Third-party library)
   - Dedicated validation library for Kotlin
   - DSL-based validation rules
   - Adds external dependency

**Decision**: Use Kotlin data classes with validation functions

**Rationale**:
- Kotlin's type system provides compile-time safety (required vs optional fields)
- Data classes are idiomatic Kotlin with built-in copy, equals, hashCode
- No external dependencies beyond Kotlin stdlib (aligns with Constitution Principle VII: Simplicity)
- Validation logic can be simple functions or extension methods
- Sufficient for BaseEvent schema requirements (event-id, type, correlation-id, subject, data)

**Alternatives Considered**:
- Konform: Rejected due to external dependency and overkill for simple event validation
- kotlinx.serialization: Rejected as we don't need serialization, just validation
- Manual validation: Rejected as data classes provide better ergonomics

**Implementation Approach**:
```kotlin
data class BaseEvent(
    val eventId: String,
    val type: String,
    val data: Map<String, Any?>,
    val correlationId: String? = null,
    val subject: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(type.isNotBlank()) { "type must not be blank" }
    }
}

// Validation function for additional rules
fun BaseEvent.validate(): Result<BaseEvent> = runCatching {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(type.isNotBlank()) { "type must not be blank" }
    this
}
```

### 2. Metadata Extraction Pattern in Kotlin

**Question**: How to implement metadata extraction (var->meta) for handlers and executors in Kotlin?

**Research Findings**:

The spec mentions extracting metadata from "vars" (Clojure concept). In Kotlin, we need reflection or annotation-based metadata.

**Decision**: Use Kotlin annotations with reflection

**Rationale**:
- Kotlin reflection (kotlin-reflect) allows runtime metadata access
- Annotations are idiomatic for metadata in Kotlin/JVM
- Can define custom annotations for handler metadata (receives/returns event types)
- Reflection overhead is acceptable for metadata extraction (not in hot path)

**Implementation Approach**:
```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HandlerMetadata(
    val receives: Array<String> = [],
    val returns: Array<String> = []
)

// Extract metadata from handler
fun extractMetadata(handler: Any): HandlerMetadata? {
    return handler::class.annotations.filterIsInstance<HandlerMetadata>().firstOrNull()
}
```

**Alternatives Considered**:
- Manual metadata properties: Rejected as less discoverable and requires boilerplate
- Compile-time code generation (KSP): Rejected as overkill for this use case

### 3. Multimethod Pattern for Entity Dispatch

**Question**: How to implement Clojure-style multimethods for entity update dispatch in Kotlin?

**Research Findings**:

Clojure multimethods dispatch on arbitrary functions. In Kotlin, we can use:

1. **Sealed classes with when expressions** (Recommended)
2. **Map-based dispatch**
3. **Strategy pattern with registry**

**Decision**: Use map-based dispatch with type-safe keys

**Rationale**:
- Flexible like Clojure multimethods
- Allows runtime registration of handlers
- Type-safe with Pair<String, String> keys (entity-name, event-type)
- Simple to implement and test

**Implementation Approach**:
```kotlin
typealias EntityUpdateHandler = (context: Any, event: BaseEvent, entity: Any) -> Any

object EntityDispatch {
    private val handlers = mutableMapOf<Pair<String, String>, EntityUpdateHandler>()
    
    fun register(entityName: String, eventType: String, handler: EntityUpdateHandler) {
        handlers[entityName to eventType] = handler
    }
    
    fun dispatch(entityName: String, eventType: String, context: Any, event: BaseEvent, entity: Any): Any {
        val handler = handlers[entityName to eventType] 
            ?: throw IllegalArgumentException("No handler for [$entityName, $eventType]")
        return handler(context, event, entity)
    }
}
```

**Alternatives Considered**:
- Sealed classes: Rejected as requires compile-time knowledge of all entity types
- Reflection-based dispatch: Rejected as more complex and slower

## Summary

All technical unknowns resolved:
1. ✅ Schema validation: Kotlin data classes with init validation
2. ✅ Metadata extraction: Kotlin annotations with reflection
3. ✅ Multimethod dispatch: Map-based dispatch with type-safe keys

No external dependencies required beyond Kotlin stdlib and kotlin-reflect (already available in Kotlin runtime).

