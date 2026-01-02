# Data Model: Platform Event Protocol

**Feature**: 001-event-protocol  
**Date**: 2025-12-22  
**Purpose**: Define core entities, their relationships, and validation rules

## Core Entities

### 1. BaseEvent

**Purpose**: Foundation schema for all events in the system

**Fields**:
- `eventId: String` (required) - Unique identifier for the event
- `type: String` (required) - Event type identifier (e.g., "party_created", "order_placed")
- `data: Map<String, Any?>` (required) - Event payload data
- `correlationId: String?` (optional) - Correlation ID for tracing related events
- `subject: String?` (optional) - Subject/entity the event relates to
- `timestamp: Long` (auto-generated) - Event creation timestamp in milliseconds

**Validation Rules**:
- `eventId` must not be blank
- `type` must not be blank
- `data` can be empty but not null
- `correlationId` preserved through handler pipeline for tracing
- `timestamp` defaults to current time if not provided

**Relationships**: None (base entity)

**State Transitions**: Immutable - events are created and passed through handlers without modification (handlers return new events)

---

### 2. HandlerContext

**Purpose**: Execution context passed to handlers containing request-scoped data

**Fields**:
- `requestId: String` (required) - Unique request identifier
- `metadata: Map<String, Any?>` (optional) - Additional context metadata
- `timestamp: Long` (auto-generated) - Context creation timestamp

**Validation Rules**:
- `requestId` must not be blank
- `metadata` can be empty but not null

**Relationships**: Passed to all handler methods (lookup, operate, write)

**State Transitions**: Immutable - context is read-only during handler execution

---

### 3. HandlerMetadata

**Purpose**: Metadata describing which events a handler receives and returns

**Fields**:
- `receives: Set<String>` (required) - Set of event types this handler can process
- `returns: Set<String>` (required) - Set of event types this handler can produce
- `description: String?` (optional) - Human-readable handler description

**Validation Rules**:
- `receives` must not be empty
- `returns` can be empty (for terminal handlers)
- Event type strings should follow naming convention (lowercase_with_underscores)

**Relationships**: Attached to handler implementations via annotations or properties

**State Transitions**: Immutable - metadata is defined at handler creation time

---

### 4. LookupData

**Purpose**: Data returned from lookup phase, passed to operate phase

**Fields**:
- `data: Map<String, Any?>` (required) - Lookup results keyed by entity name or identifier

**Validation Rules**:
- `data` can be empty (no lookup results) but not null
- SimpleHandler always returns empty map

**Relationships**: 
- Produced by: IHandler.lookup()
- Consumed by: IHandler.operate()

**State Transitions**: Created in lookup phase, consumed in operate phase, discarded after

---

## Protocol Interfaces

### IExecute

**Purpose**: Protocol for executing events through a handler pipeline

**Methods**:
- `execute(context: HandlerContext, event: BaseEvent): BaseEvent` - Execute event through handler
- `executorMeta(): HandlerMetadata` - Get metadata from wrapped handler

**Implementations**: Executor

---

### IHandler

**Purpose**: Protocol defining three-phase handler interface

**Methods**:
- `lookup(context: HandlerContext, event: BaseEvent): LookupData` - Fetch data needed for operation
- `operate(context: HandlerContext, event: BaseEvent, data: LookupData): BaseEvent` - Transform event
- `write(context: HandlerContext, event: BaseEvent): BaseEvent` - Persist changes
- `operatorMeta(): HandlerMetadata` - Get handler metadata

**Implementations**: SimpleHandler, LookupHandler, LookupWriterHandler

---

## Handler Implementations

### SimpleHandler

**Purpose**: Handler for pure transformations with no database interaction

**Behavior**:
- `lookup()` returns empty LookupData
- `operate()` invokes user-provided operator function
- `write()` returns event unchanged
- `operatorMeta()` returns metadata from operator function

**Use Cases**: Validation, enrichment, filtering, pure business logic

---

### LookupHandler

**Purpose**: Handler with database lookup before operation

**Behavior**:
- `lookup()` invokes user-provided looker-upper function
- `operate()` invokes operator with lookup data
- `write()` returns event unchanged
- `operatorMeta()` returns metadata from operator function

**Use Cases**: Duplicate detection, validation against existing data, enrichment from database

---

### LookupWriterHandler

**Purpose**: Full handler with lookup, operation, and database write

**Behavior**:
- `lookup()` invokes user-provided looker-upper function
- `operate()` invokes operator with lookup data
- `write()` invokes user-provided writer function
- `operatorMeta()` returns metadata from operator function

**Use Cases**: CRUD operations, event sourcing, state updates

---

## Entity Dispatch

### EntityUpdateDispatcher

**Purpose**: Multimethod-style dispatch for entity updates based on entity name and event type

**Dispatch Key**: `Pair<String, String>` (entityName, eventType)

**Handler Signature**: `(context: HandlerContext, event: BaseEvent, entity: Any) -> Any`

**Registration**: Handlers register for specific (entityName, eventType) combinations

**Example**:
- Key: ("party", "party_created") → Handler creates new party entity
- Key: ("party", "party_updated") → Handler updates existing party entity
- Key: ("order", "order_placed") → Handler creates new order entity

---

## Validation Summary

| Entity | Required Fields | Optional Fields | Validation Rules |
|--------|----------------|-----------------|------------------|
| BaseEvent | eventId, type, data | correlationId, subject, timestamp | eventId/type not blank |
| HandlerContext | requestId | metadata, timestamp | requestId not blank |
| HandlerMetadata | receives, returns | description | receives not empty |
| LookupData | data | - | data not null (can be empty) |

---

## Relationships Diagram

```
HandlerContext ──┐
                 ├──> IHandler.lookup() ──> LookupData
BaseEvent ───────┤                              │
                 ├──> IHandler.operate() <──────┘
                 │         │
                 │         v
                 └──> IHandler.write() ──> BaseEvent (result)

IExecute.execute() orchestrates: lookup → operate → write

EntityUpdateDispatcher: (entityName, eventType) → Handler function
```

