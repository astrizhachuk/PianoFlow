package com.astrizhachuk.pianoflow.domain.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import javax.inject.Inject

/**
 * Domain service for parsing notes from JSON and processing chord analysis results.
 */
class ChordAnalysisService @Inject constructor() {

    /**
     * Parses a JSON string containing notes and returns a list of formatted note names.
     *
     * @param notesJson A JSON string with structure: {"treble":[{"keys":["c/5"]}], "bass":[{"keys":["e/3"]}]}
     * @return List of unique note names formatted for Tonal (e.g., ["C5", "E3"])
     */
    fun parseNotesFromJson(notesJson: String): List<String> {
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

    /**
     * Processes the raw result from JavaScript chord analysis into a clean chord name.
     *
     * @param rawChord Raw string returned from JavaScript execution
     * @param isChord Flag indicating if multiple notes were analyzed
     * @param chordNotDefined String to return if chord is not identified
     * @return Standardized chord name, or null for single notes/failed analysis
     */
    fun processChordAnalysisResult(
        rawChord: String?,
        isChord: Boolean,
        chordNotDefined: String = "Chord not defined"
    ): String? {
        val cleanedChord = rawChord
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "null" }

        return when {
            cleanedChord?.endsWith("M") == true -> cleanedChord.removeSuffix("M")
            cleanedChord != null -> cleanedChord
            isChord -> chordNotDefined
            else -> null
        }
    }

    private fun formatNoteForTonal(noteName: String): String {
        val parts = noteName.split('/')
        if (parts.size != 2) return noteName

        val (note, octave) = parts
        return note.replaceFirstChar { it.uppercase(Locale.ROOT) } + octave
    }
}

internal data class NotesRoot(
    val treble: List<NoteElement> = emptyList(),
    val bass: List<NoteElement> = emptyList()
)

internal data class NoteElement(
    val keys: List<String> = emptyList()
)
