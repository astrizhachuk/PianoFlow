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

```
presentation/   ← MVVM (ViewModels, Compose UI, state models)
domain/         ← Use cases, repository interfaces, domain models, services (pure Kotlin)
data/           ← Repository implementations, data sources, mappers
```

Package naming: `{layer}.{component_type}.{functionality}`, e.g. `domain.usecase.midi`.

### Key Data Flow

1. Android MIDI API → `MidiDataSource` → `MidiMessageParser` → raw MIDI bytes
2. `ObserveMidiMessagesUseCase` groups notes within a 260ms time window into chords
3. `AnalyzeChordUseCase` → `ChordAnalysisRepositoryImpl` → `MusicScriptEngine` (JavaScript via WebView + VexFlow)
4. Results flow via Kotlin `StateFlow`/`Flow` into `PianoStaffViewModel` → Compose UI

### Chord Analysis via WebView

`MusicScriptEngine` runs JavaScript music theory logic inside a `WebView`. The script file is `assets/vexflow.html`. Results are returned asynchronously via `JavascriptInterface`.

### Dependency Injection

Hilt is used throughout. Key DI modules:
- `data/di/DataModule.kt` — binds repository interfaces to implementations
- `presentation/di/NotificationModule.kt` — binds `UserNotifier`

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