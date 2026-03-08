package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain model representing the result of chord analysis.
 *
 * This value object contains the processed result of analyzing a chord,
 * maintaining type safety and clarity about what the result represents.
 *
 * @param chordName The name of the identified chord (e.g., "C", "Am", "Cmaj7"), or null if:
 *                  - A single note was analyzed (not a chord)
 *                  - Chord analysis failed or returned undefined
 * @param wasUndefined Boolean flag indicating whether the chord was undefined
 *                     (multiple notes analyzed but chord not recognized)
 */
data class ChordAnalysisResult(
    val chordName: String?,
    val wasUndefined: Boolean = false
) {
    companion object {
        /**
         * Creates an undefined chord result when chord analysis fails.
         */
        fun undefined(chordNotDefined: String): ChordAnalysisResult =
            ChordAnalysisResult(chordName = chordNotDefined, wasUndefined = true)

        /**
         * Creates a result for a single note (not a chord).
         */
        fun singleNote(): ChordAnalysisResult =
            ChordAnalysisResult(chordName = null, wasUndefined = false)

        /**
         * Creates a successful chord analysis result.
         */
        fun identified(chordName: String): ChordAnalysisResult =
            ChordAnalysisResult(chordName = chordName, wasUndefined = false)
    }
}