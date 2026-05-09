package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.service.analysis.ChordAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

class ChordAnalysisRepositoryImpl @Inject constructor(
    private val chordAnalyzer: ChordAnalyzer
) : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult.asStateFlow()

    override fun analyzeChord(notes: List<Note>) {
        try {
            Timber.d("analyzeChord: ${notes.size} notes")

            if (notes.isEmpty()) {
                _chordAnalysisResult.value = null
                return
            }

            val noteNames = notes.map { it.name }.distinct().sorted()
            _chordAnalysisResult.value = chordAnalyzer.analyze(noteNames)
        } catch (e: Exception) {
            Timber.e(e, "analyzeChord: failed")
            _chordAnalysisResult.value = null
        }
    }
}
