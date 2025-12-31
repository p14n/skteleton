<!--
═══════════════════════════════════════════════════════════════════════════════
SYNC IMPACT REPORT
═══════════════════════════════════════════════════════════════════════════════
Version Change: NONE → 1.0.0 (Initial ratification)

Modified Principles: N/A (Initial creation)

Added Sections:
  - Core Principles (7 principles)
  - Component Architecture
  - Development Workflow
  - Governance

Removed Sections: N/A

Templates Requiring Updates:
  ✅ .specify/templates/plan-template.md - Constitution Check section aligned
  ✅ .specify/templates/spec-template.md - Requirements structure aligned
  ✅ .specify/templates/tasks-template.md - Task categorization aligned
  ✅ .specify/templates/agent-file-template.md - No changes required

Follow-up TODOs: None
═══════════════════════════════════════════════════════════════════════════════
-->

# Skteleton Constitution

## Core Principles

### I. Component-First Architecture

Every feature MUST be developed as a standalone component module. Components MUST be:
- Self-contained with clear boundaries and minimal dependencies
- Independently buildable and testable via Gradle build tool
- Documented with clear purpose and API contracts
- Located in the `components/` directory with consistent structure

**Rationale**: Component isolation enables parallel development, easier testing, and clearer
dependency management. This aligns with the event-driven architecture pattern already
established in the project (event-protocol, event-system).

### II. Specification-Driven Development

All features MUST begin with a complete specification before implementation. The specification
workflow is NON-NEGOTIABLE:
1. Feature specification created via `/speckit.specify` command
2. Implementation plan generated via `/speckit.plan` command
3. Tasks broken down via `/speckit.tasks` command
4. Implementation executed via `/speckit.implement` command

**Rationale**: Specification-first development ensures clear requirements, reduces rework,
enables better estimation, and creates living documentation. The speckit framework enforces
this discipline systematically.

### III. Test-First Development (NON-NEGOTIABLE)

TDD is mandatory for all production code. The cycle MUST be strictly followed:
1. Write tests based on specification acceptance criteria
2. Verify tests FAIL (red phase)
3. Implement minimum code to pass tests (green phase)
4. Refactor while keeping tests green

All components MUST include JUnit5 test modules using Kotlin test framework. Test coverage
MUST include unit, integration, and contract tests as appropriate.

**Rationale**: Test-first development catches defects early, drives better design, provides
regression safety, and serves as executable documentation. The Gradle build structure already
supports this with dedicated test modules.

### IV. Independent User Stories

Features MUST be decomposed into independently testable user stories with clear priorities
(P1, P2, P3). Each user story MUST:
- Deliver standalone value that can be demonstrated independently
- Be implementable without requiring other stories to be complete
- Include specific acceptance scenarios in Given-When-Then format
- Map to discrete tasks in the implementation plan

**Rationale**: Independent stories enable incremental delivery, parallel development, early
validation, and flexible prioritization. This supports MVP-first and iterative delivery models.

### V. Observability and Debuggability

All components MUST be observable and debuggable through:
- Structured logging with appropriate log levels
- Clear error messages with actionable context
- Event tracing for event-driven interactions
- Text-based I/O protocols where applicable (stdin/stdout for CLI tools)

**Rationale**: Observability is essential for debugging distributed event-driven systems.
Text-based protocols and structured logging enable inspection, testing, and troubleshooting
without specialized tools.

### VI. Semantic Versioning and Breaking Changes

All components MUST follow semantic versioning (MAJOR.MINOR.PATCH):
- MAJOR: Breaking API changes, removed functionality, incompatible protocol changes
- MINOR: New features, new APIs, backward-compatible enhancements
- PATCH: Bug fixes, documentation updates, non-functional improvements

Breaking changes MUST include:
- Migration guide in component documentation
- Deprecation warnings in prior MINOR version when possible
- Update to dependent components and integration tests

**Rationale**: Semantic versioning provides clear expectations for consumers, enables safe
upgrades, and forces deliberate consideration of breaking changes in component-based systems.

### VII. Simplicity and YAGNI

Complexity MUST be justified. Default to the simplest solution that meets requirements:
- Avoid premature abstraction and over-engineering
- Implement features only when needed (YAGNI - You Aren't Gonna Need It)
- Prefer composition over inheritance
- Keep Kotlin code idiomatic and readable

Any deviation from simplicity MUST be documented in the implementation plan's Complexity
Tracking section with explicit justification.

**Rationale**: Simplicity reduces cognitive load, maintenance burden, and defect rates.
The Complexity Tracking mechanism ensures complexity is conscious and justified rather than
accidental.

## Component Architecture

### Technology Stack

- **Language**: Kotlin 2.0.21
- **Build Tool**: Gradle 9.2.1
- **Testing**: JUnit5 with Kotlin test framework
- **Module Structure**: Gradle modules under `components/` directory

### Component Structure Requirements

Each component MUST follow this structure:
```
components/
└── component-name/
    ├── src/
    │   └── [component source code]
    └── test/
        └── [JUnit5 tests]
```

Components MUST declare dependencies explicitly via Gradle's `dependencies` block.

### Event-Driven Patterns

Components using event-driven patterns MUST:
- Define event protocols in dedicated protocol modules (e.g., `event-protocol`)
- Implement event handlers in separate system modules (e.g., `event-system`)
- Document event contracts and message formats
- Include contract tests for event interactions

## Development Workflow

### Specification Workflow

1. **Specify**: Create feature specification with user stories and acceptance criteria
2. **Plan**: Generate implementation plan with technical context and structure decisions
3. **Task Breakdown**: Create dependency-ordered task list organized by user story
4. **Implement**: Execute tasks in phases (Setup → Foundational → User Stories → Polish)
5. **Validate**: Verify each user story independently against acceptance criteria

### Constitution Compliance Gates

Before Phase 0 research and after Phase 1 design, implementations MUST verify:
- [ ] Component isolation maintained (Principle I)
- [ ] Specification complete and approved (Principle II)
- [ ] Tests written and failing before implementation (Principle III)
- [ ] User stories are independently testable (Principle IV)
- [ ] Observability mechanisms included (Principle V)
- [ ] Versioning strategy documented (Principle VI)
- [ ] Complexity justified in plan (Principle VII)

### Code Review Requirements

All code changes MUST:
- Pass all tests (unit, integration, contract as applicable)
- Include tests for new functionality
- Update documentation for API changes
- Verify constitution compliance
- Build successfully via Gradle

## Governance

### Authority and Precedence

This constitution supersedes all other development practices and guidelines. In case of
conflict between this constitution and other documentation, the constitution takes precedence.

### Amendment Process

Constitution amendments MUST:
1. Document the proposed change with clear rationale
2. Identify impact on existing components and templates
3. Update version according to semantic versioning rules:
   - MAJOR: Principle removal or incompatible governance changes
   - MINOR: New principle or materially expanded guidance
   - PATCH: Clarifications, wording improvements, non-semantic refinements
4. Propagate changes to all dependent templates and documentation
5. Include migration plan for existing code if applicable

### Compliance Review

Constitution compliance MUST be verified:
- During specification review (before implementation begins)
- During code review (before merging changes)
- During retrospectives (to identify systemic violations)

Violations MUST be either:
- Corrected to achieve compliance, OR
- Explicitly justified in the Complexity Tracking section with approval

### Living Documentation

The constitution is a living document. Updates MUST maintain the Sync Impact Report at the
top of this file documenting version history and template synchronization status.

**Version**: 1.0.0 | **Ratified**: 2025-12-22 | **Last Amended**: 2025-12-22
