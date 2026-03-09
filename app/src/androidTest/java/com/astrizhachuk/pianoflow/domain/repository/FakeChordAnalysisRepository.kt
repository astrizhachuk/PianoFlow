package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [ChordAnalysisRepository] for UI tests.
 */
class FakeChordAnalysisRepository : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult.asStateFlow()

    override fun analyzeChord(notes: List<Note>) {
        // In UI tests, we might not need real analysis, 
        // but we can simulate it if needed.
    }

    /**
     * Helper method for tests to simulate analysis results.
     */
    fun emitResult(result: String?) {
        _chordAnalysisResult.value = result
    }
}
