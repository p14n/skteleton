# Tasks: System Definition Component

**Input**: Design documents from `/specs/002-system-definition/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Following TDD approach per Constitution Principle III - tests written FIRST before implementation

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Component-based structure (Skteleton default): `components/system-definition/src/main/kotlin/systemdefinition/` and `components/system-definition/src/test/kotlin/systemdefinition/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create component directory structure at components/system-definition/
- [ ] T002 Create build.gradle.kts in components/system-definition/ with Kotlin 2.0.21 and event-protocol dependency
- [ ] T003 Add system-definition to settings.gradle.kts includes
- [ ] T004 [P] Create package structure in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T005 [P] Create test package structure in components/system-definition/src/test/kotlin/systemdefinition/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data classes that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 [P] Create Channel.kt data class in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T007 [P] Create HandlerRegistration.kt data class in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T008 [P] Create EventDefinition.kt data class in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T009 [P] Create Deployment.kt data class in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T010 Create SystemDefinition.kt data class with Builder in components/system-definition/src/main/kotlin/systemdefinition/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Define Event-Driven System Structure (Priority: P1) 🎯 MVP

**Goal**: Enable developers to define event-driven systems using type-safe Kotlin DSL and query the system structure programmatically

**Independent Test**: Create a system definition with handlers and events, verify structure can be queried for handlers by event, events by handler, channels, and event routing

### Tests for User Story 1 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US1] Write test for system definition creation in components/system-definition/src/test/kotlin/systemdefinition/SystemDefinitionTest.kt
- [ ] T012 [P] [US1] Write test for querying handlers by event in components/system-definition/src/test/kotlin/systemdefinition/SystemDefinitionTest.kt
- [ ] T013 [P] [US1] Write test for querying events by handler in components/system-definition/src/test/kotlin/systemdefinition/SystemDefinitionTest.kt
- [ ] T014 [P] [US1] Write test for querying all channels in components/system-definition/src/test/kotlin/systemdefinition/SystemDefinitionTest.kt
- [ ] T015 [P] [US1] Write test for querying event routing to channels in components/system-definition/src/test/kotlin/systemdefinition/SystemDefinitionTest.kt

### Implementation for User Story 1

- [ ] T016 [US1] Implement SystemDefinition.Builder.addHandler() method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T017 [US1] Implement SystemDefinition.Builder.addEvent() method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T018 [US1] Implement SystemDefinition.Builder.addChannel() method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T019 [US1] Implement SystemDefinition.Builder.addDeployment() method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T020 [US1] Implement SystemDefinition.Builder.build() with validation in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T021 [US1] Implement SystemDefinition.getProducersOf(eventType) query method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T022 [US1] Implement SystemDefinition.getConsumersOf(eventType) query method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T023 [US1] Implement SystemDefinition.getEventsForChannel(channelName) query method in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T024 [US1] Run tests to verify all User Story 1 acceptance scenarios pass

**Checkpoint**: At this point, User Story 1 should be fully functional - system definitions can be created and queried

---

## Phase 4: User Story 2 - Verify System Configuration (Priority: P2)

**Goal**: Automatically verify system definitions to detect misconfigurations and provide comprehensive error/warning reports

**Independent Test**: Create intentionally misconfigured systems and verify that validation catches all error types (unhandled events, handler mismatches, orphaned events)

### Tests for User Story 2 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T025 [P] [US2] Write test for detecting unhandled events in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt
- [ ] T026 [P] [US2] Write test for detecting handler event mismatches in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt
- [ ] T027 [P] [US2] Write test for detecting orphaned events (warning) in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt
- [ ] T028 [P] [US2] Write test for detecting circular flows (warning) in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt
- [ ] T029 [P] [US2] Write test for valid system returning no errors in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt
- [ ] T030 [P] [US2] Write test for comprehensive error list with multiple issues in components/system-definition/src/test/kotlin/systemdefinition/SystemVerifierTest.kt

### Implementation for User Story 2

- [ ] T031 [P] [US2] Create VerificationResult.kt data class in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T032 [P] [US2] Create VerificationError.kt with ErrorType enum in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T033 [P] [US2] Create VerificationWarning.kt with WarningType enum in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T034 [P] [US2] Create EventFlowGraph.kt for internal graph representation in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T035 [US2] Create SystemVerifier.kt with verify() method in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T036 [US2] Implement unhandled event detection in SystemVerifier.kt
- [ ] T037 [US2] Implement handler event mismatch detection in SystemVerifier.kt
- [ ] T038 [US2] Implement orphaned event detection (warning) in SystemVerifier.kt
- [ ] T039 [US2] Implement circular flow detection using DFS in SystemVerifier.kt
- [ ] T040 [US2] Add SystemDefinition.verify() method that delegates to SystemVerifier in components/system-definition/src/main/kotlin/systemdefinition/SystemDefinition.kt
- [ ] T041 [US2] Run tests to verify all User Story 2 acceptance scenarios pass

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - systems can be defined, queried, and verified

---

## Phase 5: User Story 3 - Generate D2 Diagrams (Priority: P3)

**Goal**: Automatically generate D2 diagram syntax from system definitions for visualization and documentation

**Independent Test**: Create a system definition and verify the generated D2 output correctly represents all handlers, events, channels, and deployments

### Tests for User Story 3 (TDD - Write FIRST)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T042 [P] [US3] Write test for D2 output includes all handler nodes in components/system-definition/src/test/kotlin/systemdefinition/D2GeneratorTest.kt
- [ ] T043 [P] [US3] Write test for D2 output includes event flow edges in components/system-definition/src/test/kotlin/systemdefinition/D2GeneratorTest.kt
- [ ] T044 [P] [US3] Write test for D2 output groups handlers by channel in components/system-definition/src/test/kotlin/systemdefinition/D2GeneratorTest.kt
- [ ] T045 [P] [US3] Write test for D2 output groups handlers by deployment in components/system-definition/src/test/kotlin/systemdefinition/D2GeneratorTest.kt
- [ ] T046 [P] [US3] Write test for generated D2 syntax is valid and renderable in components/system-definition/src/test/kotlin/systemdefinition/D2GeneratorTest.kt

### Implementation for User Story 3

- [ ] T047 [US3] Create D2Generator.kt with toD2() method in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T048 [US3] Implement handler node generation in D2Generator.kt
- [ ] T049 [US3] Implement event flow edge generation in D2Generator.kt
- [ ] T050 [US3] Implement channel grouping in D2Generator.kt
- [ ] T051 [US3] Implement deployment container generation in D2Generator.kt
- [ ] T052 [US3] Add SystemDefinition.toD2() extension method in components/system-definition/src/main/kotlin/systemdefinition/D2Generator.kt
- [ ] T053 [US3] Run tests to verify all User Story 3 acceptance scenarios pass

**Checkpoint**: All user stories should now be independently functional - systems can be defined, queried, verified, and visualized

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T054 [P] Add comprehensive KDoc documentation to all public APIs in components/system-definition/src/main/kotlin/systemdefinition/
- [ ] T055 [P] Add validation error messages with clear guidance in VerificationError.kt and VerificationWarning.kt
- [ ] T056 [P] Add example usage in quickstart.md validation
- [ ] T057 Code cleanup and refactoring for consistency
- [ ] T058 Performance optimization for large systems (100+ handlers)
- [ ] T059 [P] Add edge case tests for empty systems, single handler, etc.
- [ ] T060 Final integration test covering all three user stories together

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (uses same data model)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2 (uses same data model)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Data classes before services
- Core implementation before query methods
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Data classes within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Write test for system definition creation in SystemDefinitionTest.kt"
Task: "Write test for querying handlers by event in SystemDefinitionTest.kt"
Task: "Write test for querying events by handler in SystemDefinitionTest.kt"
Task: "Write test for querying all channels in SystemDefinitionTest.kt"
Task: "Write test for querying event routing to channels in SystemDefinitionTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Total tasks: 60 (5 setup, 5 foundational, 14 US1, 17 US2, 12 US3, 7 polish)

