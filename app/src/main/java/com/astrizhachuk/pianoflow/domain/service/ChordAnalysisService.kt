package com.astrizhachuk.pianoflow.domain.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import javax.inject.Inject

/**
 * Domain service responsible for parsing musical notes from JSON data and processing 
 * the results of chord analysis.
 *
 * This service acts as a bridge between the raw musical data and the domain 
 * representation required for chord identification.
 *
 * Independent of Android Framework (Pure Kotlin).
 */
class ChordAnalysisService @Inject constructor() {

    /**
     * Parses a JSON string containing notes and returns a list of formatted note names.
     *
     * @param notesJson A JSON string with structure: {"treble":[{"keys":["c/5"]}], "bass":[{"keys":["e/3"]}]}
     * @return List of unique note names (e.g., ["C5", "E3"])
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
     * @return Standardized chord name (e.g., "Am", "C"), or null if not defined
     */
    fun processChordAnalysisResult(
        rawChord: String?
    ): String? {
        val cleanedChord = rawChord
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "null" }

        return when {
            cleanedChord?.endsWith("M") == true -> cleanedChord.removeSuffix("M")
            else -> cleanedChord
        }
    }

    private fun formatNoteForTonal(noteName: String): String {
        val parts = noteName.split('/')
        if (parts.size != 2) return noteName

        val (note, octave) = parts
        return note.replaceFirstChar { it.uppercase(Locale.ROOT) } + octave
    }
}

private data class NotesRoot(
    val treble: List<NoteElement> = emptyList(),
    val bass: List<NoteElement> = emptyList()
)

private data class NoteElement(
    val keys: List<String> = emptyList()
)
