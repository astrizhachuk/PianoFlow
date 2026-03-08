package com.astrizhachuk.pianoflow.domain.service

import javax.inject.Inject

/**
 * Domain service responsible for processing the results of chord analysis.
 *
 * This service acts as a bridge between the raw musical data and the domain 
 * representation required for chord identification.
 *
 * Independent of Android Framework (Pure Kotlin).
 */
class ChordAnalysisService @Inject constructor() {

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
}
