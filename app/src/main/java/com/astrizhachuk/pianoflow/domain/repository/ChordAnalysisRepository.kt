package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for analyzing musical chords.
 *
 * This interface defines the contract for chord analysis operations, abstracting the
 * underlying implementation (e.g., a JavaScript engine via a WebView) from the
 * domain layer. It provides a reactive API for obtaining analysis results.
 */
interface ChordAnalysisRepository {

    /**
     * A reactive [StateFlow] that provides the latest chord analysis result.
     *
     * This flow emits a chord name string whenever a new analysis is completed,
     * or `null` if no analysis has been performed yet or has been cleared.
     */
    val chordAnalysisResult: StateFlow<String?>

    /**
     * Analyzes a set of musical notes to identify the corresponding chord.
     *
     * This function triggers the analysis process asynchronously. The result is not
     * returned directly but is instead emitted through the [chordAnalysisResult] flow.
     *
     * @param notes A list of [Note] objects representing the notes to be analyzed.
     */
    fun analyzeChord(
        notes: List<Note>
    )
}
