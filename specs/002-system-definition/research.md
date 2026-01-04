# Research: System Definition Component

**Date**: 2026-01-03  
**Feature**: 002-system-definition

## Research Questions

### 1. D2 Diagram Generation Library

**Question**: What library or approach should be used for generating D2 diagrams from Kotlin?

**Decision**: Generate D2 syntax as plain text strings - no external library needed

**Rationale**:
- D2 is a text-based diagram language with simple, human-readable syntax
- D2 syntax is straightforward: `node1 -> node2: label` for edges, `node { }` for containers
- No Kotlin-specific D2 library exists or is needed
- Generating plain text is simpler, more maintainable, and has zero dependencies
- D2 tool itself handles rendering (SVG/PNG/PDF) from the generated text
- This aligns with Constitution Principle VII (Simplicity and YAGNI)

**Alternatives Considered**:
- **Kotlin DSL wrapper for D2**: Would add unnecessary complexity and maintenance burden. D2 syntax is already simple enough that string generation is clearer.
- **JVM bindings to D2 Go library**: Would require native bindings, complex build setup, and tight coupling to D2 internals. Overkill for text generation.
- **Third-party Kotlin D2 library**: None exist as of 2026-01-03. Creating one would violate YAGNI.

**Implementation Approach**:
```kotlin
// Simple string builder approach
fun SystemDefinition.toD2(): String {
    val builder = StringBuilder()
    
    // Add handlers as nodes
    handlers.forEach { handler ->
        builder.appendLine("${handler.name} {")
        builder.appendLine("  shape: rectangle")
        builder.appendLine("}")
    }
    
    // Add event flows as edges
    eventFlows.forEach { flow ->
        builder.appendLine("${flow.from} -> ${flow.to}: ${flow.eventType}")
    }
    
    // Add deployment groupings as containers
    deployments.forEach { deployment ->
        builder.appendLine("${deployment.name} {")
        deployment.handlers.forEach { handler ->
            builder.appendLine("  ${handler.name}")
        }
        builder.appendLine("}")
    }
    
    return builder.toString()
}
```

### 2. D2 Syntax Best Practices

**Research Findings**:
- **Nodes**: Simple identifiers or quoted strings: `handler1` or `"User Handler"`
- **Edges**: `source -> target: label` for directed edges
- **Containers**: `container { child1; child2 }` for grouping
- **Shapes**: `shape: rectangle|circle|cylinder|...` for node types
- **Styles**: `style.fill: "#color"` for customization
- **Labels**: Use `:` after edge definition for labels
- **Multi-line**: Semicolons or newlines separate statements

**Best Practices for Event-Driven Systems**:
- Use rectangles for handlers
- Use edge labels for event types
- Use containers for deployment units or channels
- Use different colors/styles to distinguish handler types
- Keep node names concise but descriptive

### 3. Builder Pattern for Immutable System Definitions

**Decision**: Use Kotlin data class with builder pattern

**Rationale**:
- Kotlin data classes provide immutability by default (val properties)
- Builder pattern allows fluent, readable construction
- Kotlin's `apply` and `also` functions make builders natural
- Aligns with clarification decision for immutability

**Implementation Pattern**:
```kotlin
data class SystemDefinition(
    val handlers: Set<HandlerRegistration>,
    val events: Set<EventDefinition>,
    val channels: Set<Channel>,
    val deployments: Set<Deployment>
) {
    class Builder {
        private val handlers = mutableSetOf<HandlerRegistration>()
        private val events = mutableSetOf<EventDefinition>()
        private val channels = mutableSetOf<Channel>()
        private val deployments = mutableSetOf<Deployment>()
        
        fun addHandler(handler: IHandler, name: String) = apply {
            handlers.add(HandlerRegistration(name, handler))
        }
        
        fun addEvent(eventType: String, channels: Set<String>) = apply {
            events.add(EventDefinition(eventType, channels))
        }
        
        fun build(): SystemDefinition {
            return SystemDefinition(
                handlers.toSet(),
                events.toSet(),
                channels.toSet(),
                deployments.toSet()
            )
        }
    }
}

// Usage
val system = SystemDefinition.Builder()
    .addHandler(userHandler, "UserHandler")
    .addEvent("user.created", setOf("user-channel"))
    .build()
```

### 4. Verification Algorithm Design

**Decision**: Graph-based verification with separate error and warning categories

**Rationale**:
- Event flows form a directed graph (handlers as nodes, events as edges)
- Graph traversal can detect circular flows, orphaned events, and mismatches
- Separate error vs warning categories provides flexibility (per clarifications)

**Algorithm Outline**:
1. Build event flow graph from handler metadata
2. Check each handler's returned events are in system definition (ERROR if not)
3. Check each handler only receives declared events (ERROR if not)
4. Check for events with no handlers (WARNING)
5. Detect circular flows using DFS cycle detection (WARNING)
6. Check terminal handlers (empty returns) are valid (always valid per clarifications)

## Summary

All NEEDS CLARIFICATION items resolved:
- ✅ D2 generation: Plain text string generation, no library needed
- ✅ Builder pattern: Kotlin data class with builder for immutability
- ✅ Verification: Graph-based algorithm with error/warning distinction

No blocking technical issues identified. Ready to proceed to Phase 1 (Data Model & Contracts).

