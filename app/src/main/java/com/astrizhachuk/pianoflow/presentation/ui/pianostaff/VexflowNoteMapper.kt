
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

private enum class Staff { TREBLE, BASS }

private data class ProcessedNote(val key: String, val staff: Staff, val isGhost: Boolean = false)

/**
 * Converts a list of played [Note] objects into a JSON string formatted for VexFlow.
 *
 * This function processes a list of MIDI notes and organizes them for display on a grand staff
 * (treble and bass clefs). It follows a two-step process:
 *
 * 1.  **Processing:** Each note is mapped to a `ProcessedNote` object, which contains its VexFlow key,
 *      its primary staff (treble or bass), and whether it's a candidate for ghosting.
 * 2.  **JSON Generation:** The list of `ProcessedNote` objects is then used to build the final JSON.
 *      For each staff, we filter the list to find its primary and ghost notes and assemble them
 *      into the required JSON structure.
 *
 * The final JSON structure looks like this:
 * ```json
 * {
 *   "treble": [{"keys":["c/5"], "duration":"w"}, {"keys":["g/4"], "duration":"w", "ghost":true}],
 *   "bass": [{"keys":["g/3"], "duration":"w"}]
 * }
 * ```
 */
fun List<Note>.toVexflowJson(): String {
    Timber.d("toVexflowJson() called with notes: $this")

    if (isEmpty()) {
        return "{\"treble\":[], \"bass\":[]}"
    }

    return flatMap(::toProcessedNotes)
        .let(::buildStaffJson)
        .also { result -> Timber.d("toVexflowJson() result: $result") }
}

/**
 * Converts a single Note to a list of ProcessedNotes (primary and optionally ghost).
 */
private fun toProcessedNotes(note: Note): List<ProcessedNote> {
    return note.pitchToVexflow()?.let { key ->
        val primaryStaff = if (note.pitch >= 60) Staff.TREBLE else Staff.BASS
        val ghostStaff = if (primaryStaff == Staff.TREBLE) Staff.BASS else Staff.TREBLE

        listOfNotNull(
            ProcessedNote(key, primaryStaff),
            ProcessedNote(key, ghostStaff, isGhost = true).takeIf { note.pitch in GHOST_NOTE_RANGE }
        )
    } ?: emptyList()
}

/**
 * Builds the final JSON structure from processed notes.
 */
private fun buildStaffJson(processedNotes: List<ProcessedNote>): String {
    return Staff.entries.associate { staff ->
        staff.name.lowercase() to buildStaffContent(processedNotes, staff)
    }.entries.joinToString(", ", "{", "}") { (staffName, json) ->
        "\"$staffName\":$json"
    }
}

/**
 * Builds the JSON content (primary and ghost notes) for a specific staff.
 */
private fun buildStaffContent(processedNotes: List<ProcessedNote>, staff: Staff): String {
    return processedNotes
        .filter { it.staff == staff }
        .groupBy { it.isGhost }
        .let { notesByGhost ->
            listOfNotNull(
                notesByGhost[false]?.map { it.key }?.let { createVexflowNoteJson(it, isGhost = false) },
                notesByGhost[true]?.map { it.key }?.let { createVexflowNoteJson(it, isGhost = true) }
            ).joinToString(",", "[", "]")
        }
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
    val keysJson = keys.joinToString(", ") { "\"$it\"" }
    val ghostAttr = if (isGhost) ", \"ghost\":true" else ""
    return "{\"keys\":[$keysJson], \"duration\":\"w\"$ghostAttr}"
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
