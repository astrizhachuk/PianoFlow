package com.astrizhachuk.pianoflow.domain.repository

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
     * Analyzes a chord from a set of musical notes.
     *
     * This function triggers the chord analysis process. It is a "fire-and-forget"
     * operation, meaning it does not return a value directly. Instead, the analysis
     * result is asynchronously emitted through the [chordAnalysisResult] StateFlow.
     *
     * @param notesJson A JSON string representing the notes to be analyzed.
     */
    fun analyzeChord(
        notesJson: String
    )
}
