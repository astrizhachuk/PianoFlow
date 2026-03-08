package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain model representing input data for chord analysis.
 *
 * This value object encapsulates all the information needed to analyze a chord,
 * keeping the Domain layer independent of Presentation concerns.
 *
 * @param notes List of note names in standardized format (e.g., ["C5", "E3", "G3"])
 * @param isChord Boolean flag indicating if multiple notes form a chord
 */
data class ChordAnalysisInput(
    val notes: List<String>,
    val isChord: Boolean
)