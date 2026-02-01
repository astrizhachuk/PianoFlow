package com.astrizhachuk.pianoflow.presentation.model.pianostaff

/**
 * Represents the UI state for the piano staff screen.
 *
 * @param notesJson A JSON string containing the notes for both the treble and bass clefs.
 *                  It defaults to an empty structure: `{"treble":[], "bass":[]}`.
 * @param chordName The name of the analyzed chord, if any.
 */
data class PianoStaffUiState(
    val notesJson: String = "{\"treble\":[], \"bass\":[]}",
    val chordName: String? = null
)
