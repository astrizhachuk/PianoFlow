
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.Locale

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
 * Represents the two staves on a grand staff.
 *
 * This enum is used to identify whether a note should be rendered on the
 * treble (upper) or bass (lower) staff.
 */
private enum class Staff { TREBLE, BASS }

/**
 * Represents an intermediate state of a musical note during processing for VexFlow rendering.
 *
 * This class holds the necessary information to place a note on the correct staff and
 * determine if it should be displayed as a primary or "ghost" note. Ghost notes are duplicates
 * shown on an adjacent staff for improved readability, especially for notes near Middle C.
 *
 * @property key The note's representation in VexFlow format (e.g., "c/4").
 * @property staff The musical staff ([Staff.TREBLE] or [Staff.BASS]) this note belongs to.
 * @property isGhost A flag indicating if this is a duplicated "ghost" note. Defaults to `false`.
 */
private data class ProcessedNote(val key: String, val staff: Staff, val isGhost: Boolean = false)

/**
 * Converts a list of played [Note] objects into a JSON string formatted for VexFlow.
 *
 * This extension function processes a list of MIDI notes and organizes them for display on a grand staff
 * (treble and bass clefs). It handles the complexities of placing notes on the correct staff and
 * creating "ghost" notes for improved readability in the ledger line area between the staves.
 *
 * The process involves:
 * 1.  **Note Processing:** Each `Note` is converted into one or more `ProcessedNote` objects. A primary
 *     `ProcessedNote` is always created for the note's natural staff (treble for Middle C and above,
 *     bass otherwise). If the note's pitch falls within the `GHOST_NOTE_RANGE`, a secondary "ghost"
 *     `ProcessedNote` is also created for the adjacent staff.
 * 2.  **JSON Generation:** The collected `ProcessedNote` objects are then grouped by staff (treble/bass)
 *     and ghost status. This grouping is used to build the final JSON structure, where notes played
 *     simultaneously are combined into a single VexFlow chord object.
 *
 * The final JSON structure is an object with `treble` and `bass` keys, each containing an array
 * of VexFlow note objects.
 *
 * Example Output:
 * ```json
 * {
 *   "treble": [{"keys":["c/5"], "duration":"w"}, {"keys":["g/4"], "duration":"w", "ghost":true}],
 *   "bass": [{"keys":["c/4"], "duration":"w"}, {"keys":["e/3", "g/3"], "duration":"w"}]
 * }
 * ```
 *
 * @receiver A list of [Note] objects to be converted.
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
 * Maps a single [Note] to a list of [ProcessedNote]s for rendering on the grand staff.
 *
 * This function determines the primary staff (treble or bass) for a given note and
 * also creates a "ghost" note if the pitch falls within the [GHOST_NOTE_RANGE].
 *
 * - A note with a MIDI pitch of 60 (Middle C) or higher is assigned to the **treble** staff.
 * - A note with a MIDI pitch below 60 is assigned to the **bass** staff.
 * - If the note's pitch is within the [GHOST_NOTE_RANGE] (e.g., Middle C), a second,
 *   "ghost" `ProcessedNote` is created for the opposite staff. This allows the note
 *   to be rendered on both staves, improving readability around the center of the grand staff.
 *
 * @param note The input [Note] to process.
 * @return A list containing the primary [ProcessedNote] and an optional ghost `ProcessedNote`.
 *         Returns an empty list if the note's pitch is invalid.
 */
private fun toProcessedNotes(note: Note): List<ProcessedNote> {
    val key = note.pitchToVexflow() ?: return emptyList()

    val primaryStaff = if (note.pitch >= 60) Staff.TREBLE else Staff.BASS
    val ghostStaff = if (primaryStaff == Staff.TREBLE) Staff.BASS else Staff.TREBLE

    return listOfNotNull(
        ProcessedNote(key, primaryStaff),
        ProcessedNote(key, ghostStaff, isGhost = true).takeIf { note.pitch in GHOST_NOTE_RANGE }
    )
}

/**
 * Constructs the final JSON object containing separate note arrays for the treble and bass staves.
 *
 * This function takes a flat list of all `ProcessedNote` instances (both primary and ghost notes)
 * and organizes them into a JSON structure suitable for VexFlow. It iterates through each `Staff`
 * type (TREBLE, BASS), filters the notes belonging to that staff, and builds the corresponding
 * JSON array for it using `buildStaffContent`.
 *
 * The final output is a single JSON string with two keys, "treble" and "bass", each mapping to
 * an array of VexFlow note objects.
 *
 * Example output:
 * `{"treble":[...notes...], "bass":[...notes...]}`
 *
 * @param processedNotes A list of all notes to be included in the final JSON, including primary
 *   and ghost notes for both staves.
 * @return A JSON string representing the complete grand staff with all its notes.
 */
private fun buildStaffJson(processedNotes: List<ProcessedNote>): String {
    return Staff.entries.associate { staff ->
        staff.name.lowercase() to buildStaffContent(processedNotes, staff)
    }.entries.joinToString(", ", "{", "}") { (staffName, json) ->
        "\"$staffName\":$json"
    }
}

/**
 * Generates the JSON array string for a single staff (treble or bass).
 *
 * This function filters the full list of `ProcessedNote` objects to find those belonging to the
 * specified `staff`. It then groups them by whether they are "ghost" notes or regular notes.
 * Finally, it creates a JSON array containing VexFlow note objects for both the regular and ghost
 * chords on that staff.
 *
 * @param processedNotes The complete list of all notes (primary and ghost) to be considered.
 * @param staff The specific staff (TREBLE or BASS) for which to generate the content.
 * @return A JSON array string, e.g., `[{"keys":["c/5"], "duration":"w"}, {"keys":["g/4"], "duration":"w", "ghost":true}]`.
 *         Returns an empty array string `[]` if no notes belong to the staff.
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
 * Creates a JSON string for a single VexFlow `StaveNote` object.
 *
 * This function takes a list of VexFlow note keys (e.g., `["c/4", "e/4"]`) and constructs a JSON object
 * representing a whole note chord. It can also mark the note as a "ghost" note, which is
 * used for rendering notes that appear on both staves (e.g., Middle C) for readability.
 *
 * The resulting JSON format is `{"keys":["c/4", "e/4"], "duration":"w", "ghost":true}`.
 *
 * @param keys A list of strings, where each string is a VexFlow note representation (e.g., "c/4").
 * @param isGhost A boolean flag indicating if this note should be treated as a "ghost" note.
 * @return A JSON formatted string representing the VexFlow note.
 */
private fun createVexflowNoteJson(keys: List<String>, isGhost: Boolean): String {
    val keysJson = keys.joinToString(", ") { "\"$it\"" }
    val ghostAttr = if (isGhost) ", \"ghost\":true" else ""
    return "{\"keys\":[$keysJson], \"duration\":\"w\"$ghostAttr}"
}

/**
 * Converts the note's MIDI pitch to the VexFlow string format (e.g., "c#/4").
 *
 * This function maps the integer MIDI pitch to its corresponding note name
 * (c, c#, d, etc.) and octave number. The result is a string formatted as
 * "note/octave", which is the standard representation for keys in VexFlow.
 *
 * For example, MIDI pitch 60 (Middle C) is converted to "c/4".
 *
 * @receiver The [Note] instance containing the pitch to convert.
 * @return The VexFlow key string (e.g., "c/4"), or `null` if the pitch is
 *   outside the valid MIDI range (0-127).
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

/**
 * Represents a single VexFlow note or chord object within a staff.
 *
 * This class is used for parsing the VexFlow JSON structure. It holds a list of note keys
 * (e.g., `["c/4", "e/4"]`) that form a single musical event (a note or a chord) on the staff.
 *
 * @property keys A list of note representations in VexFlow format (e.g., "c/4").
 *                An empty list signifies no notes for this item.
 */
private data class NoteItem(val keys: List<String> = emptyList())

/**
 * A data class representing the root structure of parsed VexFlow notes.
 *
 * This class is used as a target for deserializing a JSON string that contains
 * separate lists of notes for the treble and bass staves. It mirrors the top-level
 * structure of the JSON generated by `List<Note>.toVexflowJson()`.
 *
 * @property treble A list of `NoteItem` objects belonging to the treble staff.
 * @property bass A list of `NoteItem` objects belonging to the bass staff.
 */
private data class NotesRoot(val treble: List<NoteItem> = emptyList(), val bass: List<NoteItem> = emptyList())

/**
 * Parses a VexFlow JSON string to extract a unique, sorted list of note names formatted for Tonal.js.
 *
 * This function takes a JSON string, typically generated by `toVexflowJson`, which contains
 * notes organized into "treble" and "bass" staves. It deserializes this JSON, flattens the
 * note lists from both staves, and processes each note key.
 *
 * The key transformations are:
 * 1.  **Extraction:** It extracts all note keys (e.g., "c/4", "f#/5") from the nested structure.
 * 2.  **Uniqueness:** It ensures the final list contains only unique note names.
 * 3.  **Formatting:** It converts VexFlow's "c#/4" format to Tonal.js's "C#4" format by
 *     capitalizing the note letter.
 * 4.  **Sorting:** The final list of note names is sorted alphabetically.
 *
 * This is useful for analyzing the set of unique pitches present in a chord or a collection of notes.
 *
 * @param notesJson A JSON string representing notes on a grand staff, with "treble" and "bass" arrays.
 *                  Example: `{"treble":[{"keys":["c/5"]}], "bass":[{"keys":["e/3", "g/3"]}]}`.
 * @return A distinct, sorted list of note names in Tonal.js format (e.g., `["C5", "E3", "G3"]`).
 *         Returns an empty list if the JSON is empty or malformed.
 */
fun parseNotesForAnalysis(notesJson: String): List<String> {
    val notesType = object : TypeToken<NotesRoot>() {}.type
    val notes = Gson().fromJson<NotesRoot>(notesJson, notesType)
        ?: NotesRoot()

    return (notes.treble + notes.bass)
        .asSequence()
        .flatMap { it.keys }
        .distinct()
        .map(::formatNoteForTonal)
        .sorted()
        .toList()
}

private fun formatNoteForTonal(noteName: String): String {
    val parts = noteName.split('/')
    if (parts.size != 2) return noteName

    val (note, octave) = parts
    return note.replaceFirstChar { it.uppercase(Locale.ROOT) } + octave
}
