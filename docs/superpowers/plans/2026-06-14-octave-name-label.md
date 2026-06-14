# Octave Name Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the traditional, localized octave name (e.g. "One-lined octave" / "Первая октава") on a smaller line below the note name in the chord card, only when a single note is active.

**Architecture:** Presentation-only. The octave number is derived from the already-available `Note.pitch`; mapping it to a localized string is a presentation concern. No domain changes. A pure `octaveLabelResOrNull(midi)` helper maps MIDI → `@StringRes`; the ViewModel resolves it to a string and exposes `octaveName` on `PianoStaffUiState`; `ChordCard` renders the extra line.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Hilt, JUnit 4 + MockK + Turbine.

See design spec: `docs/superpowers/specs/2026-06-14-octave-name-label-design.md`.

---

## File Structure

- Create: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabel.kt` — pure MIDI → `@StringRes` mapper (mirrors the top-level helper style of `VexflowNoteMapper.kt`).
- Create: `app/src/test/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabelTest.kt` — unit tests for the mapper.
- Modify: `app/src/main/res/values/strings.xml` — 9 English octave names.
- Modify: `app/src/main/res/values-ru/strings.xml` — 9 Russian octave names.
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/model/pianostaff/PianoStaffUiState.kt` — add `octaveName`.
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModel.kt` — compute `octaveName`.
- Modify: `app/src/test/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModelTest.kt` — tests for `octaveName`.
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/PianoStaffScreen.kt` — thread `octaveName` through `PianoStaffContent` and render it in `ChordCard`; update previews.

---

## Task 1: Add octave string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ru/strings.xml`

These must exist first so `R.string.octave_*` references compile in later tasks.

- [ ] **Step 1: Add English strings**

In `app/src/main/res/values/strings.xml`, add before the closing `</resources>`:

```xml
    <string name="octave_sub_contra">Sub-contra octave</string>
    <string name="octave_contra">Contra octave</string>
    <string name="octave_great">Great octave</string>
    <string name="octave_small">Small octave</string>
    <string name="octave_one_lined">One-lined octave</string>
    <string name="octave_two_lined">Two-lined octave</string>
    <string name="octave_three_lined">Three-lined octave</string>
    <string name="octave_four_lined">Four-lined octave</string>
    <string name="octave_five_lined">Five-lined octave</string>
```

- [ ] **Step 2: Add Russian strings**

In `app/src/main/res/values-ru/strings.xml`, add before the closing `</resources>` (note: no letter «ё» per project rules — "Четвертая", not "Четвёртая"):

```xml
    <string name="octave_sub_contra">Субконтроктава</string>
    <string name="octave_contra">Контроктава</string>
    <string name="octave_great">Большая октава</string>
    <string name="octave_small">Малая октава</string>
    <string name="octave_one_lined">Первая октава</string>
    <string name="octave_two_lined">Вторая октава</string>
    <string name="octave_three_lined">Третья октава</string>
    <string name="octave_four_lined">Четвертая октава</string>
    <string name="octave_five_lined">Пятая октава</string>
```

- [ ] **Step 3: Verify the project still compiles resources**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (R class regenerated with the new ids).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
git commit -m "feat(ui): add octave name string resources (#20)"
```

---

## Task 2: Octave label mapper (`octaveLabelResOrNull`)

**Files:**
- Create: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabel.kt`
- Test: `app/src/test/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabelTest.kt`

The mapper is pure (returns an `Int?` resource id), so it is tested as a plain JVM function — no Robolectric needed, the test only compares int ids.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabelTest.kt`:

```kotlin
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OctaveLabelTest {

    @Test
    fun `maps each octave boundary C to its resource`() {
        assertEquals(R.string.octave_sub_contra, octaveLabelResOrNull(21))  // A0
        assertEquals(R.string.octave_contra, octaveLabelResOrNull(24))      // C1
        assertEquals(R.string.octave_great, octaveLabelResOrNull(36))       // C2
        assertEquals(R.string.octave_small, octaveLabelResOrNull(48))       // C3
        assertEquals(R.string.octave_one_lined, octaveLabelResOrNull(60))   // C4
        assertEquals(R.string.octave_two_lined, octaveLabelResOrNull(72))   // C5
        assertEquals(R.string.octave_three_lined, octaveLabelResOrNull(84)) // C6
        assertEquals(R.string.octave_four_lined, octaveLabelResOrNull(96))  // C7
        assertEquals(R.string.octave_five_lined, octaveLabelResOrNull(108)) // C8
    }

    @Test
    fun `B8 still maps to five-lined, C9 and above are null`() {
        assertEquals(R.string.octave_five_lined, octaveLabelResOrNull(119)) // B8
        assertNull(octaveLabelResOrNull(120)) // C9
        assertNull(octaveLabelResOrNull(127)) // top of MIDI range
    }

    @Test
    fun `pitches below A0 octave are null`() {
        assertNull(octaveLabelResOrNull(11)) // octave -1
        assertNull(octaveLabelResOrNull(0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astrizhachuk.pianoflow.presentation.ui.pianostaff.OctaveLabelTest"`
Expected: FAIL — `octaveLabelResOrNull` is unresolved (does not compile yet).

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabel.kt`:

```kotlin
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.annotation.StringRes
import com.astrizhachuk.pianoflow.R

private const val SEMITONES_PER_OCTAVE = 12

/**
 * Maps a MIDI pitch to the string resource of its traditional octave name.
 *
 * The scientific octave number is `pitch / 12 - 1`. Only octaves 0..8 (A0..C8, the
 * physical piano range) have a named label; any pitch outside that range returns null
 * so the caller can hide the octave line.
 *
 * @param midi MIDI pitch (0..127).
 * @return The `@StringRes` id of the octave name, or null when the octave is unnamed.
 */
@StringRes
internal fun octaveLabelResOrNull(midi: Int): Int? =
    when (midi / SEMITONES_PER_OCTAVE - 1) {
        0 -> R.string.octave_sub_contra
        1 -> R.string.octave_contra
        2 -> R.string.octave_great
        3 -> R.string.octave_small
        4 -> R.string.octave_one_lined
        5 -> R.string.octave_two_lined
        6 -> R.string.octave_three_lined
        7 -> R.string.octave_four_lined
        8 -> R.string.octave_five_lined
        else -> null
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astrizhachuk.pianoflow.presentation.ui.pianostaff.OctaveLabelTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabel.kt app/src/test/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/OctaveLabelTest.kt
git commit -m "feat(ui): add octave label MIDI-to-resource mapper (#20)"
```

---

## Task 3: Add `octaveName` to UI state

**Files:**
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/model/pianostaff/PianoStaffUiState.kt`

- [ ] **Step 1: Add the field**

Replace the data class in `PianoStaffUiState.kt` with:

```kotlin
/**
 * Represents the UI state for the piano staff screen.
 *
 * @param notesJson A JSON string containing the notes for both the treble and bass clefs.
 *                  It defaults to an empty structure: `{"treble":[], "bass":[]}`.
 * @param chordName The name of the analyzed chord, if any.
 * @param octaveName The localized traditional octave name, shown only for a single note;
 *                   null for chords, the empty state, or octaves outside the A0..C8 range.
 */
data class PianoStaffUiState(
    val notesJson: String = "{\"treble\":[], \"bass\":[]}",
    val chordName: String? = null,
    val octaveName: String? = null
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the new field has a default, so existing call sites are unaffected).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/astrizhachuk/pianoflow/presentation/model/pianostaff/PianoStaffUiState.kt
git commit -m "feat(ui): add octaveName to PianoStaffUiState (#20)"
```

---

## Task 4: Compute `octaveName` in the ViewModel

**Files:**
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModel.kt:61-74`
- Test: `app/src/test/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these two tests inside `PianoStaffViewModelTest` (before the final closing brace). The single-note test stubs the octave string; the chord test relies on the default `octaveName == null`:

```kotlin
    @Test
    fun `single note exposes localized octave name`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4")) // C4 -> one-lined octave
        every { notes.toVexflowJson() } returns "json"
        every { context.getString(R.string.octave_one_lined) } returns "One-lined octave"

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            assertEquals("One-lined octave", awaitItem().octaveName)
        }
    }

    @Test
    fun `chord does not expose an octave name`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4"), Note(64, "E4"), Note(67, "G4"))
        every { notes.toVexflowJson() } returns "json"

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            assertEquals(null, awaitItem().octaveName)
        }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModelTest"`
Expected: `single note exposes localized octave name` FAILS (octaveName is null because nothing computes it yet). `chord does not expose an octave name` passes trivially.

- [ ] **Step 3: Add the import**

In `PianoStaffViewModel.kt`, the helper is in the same package as `toVexflowJson` and is already imported via the existing `import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson`. Add a second import line beside it:

```kotlin
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.octaveLabelResOrNull
```

- [ ] **Step 4: Compute `octaveName` in the combine block**

Replace the `combine` lambda body (currently lines 61-74) with:

```kotlin
            .combine(observeChordAnalysisResultsUseCase()) { notes, analysisResult ->
                val notesJson = notes.toVexflowJson()

                val displayChordName = when {
                    analysisResult != null -> analysisResult
                    notes.isNotEmpty() -> context.getString(R.string.chord_not_defined)
                    else -> null
                }

                val octaveName = notes.singleOrNull()
                    ?.let { octaveLabelResOrNull(it.pitch) }
                    ?.let { context.getString(it) }

                PianoStaffUiState(
                    notesJson = notesJson,
                    chordName = displayChordName,
                    octaveName = octaveName
                )
            }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModelTest"`
Expected: PASS (all tests, including the two new ones).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModel.kt app/src/test/java/com/astrizhachuk/pianoflow/presentation/viewmodel/pianostaff/PianoStaffViewModelTest.kt
git commit -m "feat(ui): compute octave name for single notes in PianoStaffViewModel (#20)"
```

---

## Task 5: Render the octave line in `ChordCard`

**Files:**
- Modify: `app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/PianoStaffScreen.kt`

This task has no unit test (Compose UI); verify via build and the `@Preview` composables.

- [ ] **Step 1: Pass `octaveName` from the screen to the content**

In `PianoStaffScreen` (the stateful composable), update the `PianoStaffContent` call to pass the new field:

```kotlin
    PianoStaffContent(
        chordName = uiState.chordName,
        octaveName = uiState.octaveName,
        notesJson = uiState.notesJson,
        windowInfo = windowInfo,
        modifier = modifier
    )
```

- [ ] **Step 2: Add `octaveName` to `PianoStaffContent` and forward it to both `ChordCard` call sites**

Change the `PianoStaffContent` signature to add the parameter right after `chordName`:

```kotlin
@Composable
fun PianoStaffContent(
    chordName: String?,
    octaveName: String?,
    notesJson: String,
    windowInfo: WindowInfo,
    modifier: Modifier = Modifier
) {
```

In the portrait branch, update the `ChordCard` call:

```kotlin
            ChordCard(
                chordName = chordName,
                octaveName = octaveName,
                fillHeight = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = mediumPadding)
            )
```

In the landscape branch, update the `ChordCard` call:

```kotlin
            ChordCard(
                chordName = chordName,
                octaveName = octaveName,
                fillHeight = false,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = extraSmallPadding)
            )
```

- [ ] **Step 3: Add the `octaveName` parameter and the second `Text` line to `ChordCard`**

Update the `ChordCard` signature and its `Column` body. Add the parameter after `chordName`, and render the octave line after the existing note/chord `Text`:

```kotlin
@Composable
private fun ChordCard(
    chordName: String?,
    octaveName: String?,
    fillHeight: Boolean,
    modifier: Modifier = Modifier
) {
```

Inside the `Column` (after the existing `displaySmall` `Text` for the chord name), add:

```kotlin
                if (octaveName != null) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_small)))
                    Text(
                        text = octaveName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
```

- [ ] **Step 4: Update existing previews and add a single-note preview**

Each existing `PianoStaffContent(...)` call in the `@Preview` functions needs the new `octaveName` argument. Set `octaveName = null` for the chord/empty previews:
- `PianoStaffContentPortraitPreview` → add `octaveName = null,`
- `PianoStaffContentPortraitNoChordPreview` → add `octaveName = null,`
- `PianoStaffContentLandscapePreview` → add `octaveName = null,`
- `PianoStaffContentLandscapeDarkPreview` → add `octaveName = null,`

Then add a new preview showing a single note with its octave line, after the existing previews:

```kotlin
@Preview(apiLevel = 34)
@Composable
fun PianoStaffContentSingleNotePreview() {
    AppTheme(darkTheme = false) {
        PianoStaffContent(
            chordName = "C4",
            octaveName = "One-lined octave",
            notesJson = "{\"treble\":[{\"keys\":[\"c/4\"], \"duration\":\"w\"}], \"bass\":[]}",
            windowInfo = WindowInfo(isLandscape = false, isPhone = true),
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/astrizhachuk/pianoflow/presentation/ui/pianostaff/PianoStaffScreen.kt
git commit -m "feat(ui): show octave name line in chord card for single notes (#20)"
```

---

## Task 6: Full verification

- [ ] **Step 1: Run the whole unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run Lint (catches missing translations / unused resources)**

Run: `./gradlew :app:lintDebug`
Expected: No new errors; the 9 strings exist in both `values` and `values-ru` (no `MissingTranslation`).

- [ ] **Step 3: Assemble the debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

---

## Notes for the implementer

- **Font size of the octave line** is `titleMedium` with 70% alpha — a deliberate "smaller, auxiliary" look. If the visual weight feels off after seeing it on device, `bodyMedium` is the lighter alternative; this is a one-line style change in Task 5 Step 3.
- **No domain changes**: do not add an `Octave` enum or use case — that was explicitly considered and rejected in the design (YAGNI).
- **PR**: title in English (Conventional Commits), bilingual body, `Closes #20`. Squash merge for the feature. Apply no labels to the PR (the issue carries the `enhancement` label).
