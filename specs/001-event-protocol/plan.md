# Implementation Plan: Platform Event Protocol

**Branch**: `001-event-protocol` | **Date**: 2025-12-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-event-protocol/spec.md`

**Note**: This plan implements a standardized event handler protocol with three handler types (SimpleHandler, LookupHandler, LookupWriterHandler) that orchestrate event processing through lookup → operate → write phases.

## Summary

Implement a platform event protocol that provides a standardized handler pipeline for event processing. The protocol defines interfaces (IExecute, IHandler) and three concrete handler implementations that enable domain developers to process events consistently through lookup, operation, and write phases. The system includes metadata extraction for automatic event routing, base event schema validation, and polymorphic entity update dispatch.

**Technical Approach**: Kotlin protocols (interfaces) with concrete handler implementations, leveraging Kotlin's functional programming features for handler composition and metadata reflection for automatic routing discovery.

## Technical Context

**Language/Version**: Kotlin 2.0.21
**Primary Dependencies**: Kotlin stdlib 2.0.21, kotlin-reflect (for metadata extraction)
**Storage**: N/A (handlers delegate to user-provided functions)
**Testing**: JUnit5 with kotlin-test-junit5 5.10.3
**Target Platform**: JVM (cross-platform Kotlin)
**Project Type**: Component library (single module in components/ directory)
**Performance Goals**: Sub-millisecond handler dispatch overhead, zero-allocation event passing where possible
**Constraints**: Must support functional composition, metadata must be extractable via reflection
**Scale/Scope**: Foundation protocol for all domain event handlers across the system

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with Skteleton Constitution (v1.0.0):

- [x] **Component-First Architecture** (Principle I): Feature designed as standalone component in `components/event-protocol` directory with clear boundaries
- [x] **Specification-Driven Development** (Principle II): Complete specification exists at `specs/001-event-protocol/spec.md` with validated requirements
- [x] **Test-First Development** (Principle III): Test strategy defined - JUnit5 tests for each handler type, protocol contract tests, metadata extraction tests
- [x] **Independent User Stories** (Principle IV): 7 user stories with P1-P3 priorities, each independently testable (see spec.md)
- [x] **Observability and Debuggability** (Principle V): Error handling via exceptions, metadata extraction for debugging, event correlation IDs for tracing
- [x] **Semantic Versioning** (Principle VI): Initial version 1.0.0, breaking changes would be protocol signature changes (tracked in component versioning)
- [x] **Simplicity and YAGNI** (Principle VII): Three handler types cover all use cases without over-abstraction, no complexity violations

## Project Structure

### Documentation (this feature)

```text
specs/001-event-protocol/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── event-protocol.kt # Kotlin interface definitions
├── checklists/
│   └── requirements.md  # Quality validation checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
components/event-protocol/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── eventprotocol/
│   │           ├── IExecute.kt        # Execute protocol interface
│   │           ├── IHandler.kt        # Handler protocol interface
│   │           ├── Executor.kt        # Executor implementation
│   │           ├── SimpleHandler.kt   # Simple handler (no lookup/write)
│   │           ├── LookupHandler.kt   # Handler with lookup
│   │           ├── LookupWriterHandler.kt  # Full handler (lookup + write)
│   │           ├── Event.kt           # Base event schema
│   │           ├── Metadata.kt        # Metadata extraction utilities
│   │           └── EntityDispatch.kt  # Entity update multimethod
│   └── test/
│       └── kotlin/
│           └── eventprotocol/
│               ├── ExecutorTest.kt
│               ├── SimpleHandlerTest.kt
│               ├── LookupHandlerTest.kt
│               ├── LookupWriterHandlerTest.kt
│               ├── EventSchemaTest.kt
│               ├── MetadataTest.kt
│               └── EntityDispatchTest.kt
```

**Structure Decision**: Component library structure following Mill build conventions. The event-protocol component is standalone with no dependencies (except Kotlin stdlib). Tests are co-located in the test/ directory following JUnit5 conventions. This aligns with the existing `components/event-system` structure which depends on `event-protocol`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No complexity violations. All constitution principles are satisfied:
- Component is standalone and self-contained
- Three handler types provide minimal abstraction for all use cases
- No premature optimization or over-engineering detected

---

## Phase 0: Research (COMPLETE)

**Status**: ✅ All technical unknowns resolved

**Outputs**:
- [research.md](./research.md) - Technical decisions and rationale

**Key Decisions**:
1. **Schema Validation**: Kotlin data classes with init validation (no external dependencies)
2. **Metadata Extraction**: Kotlin annotations with reflection
3. **Multimethod Dispatch**: Map-based dispatch with type-safe Pair<String, String> keys

**Dependencies**: Kotlin stdlib 2.0.21, kotlin-reflect (already available)

---

## Phase 1: Design & Contracts (COMPLETE)

**Status**: ✅ All design artifacts generated

**Outputs**:
- [data-model.md](./data-model.md) - Core entities, relationships, validation rules
- [contracts/event-protocol.kt](./contracts/event-protocol.kt) - API contracts (interfaces, data classes, type aliases)
- [quickstart.md](./quickstart.md) - Developer guide with usage examples
- [.augment/rules/specify-rules.md](../../.augment/rules/specify-rules.md) - Updated agent context

**Design Summary**:
- **Core Entities**: BaseEvent, HandlerContext, LookupData, HandlerMetadata
- **Protocols**: IExecute, IHandler
- **Implementations**: Executor, SimpleHandler, LookupHandler, LookupWriterHandler
- **Utilities**: EntityUpdateDispatcher for polymorphic entity updates

**API Surface**:
- 4 data classes (BaseEvent, HandlerContext, LookupData, HandlerMetadata)
- 2 interfaces (IExecute, IHandler)
- 4 type aliases for function signatures
- 4 handler implementations

---

## Constitution Check (Post-Design)

*Re-verification after Phase 1 design completion*

- [x] **Component-First Architecture** (Principle I): ✅ Component structure defined in `components/event-protocol/` with clear module boundaries
- [x] **Specification-Driven Development** (Principle II): ✅ Design artifacts complete and aligned with spec requirements
- [x] **Test-First Development** (Principle III): ✅ Test structure defined (7 test files matching 7 user stories), ready for TDD implementation
- [x] **Independent User Stories** (Principle IV): ✅ Each user story maps to specific handler types and can be tested independently
- [x] **Observability and Debuggability** (Principle V): ✅ HandlerContext provides request tracking, correlation IDs enable tracing, metadata extraction supports debugging
- [x] **Semantic Versioning** (Principle VI): ✅ Initial version 1.0.0, breaking changes clearly defined (protocol signature changes)
- [x] **Simplicity and YAGNI** (Principle VII): ✅ Design uses standard Kotlin patterns (data classes, interfaces), no over-engineering detected

**Gate Status**: ✅ PASSED - Ready for Phase 2 (Task Breakdown)
