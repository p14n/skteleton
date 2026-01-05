# Tasks: Event System Runtime

**Input**: Design documents from `/specs/003-event-system-runtime/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: This feature follows Test-First Development (TDD) - tests are written BEFORE implementation per Constitution Principle III.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Component-based**: `components/event-system-runtime/src/main/kotlin/eventsystemruntime/`, `components/event-system-runtime/src/test/kotlin/eventsystemruntime/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create component directory structure at components/event-system-runtime/
- [x] T002 Create build.gradle.kts with Kotlin 2.0.21, JVM target 21, and dependencies (Vert.x 5.0.6, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.3, postevent-vertx 0.1.0, JUnit5)
- [x] T003 [P] Create package structure: eventsystemruntime/ and eventsystemruntime/config/
- [x] T004 [P] Configure kotlinx.serialization plugin in build.gradle.kts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 [P] Create RuntimeConfig.kt data class in components/event-system-runtime/src/main/kotlin/eventsystemruntime/config/RuntimeConfig.kt
- [x] T006 [P] Create ValidationRules.kt for SystemDefinition validation in components/event-system-runtime/src/main/kotlin/eventsystemruntime/config/ValidationRules.kt
- [x] T007 [P] Create HandlerAdapter.kt interface for handler type adaptation in components/event-system-runtime/src/main/kotlin/eventsystemruntime/HandlerAdapter.kt
- [x] T008 [P] Create EventRouter.kt for event routing logic in components/event-system-runtime/src/main/kotlin/eventsystemruntime/EventRouter.kt
- [x] T009 [P] Create CorrelationIdManager.kt for correlation ID tracking in components/event-system-runtime/src/main/kotlin/eventsystemruntime/CorrelationIdManager.kt
- [x] T010 Create EventSystemRuntime.kt main class skeleton in components/event-system-runtime/src/main/kotlin/eventsystemruntime/EventSystemRuntime.kt

**Checkpoint**: ✅ Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Initialize Event System from System Definition (Priority: P1) 🎯 MVP

**Goal**: Initialize event system runtime from SystemDefinition, subscribing handlers to appropriate channels

**Independent Test**: Provide SystemDefinition with handlers, verify subscriptions created on Vert.x EventBus and postevent system

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T011 [P] [US1] Create EventSystemRuntimeTest.kt with test for transient channel subscription in components/event-system-runtime/src/test/kotlin/eventsystemruntime/EventSystemRuntimeTest.kt
- [x] T012 [P] [US1] Add test for persistent channel subscription in EventSystemRuntimeTest.kt
- [x] T013 [P] [US1] Add test for SystemDefinition validation failure in EventSystemRuntimeTest.kt
- [x] T014 [P] [US1] Add test for missing datasource configuration error in EventSystemRuntimeTest.kt

### Implementation for User Story 1

- [x] T015 [P] [US1] Implement ValidationRules.validate() method for SystemDefinition validation (FR-002, FR-003) in components/event-system-runtime/src/main/kotlin/eventsystemruntime/config/ValidationRules.kt
- [x] T016 [P] [US1] Create ChannelSubscriber.kt for channel subscription management in components/event-system-runtime/src/main/kotlin/eventsystemruntime/ChannelSubscriber.kt
- [x] T017 [US1] Implement initialize() function in EventSystemRuntime.kt (FR-001) in components/event-system-runtime/src/main/kotlin/eventsystemruntime/EventSystemRuntime.kt
- [x] T018 [US1] Implement transient channel subscription logic using Vert.x EventBus (FR-004) in EventSystemRuntime.kt
- [x] T019 [US1] Implement persistent channel subscription logic using postevent system (FR-005) in EventSystemRuntime.kt
- [x] T020 [US1] Add validation error handling and exception throwing in initialize() function
- [x] T021 [US1] Add logging for initialization events (channel subscriptions, validation errors)

**Checkpoint**: ✅ User Story 1 complete - initialization fully functional and testable independently

---

## Phase 4: User Story 2 - Route Events Through Transient Channels (Priority: P1)

**Goal**: Route events published to transient channels to subscribed handlers via Vert.x EventBus

**Independent Test**: Publish event to transient channel, verify all subscribed handlers receive and process it

### Tests for User Story 2

- [x] T022 [P] [US2] Create TransientRoutingTest.kt with test for single handler routing in components/event-system-runtime/src/test/kotlin/eventsystemruntime/TransientRoutingTest.kt
- [x] T023 [P] [US2] Add test for multiple handlers on same event type in TransientRoutingTest.kt
- [x] T024 [P] [US2] Add test for output event routing to configured channels in TransientRoutingTest.kt
- [x] T025 [P] [US2] Add test for correlation ID preservation in TransientRoutingTest.kt

### Implementation for User Story 2

- [x] T026 [P] [US2] Create TransientChannelHandler.kt for Vert.x EventBus integration in components/event-system-runtime/src/main/kotlin/eventsystemruntime/TransientChannelHandler.kt
- [x] T027 [US2] Implement EventRouter.routeEvent() method for routing to handlers (FR-007) in components/event-system-runtime/src/main/kotlin/eventsystemruntime/EventRouter.kt
- [x] T028 [US2] Implement output event routing based on SystemDefinition event-to-channel mapping (FR-008) in EventRouter.kt
- [x] T029 [US2] Implement JSON serialization/deserialization for Vert.x EventBus messages (FR-010) in TransientChannelHandler.kt
- [x] T030 [US2] Add support for multiple handlers per event type (FR-025) in EventRouter.kt

**Checkpoint**: ✅ User Stories 1 AND 2 complete - initialization and transient routing fully functional

---

## Phase 5: User Story 6 - Publish Events to Configured Channels (Priority: P1)

**Goal**: Provide publish() API for sending events to channels, routing to appropriate event bus

**Independent Test**: Publish events to various channels, verify delivery to correct event bus and handlers

### Tests for User Story 6

- [x] T031 [P] [US6] Create PublishingTest.kt with test for transient channel publishing in components/event-system-runtime/src/test/kotlin/eventsystemruntime/PublishingTest.kt
- [x] T032 [P] [US6] Add test for persistent channel publishing in PublishingTest.kt
- [x] T033 [P] [US6] Add test for JSON serialization during publishing in PublishingTest.kt
- [x] T034 [P] [US6] Add test for non-existent channel error logging in PublishingTest.kt

### Implementation for User Story 6

- [x] T035 [P] [US6] Create EventPublisher.kt for event publishing logic in components/event-system-runtime/src/main/kotlin/eventsystemruntime/EventPublisher.kt
- [x] T036 [US6] Implement publish() function for transient channels (FR-019) in EventPublisher.kt
- [x] T037 [US6] Implement publish() function for persistent channels in EventPublisher.kt
- [x] T038 [US6] Add correlation ID generation if missing in publish() function
- [x] T039 [US6] Add error logging for non-existent channels (best-effort delivery)

**Checkpoint**: ✅ MVP COMPLETE! User Stories 1, 2, AND 6 fully functional (core pub/sub system operational)

---

## Phase 6: User Story 4 - Support Multiple Handler Types (Priority: P1)

**Goal**: Support IHandler, IExecute, and plain function handlers with consistent execution

**Independent Test**: Register handlers of each type, verify all execute correctly when events published

### Tests for User Story 4

- [x] T040 [P] [US4] Create HandlerTypesTest.kt with test for IHandler execution in components/event-system-runtime/src/test/kotlin/eventsystemruntime/HandlerTypesTest.kt
- [x] T041 [P] [US4] Add test for IExecute execution in HandlerTypesTest.kt
- [x] T042 [P] [US4] Add test for plain function handler execution in HandlerTypesTest.kt
- [x] T043 [P] [US4] Add test for output event publishing from all handler types in HandlerTypesTest.kt

### Implementation for User Story 4

- [x] T044 [P] [US4] Implement IHandlerAdapter for IHandler type (wraps in Executor) in components/event-system-runtime/src/main/kotlin/eventsystemruntime/HandlerAdapter.kt
- [-] T045 [P] [US4] Implement IExecuteAdapter for IExecute type in HandlerAdapter.kt (BLOCKED: system-definition only supports IHandler)
- [-] T046 [P] [US4] Implement FunctionAdapter for plain function handlers in HandlerAdapter.kt (BLOCKED: system-definition only supports IHandler)
- [x] T047 [US4] Integrate handler adapters into EventRouter.routeEvent() (FR-006) in EventRouter.kt
- [x] T048 [US4] Add handler type detection and adapter selection logic in EventRouter.kt

**Checkpoint**: ✅ User Story 4 complete (IHandler support) - IExecute and plain functions BLOCKED by system-definition limitations

**Note**: Full handler type support (IExecute, plain functions) requires system-definition component changes (out of scope for this feature)

---

## Phase 7: User Story 3 - Route Events Through Persistent Channels (Priority: P2)

**Goal**: Route events to persistent channels with database durability and transactional context

**Independent Test**: Configure persistent channels, publish events, verify database storage and transactional context

### Tests for User Story 3

- [ ] T049 [P] [US3] Create PersistentRoutingTest.kt with test for persistent event publishing in components/event-system-runtime/src/test/kotlin/eventsystemruntime/PersistentRoutingTest.kt
- [ ] T050 [P] [US3] Add test for transactional context in handler in PersistentRoutingTest.kt
- [ ] T051 [P] [US3] Add test for successful transaction commit in PersistentRoutingTest.kt
- [ ] T052 [P] [US3] Add test for transaction rollback on handler exception in PersistentRoutingTest.kt

### Implementation for User Story 3

- [ ] T053 [P] [US3] Create PersistentChannelHandler.kt for postevent integration in components/event-system-runtime/src/main/kotlin/eventsystemruntime/PersistentChannelHandler.kt
- [ ] T054 [US3] Implement persistent channel subscription using postevent system (FR-005, FR-011) in PersistentChannelHandler.kt
- [ ] T055 [US3] Implement transactional context creation with database connection (FR-011) in PersistentChannelHandler.kt
- [ ] T056 [US3] Implement transaction commit/rollback logic in PersistentChannelHandler.kt
- [ ] T057 [US3] Integrate persistent channel handling into EventRouter.routeEvent()

**Checkpoint**: User Story 3 should work independently with database-backed event processing

---

## Phase 8: User Story 5 - Maintain Correlation IDs Across Event Flows (Priority: P2)

**Goal**: Automatically track and propagate correlation IDs through entire event flow

**Independent Test**: Publish event with/without correlation ID, verify preservation/generation in downstream events

### Tests for User Story 5

- [ ] T058 [P] [US5] Create CorrelationIdTest.kt with test for correlation ID preservation in components/event-system-runtime/src/test/kotlin/eventsystemruntime/CorrelationIdTest.kt
- [ ] T059 [P] [US5] Add test for correlation ID generation when missing in CorrelationIdTest.kt
- [ ] T060 [P] [US5] Add test for multi-step event flow tracing in CorrelationIdTest.kt

### Implementation for User Story 5

- [ ] T061 [P] [US5] Implement CorrelationIdManager.ensureCorrelationId() method (FR-009) in components/event-system-runtime/src/main/kotlin/eventsystemruntime/CorrelationIdManager.kt
- [ ] T062 [P] [US5] Implement CorrelationIdManager.propagateToOutputEvents() method in CorrelationIdManager.kt
- [ ] T063 [US5] Integrate correlation ID management into EventRouter.routeEvent()
- [ ] T064 [US5] Integrate correlation ID generation into EventPublisher.publish()
- [ ] T065 [US5] Add correlation ID to CoroutineContext for async propagation

**Checkpoint**: User Story 5 should work independently with full correlation ID tracking

---

## Phase 9: User Story 7 - Apply Execution Interceptors and Finalisers (Priority: P3)

**Goal**: Support interceptor/finaliser hooks for cross-cutting concerns (logging, tracing, security)

**Independent Test**: Register interceptor/finaliser functions, verify they're called at correct execution points

### Tests for User Story 7

- [ ] T066 [P] [US7] Create InterceptorTest.kt with test for interceptor invocation in components/event-system-runtime/src/test/kotlin/eventsystemruntime/InterceptorTest.kt
- [ ] T067 [P] [US7] Add test for finaliser invocation in InterceptorTest.kt
- [ ] T068 [P] [US7] Add test for context modification by interceptor in InterceptorTest.kt
- [ ] T069 [P] [US7] Add test for default pass-through behavior in InterceptorTest.kt

### Implementation for User Story 7

- [ ] T070 [P] [US7] Create InterceptorChain.kt for execution interceptors in components/event-system-runtime/src/main/kotlin/eventsystemruntime/InterceptorChain.kt
- [ ] T071 [US7] Implement registerInterceptor() function (FR-016) in InterceptorChain.kt
- [ ] T072 [US7] Implement registerFinaliser() function (FR-017) in InterceptorChain.kt
- [ ] T073 [US7] Implement interceptor execution before handler in InterceptorChain.kt
- [ ] T074 [US7] Implement finaliser execution after handler in InterceptorChain.kt
- [ ] T075 [US7] Integrate InterceptorChain into EventRouter.routeEvent()

**Checkpoint**: User Story 7 should work independently with interceptor/finaliser support

---

## Phase 10: Edge Cases & Resilience

**Purpose**: Handle edge cases and implement resilience patterns

### Tests for Edge Cases

- [ ] T076 [P] Create CircuitBreakerTest.kt with test for circuit breaker opening after N failures in components/event-system-runtime/src/test/kotlin/eventsystemruntime/CircuitBreakerTest.kt
- [ ] T077 [P] Add test for circuit breaker reset after timeout in CircuitBreakerTest.kt
- [ ] T078 [P] Add test for fail-fast when circuit breaker open in CircuitBreakerTest.kt
- [ ] T079 [P] Create ShutdownTest.kt with test for graceful shutdown within timeout in components/event-system-runtime/src/test/kotlin/eventsystemruntime/ShutdownTest.kt
- [ ] T080 [P] Add test for forced shutdown after timeout in ShutdownTest.kt
- [ ] T081 [P] Add test for handler exception isolation in ShutdownTest.kt

### Implementation for Edge Cases

- [ ] T082 [P] Create CircuitBreakerManager.kt for database failure resilience in components/event-system-runtime/src/main/kotlin/eventsystemruntime/CircuitBreakerManager.kt
- [ ] T083 [P] Create ShutdownCoordinator.kt for graceful shutdown in components/event-system-runtime/src/main/kotlin/eventsystemruntime/ShutdownCoordinator.kt
- [ ] T084 Implement circuit breaker pattern for persistent channels (FR-013-015) in CircuitBreakerManager.kt
- [ ] T085 Implement graceful shutdown with timeout (FR-020-022) in ShutdownCoordinator.kt
- [ ] T086 Implement handler exception isolation (FR-023-024) in EventRouter.kt
- [ ] T087 Add structured logging for handler errors with correlation IDs
- [ ] T088 Integrate CircuitBreakerManager into PersistentChannelHandler.kt
- [ ] T089 Integrate ShutdownCoordinator into EventSystemRuntime.shutdown()

**Checkpoint**: All edge cases handled, system is production-ready

---

## Phase 11: Contract Tests & Integration Validation

**Purpose**: Verify integration contracts with Vert.x and postevent

- [ ] T090 [P] Create EventRoutingContractTest.kt for Vert.x EventBus integration in components/event-system-runtime/src/test/kotlin/eventsystemruntime/contract/EventRoutingContractTest.kt
- [ ] T091 [P] Add test for transient channel address pattern in EventRoutingContractTest.kt
- [ ] T092 [P] Add test for event serialization format in EventRoutingContractTest.kt
- [ ] T093 [P] Create HandlerProtocolContractTest.kt for handler execution contracts in components/event-system-runtime/src/test/kotlin/eventsystemruntime/contract/HandlerProtocolContractTest.kt
- [ ] T094 [P] Add test for handler type adaptation in HandlerProtocolContractTest.kt
- [ ] T095 [P] Add test for handler failure isolation in HandlerProtocolContractTest.kt

**Checkpoint**: All integration contracts verified

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T096 [P] Add comprehensive KDoc documentation to all public APIs
- [ ] T097 [P] Add logging statements for all major operations (initialization, publishing, shutdown)
- [ ] T098 [P] Implement getStatus() function for runtime monitoring in EventSystemRuntime.kt
- [ ] T099 Code cleanup and refactoring for consistency
- [ ] T100 Performance optimization: review thread pool sizing and coroutine usage
- [ ] T101 Run quickstart.md validation to ensure examples work
- [ ] T102 [P] Update README.md with component overview and usage examples

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Edge Cases (Phase 10)**: Depends on User Stories 3 (persistent channels) for circuit breaker
- **Contract Tests (Phase 11)**: Can run in parallel with implementation
- **Polish (Phase 12)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 6 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 4 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 5 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 7 (P3)**: Can start after Foundational (Phase 2) - No dependencies on other stories

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Models/adapters before services
- Services before integration
- Core implementation before edge cases
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models/adapters within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T011: "Create EventSystemRuntimeTest.kt with test for transient channel subscription"
Task T012: "Add test for persistent channel subscription in EventSystemRuntimeTest.kt"
Task T013: "Add test for SystemDefinition validation failure in EventSystemRuntimeTest.kt"
Task T014: "Add test for missing datasource configuration error in EventSystemRuntimeTest.kt"

# Launch parallel implementation tasks:
Task T015: "Implement ValidationRules.validate() method"
Task T016: "Create ChannelSubscriber.kt for channel subscription management"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 6 Only - Core Pub/Sub)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Initialize)
4. Complete Phase 4: User Story 2 (Route Transient)
5. Complete Phase 5: User Story 6 (Publish)
6. **STOP and VALIDATE**: Test core pub/sub independently
7. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Stories 1, 2, 6 → Test independently → Deploy/Demo (MVP - core pub/sub!)
3. Add User Story 4 → Test independently → Deploy/Demo (handler flexibility)
4. Add User Story 3 → Test independently → Deploy/Demo (persistent channels)
5. Add User Story 5 → Test independently → Deploy/Demo (correlation tracking)
6. Add User Story 7 → Test independently → Deploy/Demo (interceptors)
7. Add Edge Cases (Phase 10) → Test independently → Deploy/Demo (production-ready)
8. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Initialize)
   - Developer B: User Story 2 (Route Transient)
   - Developer C: User Story 6 (Publish)
3. Then:
   - Developer A: User Story 4 (Handler Types)
   - Developer B: User Story 3 (Persistent)
   - Developer C: User Story 5 (Correlation)
4. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All tasks follow Constitution Principle III: Test-First Development

