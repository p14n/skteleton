# Data Model: System Definition Component

**Date**: 2026-01-03  
**Feature**: 002-system-definition

## Core Entities

### SystemDefinition

**Purpose**: Immutable representation of a complete event-driven system

**Fields**:
- `handlers: Set<HandlerRegistration>` - All registered handlers in the system
- `events: Set<EventDefinition>` - All event types defined in the system
- `channels: Set<Channel>` - All communication channels/topics
- `deployments: Set<Deployment>` - Logical groupings of handlers

**Relationships**:
- Contains many HandlerRegistrations
- Contains many EventDefinitions
- Contains many Channels
- Contains many Deployments

**Validation Rules**:
- All sets must be immutable (toSet() on construction)
- Handler names must be unique within the system
- Event types must be unique within the system
- Channel names must be unique within the system

**State Transitions**: None (immutable after creation)

---

### HandlerRegistration

**Purpose**: Associates a handler instance with a name in the system

**Fields**:
- `name: String` - Unique identifier for the handler in the system
- `handler: IHandler` - The actual handler instance from event-protocol
- `metadata: HandlerMetadata` - Cached metadata from handler.operatorMeta()

**Relationships**:
- Belongs to one SystemDefinition
- References one IHandler from event-protocol component
- May belong to zero or one Deployment

**Validation Rules**:
- Name must not be empty
- Handler must not be null
- Metadata is derived from handler.operatorMeta() on construction

---

### EventDefinition

**Purpose**: Defines an event type and its routing configuration

**Fields**:
- `eventType: String` - Unique event type identifier (e.g., "user.created")
- `outputChannels: Set<String>` - Channels this event can be routed to (may be multiple)
- `producedBy: Set<String>` - Handler names that return this event
- `consumedBy: Set<String>` - Handler names that receive this event

**Relationships**:
- Belongs to one SystemDefinition
- References multiple Channels via outputChannels
- References multiple HandlerRegistrations via producedBy/consumedBy

**Validation Rules**:
- eventType must not be empty
- outputChannels may be empty (for internal-only events)
- producedBy and consumedBy are computed from handler metadata

---

### Channel

**Purpose**: Represents a communication channel/topic for event routing

**Fields**:
- `name: String` - Unique channel identifier (e.g., "user-events", "order-queue")
- `description: String?` - Optional human-readable description

**Relationships**:
- Belongs to one SystemDefinition
- Referenced by multiple EventDefinitions

**Validation Rules**:
- Name must not be empty
- Name should follow naming conventions (lowercase, hyphens)

---

### Deployment

**Purpose**: Logical grouping of handlers deployed together

**Fields**:
- `name: String` - Unique deployment identifier (e.g., "user-service", "payment-processor")
- `handlerNames: Set<String>` - Names of handlers in this deployment
- `description: String?` - Optional description

**Relationships**:
- Belongs to one SystemDefinition
- Contains multiple HandlerRegistrations via handlerNames

**Validation Rules**:
- Name must not be empty
- handlerNames must reference existing handlers in the system
- A handler may only belong to one deployment

---

### VerificationResult

**Purpose**: Contains results of system verification

**Fields**:
- `errors: List<VerificationError>` - Configuration errors that must be fixed
- `warnings: List<VerificationWarning>` - Issues that should be reviewed
- `isValid: Boolean` - True if errors list is empty

**Relationships**:
- Produced by SystemDefinition.verify()
- Contains multiple VerificationError and VerificationWarning instances

**Validation Rules**:
- isValid is computed: errors.isEmpty()

---

### VerificationError

**Purpose**: Represents a configuration error that must be fixed

**Fields**:
- `type: ErrorType` - Category of error (enum)
- `message: String` - Human-readable error description
- `handlerName: String?` - Handler involved (if applicable)
- `eventType: String?` - Event involved (if applicable)

**ErrorType Enum**:
- `UNHANDLED_EVENT` - Event returned by handler but not in system definition
- `HANDLER_EVENT_MISMATCH` - Handler registered for event it doesn't declare receiving
- `UNDECLARED_EVENT_PRODUCED` - Handler returns event not declared in system

---

### VerificationWarning

**Purpose**: Represents a configuration issue that should be reviewed

**Fields**:
- `type: WarningType` - Category of warning (enum)
- `message: String` - Human-readable warning description
- `handlerNames: List<String>` - Handlers involved (if applicable)
- `eventType: String?` - Event involved (if applicable)

**WarningType Enum**:
- `ORPHANED_EVENT` - Event declared but has no handlers
- `CIRCULAR_FLOW` - Circular event flow detected between handlers

---

## Builder Pattern

### SystemDefinition.Builder

**Purpose**: Fluent API for constructing immutable SystemDefinition instances

**Methods**:
- `addHandler(handler: IHandler, name: String): Builder` - Register a handler
- `addEvent(eventType: String, outputChannels: Set<String>): Builder` - Define an event
- `addChannel(name: String, description: String? = null): Builder` - Add a channel
- `addDeployment(name: String, handlerNames: Set<String>, description: String? = null): Builder` - Group handlers
- `build(): SystemDefinition` - Construct immutable SystemDefinition

**Validation on build()**:
- All handler names referenced in deployments must exist
- All event types referenced in handler metadata must be defined
- All channel names referenced in event definitions must exist

---

## Graph Representation (Internal)

### EventFlowGraph

**Purpose**: Internal representation for verification algorithms

**Structure**:
- Nodes: HandlerRegistrations
- Edges: Event flows (from handler A returning event X to handler B receiving event X)
- Edge labels: Event types

**Used For**:
- Circular flow detection (DFS cycle detection)
- Orphaned event detection (nodes with no incoming/outgoing edges)
- Handler reachability analysis

**Not Exposed**: Internal implementation detail, not part of public API

