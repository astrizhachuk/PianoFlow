# Design: Octave name label below note/chord (issue #20)

Date: 2026-06-14
Issue: https://github.com/astrizhachuk/PianoFlow/issues/20

## Goal

When a single note is active, show the traditional octave name (localized) on a
separate, smaller line below the note name in the chord card. Hide it for chords
and when nothing is played.

## Octave mapping

Scientific octave number is derived from the MIDI pitch: `octave = pitch / 12 - 1`.
Each number 0..8 maps to a traditional octave name. Outside 0..8 (below A0 / above
C8) the label is hidden — the physical piano range A0..C8 is always covered.

| Scientific octave | Range starts | English            | Russian          |
|-------------------|--------------|--------------------|------------------|
| 0                 | A0           | Sub-contra octave  | Субконтроктава   |
| 1                 | C1           | Contra octave      | Контроктава      |
| 2                 | C2           | Great octave       | Большая октава   |
| 3                 | C3           | Small octave       | Малая октава     |
| 4                 | C4           | One-lined octave   | Первая октава    |
| 5                 | C5           | Two-lined octave   | Вторая октава    |
| 6                 | C6           | Three-lined octave | Третья октава    |
| 7                 | C7           | Four-lined octave  | Четвертая октава |
| 8                 | C8           | Five-lined octave  | Пятая октава     |

## Architectural decision: presentation-only

This feature is **visualization of already-available data**, not new business logic,
so it lives entirely in the presentation layer. The domain layer is not touched.

Rationale:

1. The octave number is already exposed by the domain via `Note.pitch`
   (`octave = pitch / 12 - 1`). No new domain computation is required.
2. Turning the octave number into a human-readable, localized label is a
   number → localized-string lookup. Localization and string resources are, by
   definition, a presentation concern (per `ARCHITECTURE_PRINCIPLES.md`: the
   presentation layer displays data and contains no business logic).

A domain model (`Octave` enum + factory + use case) was considered and rejected as
over-engineering for the current scope (YAGNI). It would be justified only if the
octave became a first-class domain concept used in business logic (e.g. theory
exercises "play a note in the small octave", octave-based filtering). That feature
does not exist today; if it appears, introduce the domain abstraction then.

## Trigger condition

The octave label is shown only when exactly one note is active (`notes.size == 1`).
For chords (2+ notes) and the empty state it is `null`. The octave is computed from
`notes[0].pitch` directly, not by parsing the displayed chord/note string.

## Components and data flow

```
ObserveMidiMessagesUseCase : Flow<List<Note>>   (unchanged)
        │
        ▼
PianoStaffViewModel.combine { notes, analysisResult ->
    chordName  = … (unchanged)
    octaveName = if (notes.size == 1)
                     octaveLabelResOrNull(notes[0].pitch)
                         ?.let { context.getString(it) }
                 else null
}
        │
        ▼
PianoStaffUiState(notesJson, chordName, octaveName)
        │
        ▼
PianoStaffContent → ChordCard(chordName, octaveName)
```

### Presentation helper

A pure function near `ChordCard`:

```kotlin
@StringRes
private fun octaveLabelResOrNull(midi: Int): Int?
```

Maps scientific octave 0..8 to the corresponding `R.string.octave_*`, returns
`null` otherwise. The number → resource-id mapping is unit-testable as a plain
function.

### UiState

`PianoStaffUiState` gains `val octaveName: String? = null`.

### ChordCard

Gains an `octaveName: String?` parameter. Below the main note/chord `Text`
(`displaySmall`), when `octaveName != null`, render a second `Text` in a smaller
style with a muted color. When `null`, the line takes no space.

## Localization

Nine keys `octave_sub_contra` … `octave_five_lined` added to both
`values/strings.xml` (English) and `values-ru/strings.xml` (Russian), per the table
above.

## Testing

- Unit test for `octaveLabelResOrNull`: boundary MIDI values (A0=21, C1=24,
  C4=60, C8=108) map to the expected resource id; out-of-range values
  (e.g. 0..20, ≥120) return `null`.
- `PianoStaffViewModelTest`: a single note produces a non-null `octaveName`;
  a chord and the empty state produce `null`.
- Optional UI/preview check that the second line appears only for a single note.

## Out of scope

- Domain `Octave` model and use case (deferred until a real domain need exists).
- Octave names beyond the A0..C8 piano range.
