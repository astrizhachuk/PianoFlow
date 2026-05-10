# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build (R8 minification enabled)
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires connected device/emulator)
./gradlew lint                   # Android Lint checks
./gradlew clean                  # Clean build artifacts

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCaseTest"
```

Custom `HiltTestRunner` is used for instrumented tests (`testInstrumentationRunner` in `app/build.gradle.kts`).

## Architecture

**Clean Architecture** with three strictly separated layers. The dependency rule: `Presentation → Domain ← Data`. The Domain layer has **zero** platform dependencies.

## Code Standards

Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Kotlin idioms](https://kotlinlang.org/docs/idioms.html). Any deviation must be explicitly noted.

Architecture principles are documented in `docs/en/plans/ARCHITECTURE_PRINCIPLES.md` — consult it for layer responsibilities, naming rules, logging strategy, and planned multi-platform extension.

Testing strategy is in `docs/en/plans/TESTING_STRATEGY.md`. All business logic must have unit tests (JUnit 4 + MockK + Turbine for flows). Use Robolectric for tests requiring Android context. UI tests use Espresso + Hilt test utilities.

## Logging

Use **Timber** for all logging (never `Log.*` directly):
- Debug build: `Timber.DebugTree()` → Logcat
- Release build: `CrashReportingTree` → analytics/crash reporting

## Localization

All user-visible strings must be in both `values/strings.xml` (English) and `values-ru/strings.xml` (Russian). Keep them in sync.

## Workflow: Issues and Pull Requests

PianoFlow uses a **hybrid GitHub Flow**: an issue is created only when it adds value — to keep a useful changelog without bureaucratic ceremony.

### Issue is REQUIRED for

| Category | Reason |
|----------|--------|
| Bug | Reproduction history, environment, and steps must be preserved |
| New feature / idea | Design and discussion happen before code, to avoid PR rewrites |
| Architectural improvement | Decision is made before code — see `docs/en/plans/ARCHITECTURE_PRINCIPLES.md` |

Suggested labels: `bug`, `enhancement`, `idea`, `architecture`.

### Direct PR (no issue) for

- Typos and formatting
- Refactoring without behavior change
- Dependency updates
- Documentation edits (`docs/en` + `docs/ru`)
- Obvious small fixes (≤30 lines, self-explanatory from the diff)

### Labels

Apply labels to **issues**, not to PRs — the linked PR (`Closes #N`) inherits context from the issue, and the issue is the single source of truth for the "what". Add labels to a PR only when:

- The PR has no linked issue (direct PR for typos, deps, docs, small fixes).
- Automation depends on PR labels (Release Drafter, label-gated CI, etc.).

This avoids manual synchronization between issue and PR labels.

### Linking issue and PR

Every PR that resolves an issue must include `Closes #N` or `Fixes #N` in the description. GitHub closes the linked issue automatically on merge — this gives an "idea-to-code" trail without separate tracking.

### Bilingual descriptions (MANDATORY for AI-authored content)

When AI (Claude Code or any other agent) authors an issue or pull request description, the body MUST be bilingual: English first, then a horizontal rule (`---`), then a Russian version with the same structure and content.

Issue and PR **titles MUST always be written in English** (Conventional Commits style), regardless of the body language and regardless of who authors them. Titles drive changelogs, search, and tooling — they stay in a single working language.

The bilingual-body rule is mandatory for AI; for human contributors a bilingual description is encouraged but not required (see `CONTRIBUTING.md`). The English-title rule applies to everyone.

### Minimal process

1. Idea or bug → issue (if it falls into the "required" table above)
2. Branch: `feat/<short-desc>`, `fix/<short-desc>`, `docs/<short-desc>`, `refactor/<short-desc>`
3. Commits — Conventional Commits
4. PR via `gh pr create` with template (Summary + Test plan + `Closes #N`)
5. Self-review → merge (squash for features, regular merge for integration PRs)
6. Delete the branch

**Rationale:** issue-first preserves traceability for meaningful changes; direct PRs keep technical hygiene fast. The hybrid avoids both extremes — full ceremony and lost decision history.

## Documentation

Documentation is maintained in two languages under `docs/`:

```
docs/
  en/   ← English (source of truth)
  ru/   ← Russian (translation)
```

Subdirectories mirror each other: `plans/`, `specs/`, `tech/`, `uc/`.

**Rules:**
- English is the single source of truth. Make all content decisions in `docs/en/` first.
- After any change to an English document, update the corresponding Russian document in `docs/ru/` to keep them in sync.
- When adding a new document, create both language versions simultaneously.
- If only the Russian document exists for some topic, treat it as a draft — create the English version before merging.

**Spec section convention:**
- `### 1.2. Base Documents` (in §1 General Information) — **prerequisites**: parent specs, use cases, architectural principles the reader needs to know before reading this spec.
- `## See Also` (trailing, optional) — **additional technical references** not strictly required: tech docs (Kotlin Flow, MIDI API, etc.). Omit the section entirely if there are no entries beyond what is already listed in Base Documents — never duplicate.
- Specs describe the **current state** of the system, not the history of changes. Migration narratives (file removals, "rewritten internals", "previously used X, now uses Y") belong in commit messages and PR descriptions, not in `specs/`.