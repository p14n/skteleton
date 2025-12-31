# Tasks: Platform Event Protocol

**Input**: Design documents from `/specs/001-event-protocol/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Following Test-First Development (Constitution Principle III) - all tests written FIRST before implementation

**Organization**: Tasks grouped by user story to enable independent implementation and testing

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Component-based structure (Skteleton default):
- Source: `components/event-protocol/src/main/kotlin/eventprotocol/`
- Tests: `components/event-protocol/src/test/kotlin/eventprotocol/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Verify component directory structure exists at components/event-protocol/
- [x] T002 Create source directory structure: components/event-protocol/src/main/kotlin/eventprotocol/
- [x] T003 [P] Create test directory structure: components/event-protocol/src/test/kotlin/eventprotocol/
- [x] T004 [P] Verify Mill build configuration includes event-protocol component in build.mill

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data types that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 [P] Create BaseEvent data class in components/event-protocol/src/main/kotlin/eventprotocol/Event.kt
- [x] T006 [P] Create HandlerContext data class in components/event-protocol/src/main/kotlin/eventprotocol/Event.kt
- [x] T007 [P] Create LookupData data class in components/event-protocol/src/main/kotlin/eventprotocol/Event.kt
- [x] T008 [P] Create HandlerMetadata data class in components/event-protocol/src/main/kotlin/eventprotocol/Metadata.kt
- [x] T009 [P] Create IExecute interface in components/event-protocol/src/main/kotlin/eventprotocol/IExecute.kt
- [x] T010 [P] Create IHandler interface in components/event-protocol/src/main/kotlin/eventprotocol/IHandler.kt
- [x] T011 [P] Define type aliases (OperatorFunction, LookerUpperFunction, WriterFunction, EntityUpdateHandler) in components/event-protocol/src/main/kotlin/eventprotocol/Types.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 6 - Define Event Schema (Priority: P1) 🎯 MVP Foundation

**Goal**: Establish base event schema with validation so all events have consistent structure

**Independent Test**: Create BaseEvent instances and verify validation rules (eventId/type not blank, correlation ID preserved)

### Tests for User Story 6 (Test-First Development)

> **BLOCKER**: Mill/Kotlin test configuration issue - JUnit5 dependencies not on compile classpath. Tests written but disabled (.kt.disabled). Need to resolve build.mill configuration before tests can run.

- [~] T012 [P] [US6] Test BaseEvent creation with valid data in components/event-protocol/src/test/kotlin/eventprotocol/EventSchemaTest.kt.disabled (BLOCKED: test config)
- [~] T013 [P] [US6] Test BaseEvent validation rejects blank eventId in components/event-protocol/src/test/kotlin/eventprotocol/EventSchemaTest.kt.disabled (BLOCKED: test config)
- [~] T014 [P] [US6] Test BaseEvent validation rejects blank type in components/event-protocol/src/test/kotlin/eventprotocol/EventSchemaTest.kt.disabled (BLOCKED: test config)
- [~] T015 [P] [US6] Test BaseEvent preserves correlationId in components/event-protocol/src/test/kotlin/eventprotocol/EventSchemaTest.kt.disabled (BLOCKED: test config)
- [~] T016 [P] [US6] Test BaseEvent auto-generates timestamp in components/event-protocol/src/test/kotlin/eventprotocol/EventSchemaTest.kt.disabled (BLOCKED: test config)

### Implementation for User Story 6

- [x] T017 [US6] Implement BaseEvent validation in init block in components/event-protocol/src/main/kotlin/eventprotocol/Event.kt
- [ ] T018 [US6] Add BaseEvent factory methods and validation helpers in components/event-protocol/src/main/kotlin/eventprotocol/Event.kt
- [~] T019 [US6] Verify all EventSchemaTest tests pass (BLOCKED: test config)

**Checkpoint**: BaseEvent schema complete and validated - ready for handler implementation

---

## Phase 4: User Story 1 - Execute Event Through Handler Pipeline (Priority: P1) 🎯 MVP Core

**Goal**: Implement Executor that orchestrates lookup → operate → write pipeline

**Independent Test**: Create Executor with mock handler, verify execute() calls lookup/operate/write in sequence

### Tests for User Story 1 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T020 [P] [US1] Test Executor calls handler.lookup first in components/event-protocol/src/test/kotlin/eventprotocol/ExecutorTest.kt
- [ ] T021 [P] [US1] Test Executor passes lookup data to operate in components/event-protocol/src/test/kotlin/eventprotocol/ExecutorTest.kt
- [ ] T022 [P] [US1] Test Executor passes operated event to write in components/event-protocol/src/test/kotlin/eventprotocol/ExecutorTest.kt
- [ ] T023 [P] [US1] Test Executor.executorMeta returns handler metadata in components/event-protocol/src/test/kotlin/eventprotocol/ExecutorTest.kt
- [ ] T024 [P] [US1] Test Executor handles exceptions from handler phases in components/event-protocol/src/test/kotlin/eventprotocol/ExecutorTest.kt

### Implementation for User Story 1

- [ ] T025 [US1] Implement Executor class implementing IExecute in components/event-protocol/src/main/kotlin/eventprotocol/Executor.kt
- [ ] T026 [US1] Implement Executor.execute() orchestrating lookup → operate → write in components/event-protocol/src/main/kotlin/eventprotocol/Executor.kt
- [ ] T027 [US1] Implement Executor.executorMeta() delegating to handler in components/event-protocol/src/main/kotlin/eventprotocol/Executor.kt
- [ ] T028 [US1] Add error handling and logging to Executor in components/event-protocol/src/main/kotlin/eventprotocol/Executor.kt
- [ ] T029 [US1] Verify all ExecutorTest tests pass

**Checkpoint**: Executor complete - can orchestrate any IHandler implementation

---

## Phase 5: User Story 4 - Full Event Processing with Lookup and Persistence (Priority: P1) 🎯 MVP Complete

**Goal**: Implement LookupWriterHandler for complete CRUD operations (most common use case)

**Independent Test**: Create LookupWriterHandler with all three functions, verify complete pipeline execution

### Tests for User Story 4 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T030 [P] [US4] Test LookupWriterHandler.lookup invokes looker-upper function in components/event-protocol/src/test/kotlin/eventprotocol/LookupWriterHandlerTest.kt
- [ ] T031 [P] [US4] Test LookupWriterHandler.operate receives lookup data in components/event-protocol/src/test/kotlin/eventprotocol/LookupWriterHandlerTest.kt
- [ ] T032 [P] [US4] Test LookupWriterHandler.write invokes writer function in components/event-protocol/src/test/kotlin/eventprotocol/LookupWriterHandlerTest.kt
- [ ] T033 [P] [US4] Test LookupWriterHandler.operatorMeta returns metadata in components/event-protocol/src/test/kotlin/eventprotocol/LookupWriterHandlerTest.kt
- [ ] T034 [P] [US4] Test full pipeline: lookup → operate → write integration in components/event-protocol/src/test/kotlin/eventprotocol/LookupWriterHandlerTest.kt

### Implementation for User Story 4

- [ ] T035 [US4] Implement LookupWriterHandler class implementing IHandler in components/event-protocol/src/main/kotlin/eventprotocol/LookupWriterHandler.kt
- [ ] T036 [US4] Implement LookupWriterHandler.lookup() calling looker-upper in components/event-protocol/src/main/kotlin/eventprotocol/LookupWriterHandler.kt
- [ ] T037 [US4] Implement LookupWriterHandler.operate() calling operator with lookup data in components/event-protocol/src/main/kotlin/eventprotocol/LookupWriterHandler.kt
- [ ] T038 [US4] Implement LookupWriterHandler.write() calling writer function in components/event-protocol/src/main/kotlin/eventprotocol/LookupWriterHandler.kt
- [ ] T039 [US4] Implement LookupWriterHandler.operatorMeta() returning metadata in components/event-protocol/src/main/kotlin/eventprotocol/LookupWriterHandler.kt
- [ ] T040 [US4] Verify all LookupWriterHandlerTest tests pass

**Checkpoint**: MVP COMPLETE - Can execute full CRUD event handlers through Executor

---

## Phase 6: User Story 2 - Simple Event Processing (Priority: P2)

**Goal**: Implement SimpleHandler for pure transformations without database interaction

**Independent Test**: Create SimpleHandler with operator function, verify lookup returns empty and write passes through

### Tests for User Story 2 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T041 [P] [US2] Test SimpleHandler.lookup returns empty LookupData in components/event-protocol/src/test/kotlin/eventprotocol/SimpleHandlerTest.kt
- [ ] T042 [P] [US2] Test SimpleHandler.operate invokes operator function in components/event-protocol/src/test/kotlin/eventprotocol/SimpleHandlerTest.kt
- [ ] T043 [P] [US2] Test SimpleHandler.write returns event unchanged in components/event-protocol/src/test/kotlin/eventprotocol/SimpleHandlerTest.kt
- [ ] T044 [P] [US2] Test SimpleHandler.operatorMeta returns metadata in components/event-protocol/src/test/kotlin/eventprotocol/SimpleHandlerTest.kt

### Implementation for User Story 2

- [ ] T045 [US2] Implement SimpleHandler class implementing IHandler in components/event-protocol/src/main/kotlin/eventprotocol/SimpleHandler.kt
- [ ] T046 [US2] Implement SimpleHandler.lookup() returning empty LookupData in components/event-protocol/src/main/kotlin/eventprotocol/SimpleHandler.kt
- [ ] T047 [US2] Implement SimpleHandler.operate() calling operator function in components/event-protocol/src/main/kotlin/eventprotocol/SimpleHandler.kt
- [ ] T048 [US2] Implement SimpleHandler.write() returning event unchanged in components/event-protocol/src/main/kotlin/eventprotocol/SimpleHandler.kt
- [ ] T049 [US2] Implement SimpleHandler.operatorMeta() returning metadata in components/event-protocol/src/main/kotlin/eventprotocol/SimpleHandler.kt
- [ ] T050 [US2] Verify all SimpleHandlerTest tests pass

**Checkpoint**: SimpleHandler complete - can handle pure transformations

---

## Phase 7: User Story 3 - Event Processing with Database Lookup (Priority: P2)

**Goal**: Implement LookupHandler for operations requiring database lookups before processing

**Independent Test**: Create LookupHandler with looker-upper and operator, verify lookup data flows to operate

### Tests for User Story 3 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T051 [P] [US3] Test LookupHandler.lookup invokes looker-upper function in components/event-protocol/src/test/kotlin/eventprotocol/LookupHandlerTest.kt
- [ ] T052 [P] [US3] Test LookupHandler.operate receives lookup data in components/event-protocol/src/test/kotlin/eventprotocol/LookupHandlerTest.kt
- [ ] T053 [P] [US3] Test LookupHandler.write returns event unchanged in components/event-protocol/src/test/kotlin/eventprotocol/LookupHandlerTest.kt
- [ ] T054 [P] [US3] Test LookupHandler.operatorMeta returns metadata in components/event-protocol/src/test/kotlin/eventprotocol/LookupHandlerTest.kt

### Implementation for User Story 3

- [ ] T055 [US3] Implement LookupHandler class implementing IHandler in components/event-protocol/src/main/kotlin/eventprotocol/LookupHandler.kt
- [ ] T056 [US3] Implement LookupHandler.lookup() calling looker-upper in components/event-protocol/src/main/kotlin/eventprotocol/LookupHandler.kt
- [ ] T057 [US3] Implement LookupHandler.operate() calling operator with lookup data in components/event-protocol/src/main/kotlin/eventprotocol/LookupHandler.kt
- [ ] T058 [US3] Implement LookupHandler.write() returning event unchanged in components/event-protocol/src/main/kotlin/eventprotocol/LookupHandler.kt
- [ ] T059 [US3] Implement LookupHandler.operatorMeta() returning metadata in components/event-protocol/src/main/kotlin/eventprotocol/LookupHandler.kt
- [ ] T060 [US3] Verify all LookupHandlerTest tests pass

**Checkpoint**: All three handler types complete - full handler palette available

---

## Phase 8: User Story 7 - Entity Update Dispatch (Priority: P2)

**Goal**: Implement multimethod-style dispatch for polymorphic entity updates

**Independent Test**: Register handlers for different (entityName, eventType) combinations, verify correct dispatch

### Tests for User Story 7 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T061 [P] [US7] Test EntityDispatch.register stores handler for entity/event pair in components/event-protocol/src/test/kotlin/eventprotocol/EntityDispatchTest.kt
- [ ] T062 [P] [US7] Test EntityDispatch.dispatch invokes correct handler in components/event-protocol/src/test/kotlin/eventprotocol/EntityDispatchTest.kt
- [ ] T063 [P] [US7] Test EntityDispatch.dispatch throws on unknown entity/event pair in components/event-protocol/src/test/kotlin/eventprotocol/EntityDispatchTest.kt
- [ ] T064 [P] [US7] Test EntityDispatch handles multiple entity types in components/event-protocol/src/test/kotlin/eventprotocol/EntityDispatchTest.kt

### Implementation for User Story 7

- [ ] T065 [US7] Implement EntityDispatch object with handler registry in components/event-protocol/src/main/kotlin/eventprotocol/EntityDispatch.kt
- [ ] T066 [US7] Implement EntityDispatch.register() for handler registration in components/event-protocol/src/main/kotlin/eventprotocol/EntityDispatch.kt
- [ ] T067 [US7] Implement EntityDispatch.dispatch() with map-based lookup in components/event-protocol/src/main/kotlin/eventprotocol/EntityDispatch.kt
- [ ] T068 [US7] Add error handling for missing handlers in components/event-protocol/src/main/kotlin/eventprotocol/EntityDispatch.kt
- [ ] T069 [US7] Verify all EntityDispatchTest tests pass

**Checkpoint**: Entity dispatch complete - polymorphic updates enabled

---

## Phase 9: User Story 5 - Extract Handler Metadata (Priority: P3)

**Goal**: Implement metadata extraction for automatic routing table generation

**Independent Test**: Call metadata extraction on handlers/executors, verify correct metadata returned

### Tests for User Story 5 (Test-First Development)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T070 [P] [US5] Test extracting metadata from IHandler implementations in components/event-protocol/src/test/kotlin/eventprotocol/MetadataTest.kt
- [ ] T071 [P] [US5] Test extracting metadata from IExecute implementations in components/event-protocol/src/test/kotlin/eventprotocol/MetadataTest.kt
- [ ] T072 [P] [US5] Test metadata extraction handles missing metadata gracefully in components/event-protocol/src/test/kotlin/eventprotocol/MetadataTest.kt

### Implementation for User Story 5

- [ ] T073 [US5] Implement metadata extraction utilities in components/event-protocol/src/main/kotlin/eventprotocol/Metadata.kt
- [ ] T074 [US5] Add reflection-based metadata access in components/event-protocol/src/main/kotlin/eventprotocol/Metadata.kt
- [ ] T075 [US5] Implement routing table builder from handler metadata in components/event-protocol/src/main/kotlin/eventprotocol/Metadata.kt
- [ ] T076 [US5] Verify all MetadataTest tests pass

**Checkpoint**: All user stories complete - full event protocol implemented

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T077 [P] Add comprehensive KDoc documentation to all public APIs in components/event-protocol/src/main/kotlin/eventprotocol/
- [ ] T078 [P] Add edge case tests for null/exception handling across all handlers
- [ ] T079 [P] Verify quickstart.md examples work with implemented code
- [ ] T080 Run full test suite: mill components.event-protocol.test
- [ ] T081 Code review and refactoring for consistency
- [ ] T082 Performance validation: verify sub-millisecond handler dispatch overhead
- [ ] T083 Update README or component documentation with usage examples

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 6 (Phase 3)**: Depends on Foundational - BaseEvent schema foundation
- **User Story 1 (Phase 4)**: Depends on Foundational + US6 - Executor needs BaseEvent
- **User Story 4 (Phase 5)**: Depends on Foundational + US6 - LookupWriterHandler needs BaseEvent
- **User Story 2 (Phase 6)**: Depends on Foundational + US6 - SimpleHandler needs BaseEvent
- **User Story 3 (Phase 7)**: Depends on Foundational + US6 - LookupHandler needs BaseEvent
- **User Story 7 (Phase 8)**: Depends on Foundational + US6 - EntityDispatch needs BaseEvent
- **User Story 5 (Phase 9)**: Depends on all handler implementations - extracts metadata from them
- **Polish (Phase 10)**: Depends on all user stories being complete

### User Story Dependencies

**Critical Path (MVP)**:
1. **Foundational (Phase 2)** → **US6 (Phase 3)** → **US1 (Phase 4)** → **US4 (Phase 5)** = MVP Complete

**Independent Stories** (can run in parallel after US6):
- **User Story 2 (P2)**: Independent - can start after US6
- **User Story 3 (P2)**: Independent - can start after US6
- **User Story 4 (P1)**: Independent - can start after US6
- **User Story 7 (P2)**: Independent - can start after US6

**Dependent Story**:
- **User Story 5 (P3)**: Depends on US1, US2, US3, US4 (needs handlers to extract metadata from)

### Within Each User Story

**Test-First Development (TDD) Flow**:
1. Write all tests for the story (marked [P] can run in parallel)
2. Verify tests FAIL (red phase)
3. Implement functionality to make tests pass (green phase)
4. Refactor and verify tests still pass
5. Move to next story

**Task Order Within Story**:
- Tests FIRST (all tests can be written in parallel)
- Core implementation
- Integration/error handling
- Verify all tests pass

### Parallel Opportunities

**Setup Phase (Phase 1)**:
- T002, T003, T004 can all run in parallel

**Foundational Phase (Phase 2)**:
- T005, T006, T007, T008, T009, T010, T011 can all run in parallel (different files)

**After US6 Complete**:
- US1, US2, US3, US4, US7 can all start in parallel (different handler files)
- Each story's tests can be written in parallel

**Within Each Story**:
- All test tasks marked [P] can run in parallel
- Implementation tasks are sequential (build on each other)

---

## Parallel Example: User Story 4 (LookupWriterHandler)

```bash
# Write all tests in parallel (red phase):
T030: "Test LookupWriterHandler.lookup invokes looker-upper function"
T031: "Test LookupWriterHandler.operate receives lookup data"
T032: "Test LookupWriterHandler.write invokes writer function"
T033: "Test LookupWriterHandler.operatorMeta returns metadata"
T034: "Test full pipeline: lookup → operate → write integration"

# Then implement sequentially (green phase):
T035: "Implement LookupWriterHandler class"
T036: "Implement lookup() method"
T037: "Implement operate() method"
T038: "Implement write() method"
T039: "Implement operatorMeta() method"
T040: "Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (Minimum Viable Product)

**Goal**: Get core event processing working with full CRUD handlers

**Phases**:
1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T011) - CRITICAL GATE
3. Complete Phase 3: User Story 6 - BaseEvent Schema (T012-T019)
4. Complete Phase 4: User Story 1 - Executor (T020-T029)
5. Complete Phase 5: User Story 4 - LookupWriterHandler (T030-T040)
6. **STOP and VALIDATE**: Test complete event processing pipeline
7. Deploy/demo if ready

**MVP Deliverable**: Can execute events through Executor with LookupWriterHandler performing full CRUD operations

**Task Count**: 40 tasks (T001-T040)

### Incremental Delivery

**Iteration 1 - MVP** (P1 stories):
- Setup + Foundational + US6 + US1 + US4 → Full CRUD event processing ✅

**Iteration 2 - Handler Variety** (P2 stories):
- Add US2 (SimpleHandler) → Pure transformations ✅
- Add US3 (LookupHandler) → Read-only operations ✅
- Add US7 (EntityDispatch) → Polymorphic updates ✅

**Iteration 3 - Advanced Features** (P3 stories):
- Add US5 (Metadata) → Automatic routing ✅

**Iteration 4 - Polish**:
- Phase 10: Documentation, performance, edge cases ✅

Each iteration adds value without breaking previous functionality.

### Parallel Team Strategy

**With 3 developers after Foundational phase**:

**Week 1**:
- Dev A: US6 (BaseEvent) → US1 (Executor)
- Dev B: US6 complete → US4 (LookupWriterHandler)
- Dev C: US6 complete → US2 (SimpleHandler)

**Week 2**:
- Dev A: US3 (LookupHandler)
- Dev B: US7 (EntityDispatch)
- Dev C: US5 (Metadata)

**Week 3**:
- All: Phase 10 (Polish) together

---

## Task Summary

**Total Tasks**: 83

**By Phase**:
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 7 tasks
- Phase 3 (US6 - BaseEvent): 8 tasks
- Phase 4 (US1 - Executor): 10 tasks
- Phase 5 (US4 - LookupWriterHandler): 11 tasks
- Phase 6 (US2 - SimpleHandler): 10 tasks
- Phase 7 (US3 - LookupHandler): 10 tasks
- Phase 8 (US7 - EntityDispatch): 9 tasks
- Phase 9 (US5 - Metadata): 7 tasks
- Phase 10 (Polish): 7 tasks

**By User Story**:
- US1 (Executor): 10 tasks
- US2 (SimpleHandler): 10 tasks
- US3 (LookupHandler): 10 tasks
- US4 (LookupWriterHandler): 11 tasks
- US5 (Metadata): 7 tasks
- US6 (BaseEvent): 8 tasks
- US7 (EntityDispatch): 9 tasks
- Setup/Foundational: 11 tasks
- Polish: 7 tasks

**Parallel Opportunities**: 45 tasks marked [P] can run in parallel within their phase

**MVP Scope**: 40 tasks (T001-T040) = Setup + Foundational + US6 + US1 + US4

**Independent Test Criteria**:
- US6: Create and validate BaseEvent instances
- US1: Execute events through Executor with mock handler
- US4: Full CRUD pipeline with LookupWriterHandler
- US2: Pure transformations with SimpleHandler
- US3: Read operations with LookupHandler
- US7: Polymorphic dispatch with EntityDispatch
- US5: Metadata extraction from all handler types

---

## Notes

- **[P] tasks**: Different files, no dependencies - can run in parallel
- **[Story] label**: Maps task to specific user story for traceability
- **Test-First**: All tests written FIRST, must FAIL before implementation (Constitution Principle III)
- **Independent Stories**: Each user story is independently completable and testable (Constitution Principle IV)
- **Commit Strategy**: Commit after each test passes or logical group
- **Checkpoints**: Stop at any checkpoint to validate story independently
- **Avoid**: Vague tasks, same file conflicts, cross-story dependencies that break independence

