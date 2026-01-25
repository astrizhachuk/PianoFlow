
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import timber.log.Timber

/**
 * The range of MIDI note pitches that should be duplicated on the adjacent staff.
 *
 * This range (C2 to C5) covers the area where notes are commonly written on ledger lines
 * between the treble and bass staves. By creating "ghost" notes for pitches within this
 * range, we can display the same note on both staves, making it easier for the user to read.
 * For example, a Middle C (MIDI pitch 60) will be shown on the bass staff's top ledger line
 * and the treble staff's bottom ledger line.
 */
private val GHOST_NOTE_RANGE = 36..72

/**
 * Converts a list of played [Note] objects into a JSON string formatted for VexFlow.
 *
 * This function processes a list of MIDI notes and organizes them for display on a grand staff
 * (treble and bass clefs). It performs the following steps:
 *
 * 1.  **Separates Notes:** Divides the notes into two groups based on their pitch:
 *     -   **Treble Clef:** Notes with MIDI pitch 60 (Middle C) and above.
 *     -   **Bass Clef:** Notes with MIDI pitch below 60.
 *
 * 2.  **Creates "Ghost" Notes:** To improve readability for notes in the middle range (C2 to C5,
 *     inclusive), it creates duplicate "ghost" notes on the adjacent staff. For example, a C4
 *     played on the right hand (treble clef) will also appear as a ghost note on the bass clef.
 *
 * 3.  **Generates JSON:** Constructs a JSON object with `treble` and `bass` keys. Each key holds an
 *     array of VexFlow note objects. Ghost notes are flagged with a `"ghost": true` property,
 *     allowing them to be styled differently in the frontend (e.g., with lower opacity).
 *
 * The final JSON structure looks like this:
 * ```json
 * {
 *   "treble": [{"keys":["c/5"], "duration":"w"}, {"keys":["g/4"], "duration":"w", "ghost":true}],
 *   "bass": [{"keys":["g/3"], "duration":"w"}]
 * }
 * ```
 *
 */
fun List<Note>.toVexflowJson(): String {
    Timber.d("toVexflowJson() called with notes: $this")
    if (this.isEmpty()) {
        return "{\"treble\":[], \"bass\":[]}"
    }

    val trebleNotes = this.filter { it.pitch >= 60 }
    val bassNotes = this.filter { it.pitch < 60 }

    val ghostNotesForTreble = bassNotes.filter { it.pitch in GHOST_NOTE_RANGE }
    val ghostNotesForBass = trebleNotes.filter { it.pitch in GHOST_NOTE_RANGE }

    val trebleNoteObjects = createNoteObjects(primary = trebleNotes, ghost = ghostNotesForTreble)
    val bassNoteObjects = createNoteObjects(primary = bassNotes, ghost = ghostNotesForBass)

    val result = "{\"treble\":$trebleNoteObjects, \"bass\":$bassNoteObjects}"
    Timber.d("toVexflowJson() result: $result")
    return result
}

/**
 * Creates a JSON array string representing notes for a single staff.
 *
 * This function takes two lists of notes: primary notes that belong to the staff,
 * and ghost notes that are duplicates from the adjacent staff. It converts them
 * into a JSON array of VexFlow note objects. Ghost notes are marked with a special flag.
 *
 * @param primary A list of `Note` objects that primarily belong to this staff.
 * @param ghost A list of `Note` objects to be displayed as "ghost" notes on this staff.
 * @return A string representing a JSON array of VexFlow note objects.
 *         Example: `[{"keys":["c/4", "e/4"], "duration":"w"}, {"keys":["a/3"], "duration":"w", "ghost":true}]`
 */
private fun createNoteObjects(primary: List<Note>, ghost: List<Note>): String {
    val objects = mutableListOf<String>()
    primary.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
        objects.add(createVexflowNoteJson(it, isGhost = false))
    }
    ghost.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
        objects.add(createVexflowNoteJson(it, isGhost = true))
    }
    return objects.joinToString(separator = ",", prefix = "[", postfix = "]")
}

/**
 * Creates a JSON string for a single chord to be rendered by VexFlow.
 *
 * This function takes a list of note keys (e.g., "c/4", "e/4") and constructs a JSON object
 * representing a whole note chord. It can also mark the chord as a "ghost" note, which is typically
 * used for rendering notes that appear on both staves for readability.
 *
 * The resulting JSON format is `{"keys":["c/4", "e/4"], "duration":"w", "ghost":true}`.
 *
 * @param keys A list of strings, where each string is a VexFlow note representation (e.g., "c/4").
 * @param isGhost A boolean flag indicating if this chord should be treated as a "ghost" chord.
 * @return A JSON formatted string representing the chord.
 */
private fun createVexflowNoteJson(keys: List<String>, isGhost: Boolean): String {
    val keysJson = keys.joinToString(separator = ", ") { "\"$it\"" }
    val ghostJson = if (isGhost) ", \"ghost\":true" else ""
    return "{\"keys\":[$keysJson], \"duration\":\"w\"$ghostJson}"
}

/**
 * Converts a MIDI pitch value to the VexFlow string format (e.g., "c#/4").
 *
 * MIDI pitches are integers from 0 to 127. This function maps a given pitch
 * to its corresponding note name (c, c#, d, etc.) and octave number. The
 * result is a string formatted as "note/octave", which is the standard
 * representation for keys in VexFlow.
 *
 * For example, MIDI pitch 60 (Middle C) is converted to "c/4".
 *
 * @return The VexFlow key string, or `null` if the pitch is outside the valid MIDI range (0-127).
 */
private fun Note.pitchToVexflow(): String? {
    if (pitch !in 0..127) {
        Timber.w("Invalid MIDI pitch value: $pitch.")
        return null
    }
    val noteNames = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
    val octave = (pitch / 12) - 1
    val noteName = noteNames[pitch % 12]
    return "$noteName/$octave"
}
