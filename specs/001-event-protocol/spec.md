# Feature Specification: Platform Event Protocol

**Feature Branch**: `001-event-protocol`
**Created**: 2025-12-22
**Status**: Migrated from existing implementation
**Input**: Reverse-engineered from existing implementation in another project

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Execute Event Through Handler Pipeline (Priority: P1)

As a domain developer, I need to execute events through a standardized handler pipeline so that I can process events consistently with lookup, operation, and write phases.

**Why this priority**: This is the core functionality - the Executor orchestrates the entire event processing flow by coordinating lookup → operate → write phases.

**Independent Test**: Can be fully tested by creating an Executor with a handler and verifying that execute() calls lookup, operate, and write in sequence, passing data through the pipeline.

**Acceptance Scenarios**:

1. **Given** an Executor wrapping a handler, **When** execute is called with context and event, **Then** the handler's lookup is called first, operate receives the lookup data, and write receives the operated event
2. **Given** an Executor with a handler, **When** executor-meta is called, **Then** it returns the operator metadata from the wrapped handler

---

### User Story 2 - Simple Event Processing (Priority: P2)

As a domain developer, I need a simple handler for events that don't require database lookups or writes so that I can implement pure transformation logic without boilerplate.

**Why this priority**: Provides the simplest handler type for pure event transformations, commonly used for validation or simple business logic.

**Independent Test**: Can be tested by creating a SimpleHandler with an operator function and verifying that operate processes the event while lookup returns empty map and write passes through unchanged.

**Acceptance Scenarios**:

1. **Given** a SimpleHandler with an operator function, **When** lookup is called, **Then** it returns an empty map
2. **Given** a SimpleHandler with an operator function, **When** operate is called, **Then** the operator function is invoked with context, event, and data
3. **Given** a SimpleHandler, **When** write is called, **Then** it returns the event unchanged
4. **Given** a SimpleHandler, **When** operator-meta is called, **Then** it returns the metadata of the operator function

---

### User Story 3 - Event Processing with Database Lookup (Priority: P2)

As a domain developer, I need a handler that performs database lookups before processing so that I can make decisions based on existing data (e.g., check if entity exists).

**Why this priority**: Many operations require checking existing state before processing - duplicate detection, validation against current data, etc.

**Independent Test**: Can be tested by creating a LookupHandler with looker-upper and operator functions, then verifying lookup calls the looker-upper and operate uses the returned data.

**Acceptance Scenarios**:

1. **Given** a LookupHandler with a looker-upper function, **When** lookup is called, **Then** the looker-upper function is invoked with context and event
2. **Given** a LookupHandler, **When** operate is called with lookup data, **Then** the operator receives the data from lookup
3. **Given** a LookupHandler, **When** write is called, **Then** it returns the event unchanged

---

### User Story 4 - Full Event Processing with Lookup and Persistence (Priority: P1)

As a domain developer, I need a handler that performs lookups, operations, and database writes so that I can implement complete CRUD operations with a single handler.

**Why this priority**: This is the most commonly used handler pattern for domain operations that need to read, process, and persist data.

**Independent Test**: Can be tested by creating a LookupWriterHandler and verifying the complete flow: lookup fetches data, operate transforms the event, and write persists to database.

**Acceptance Scenarios**:

1. **Given** a LookupWriterHandler, **When** the full pipeline executes, **Then** lookup fetches existing data, operate processes with that data, and write persists the result
2. **Given** a LookupWriterHandler with a writer function, **When** write is called, **Then** the writer function is invoked with context and event

---

### User Story 5 - Extract Handler Metadata (Priority: P3)

As a system developer, I need to extract metadata from handlers and executors so that I can build routing tables and documentation from handler definitions.

**Why this priority**: Enables the event system to automatically discover which events a handler receives and returns, supporting dynamic routing.

**Independent Test**: Can be tested by calling var->meta on different handler types and verifying correct metadata extraction.

**Acceptance Scenarios**:

1. **Given** a function var with metadata, **When** var->meta is called, **Then** it returns the function's metadata
2. **Given** an IHandler implementation, **When** var->meta is called, **Then** it returns the operator-meta
3. **Given** an IExecute implementation, **When** var->meta is called, **Then** it returns the executor-meta

---

### User Story 6 - Define Event Schema (Priority: P1)

As a domain developer, I need a base event schema so that all events have consistent structure with event-id, type, correlation-id, subject, and data.

**Why this priority**: Foundation for all event definitions - ensures consistency and enables correlation tracking across the system.

**Independent Test**: Can be tested by validating events against the BaseEvent Malli schema.

**Acceptance Scenarios**:

1. **Given** an event with event-id, type, and data, **When** validated against BaseEvent, **Then** validation passes
2. **Given** an event with correlation-id, **When** processed through handlers, **Then** correlation-id is preserved for tracing

---

### User Story 7 - Entity Update Dispatch (Priority: P2)

As a domain developer, I need a multimethod to dispatch entity updates based on entity name and event type so that I can define how different events modify different entity types.

**Why this priority**: Enables polymorphic entity updates - the same event system can update different entity types based on event type.

**Independent Test**: Can be tested by defining update-entity methods for specific entity/event-type combinations and verifying correct dispatch.

**Acceptance Scenarios**:

1. **Given** an update-entity method defined for ["party" :party_created], **When** called with those parameters, **Then** the correct method implementation is invoked
2. **Given** multiple entity types, **When** update-entity is called, **Then** dispatch occurs based on [entity-name event-type] tuple

### Edge Cases

- What happens when an operator function throws an exception during execute?
- How does the system handle nil returns from lookup functions?
- What happens when write fails after operate has already transformed the event?
- How are handlers with missing metadata handled by var->meta?
- What happens when execute is called with nil context or nil event?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide IExecute protocol with execute and executor-meta methods
- **FR-002**: System MUST provide IHandler protocol with lookup, operate, write, and operator-meta methods
- **FR-003**: Executor MUST orchestrate handler execution in lookup → operate → write sequence
- **FR-004**: SimpleHandler MUST return empty map from lookup and pass-through from write
- **FR-005**: LookupHandler MUST invoke looker-upper function during lookup phase
- **FR-006**: LookupWriterHandler MUST invoke writer function during write phase
- **FR-007**: All handlers MUST expose operator metadata via operator-meta method
- **FR-008**: var->meta MUST extract metadata from vars, IHandler, and IExecute implementations
- **FR-009**: BaseEvent schema MUST define event-id (required), type (required), correlation-id (optional), subject (optional), and data (required)
- **FR-010**: update-entity multimethod MUST dispatch on [entity-name event-type] tuple

### Key Entities

- **IExecute**: Protocol defining the execute interface for running events through a handler pipeline
- **IHandler**: Protocol defining the three-phase handler interface (lookup, operate, write)
- **Executor**: Concrete implementation that wraps an IHandler and orchestrates the execution pipeline
- **SimpleHandler**: Handler for pure transformations with no database interaction
- **LookupHandler**: Handler with database lookup before operation
- **LookupWriterHandler**: Full handler with lookup, operation, and database write phases
- **BaseEvent**: Malli schema defining the structure all events must conform to

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All domain handlers can be composed using one of the three handler types (SimpleHandler, LookupHandler, LookupWriterHandler)
- **SC-002**: Event execution follows consistent lookup → operate → write pattern across all handlers
- **SC-003**: Handler metadata (receives/returns sets) is extractable for automatic event routing
- **SC-004**: BaseEvent schema validation catches malformed events before processing
- **SC-005**: Correlation IDs are preserved through the entire event processing pipeline
- **SC-006**: Handler implementations are testable in isolation without the full event system
