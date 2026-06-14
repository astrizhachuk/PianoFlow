package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.service.analysis.ChordAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of [ChordAnalysisRepository] that coordinates chord analysis using a [ChordAnalyzer].
 *
 * This repository manages the state of the current chord analysis result and provides
 * a method to trigger new analyses based on a list of active notes.
 *
 * @property chordAnalyzer The service used to identify chords from a set of note names.
 */
class ChordAnalysisRepositoryImpl @Inject constructor(
    private val chordAnalyzer: ChordAnalyzer
) : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult.asStateFlow()

    /**
     * Analyzes a list of [Note] objects to identify the corresponding chord.
     *
     * This function orders notes by pitch (ascending, so the lowest note is the bass),
     * extracts unique note names, and uses the [chordAnalyzer] to determine the chord name.
     * The result is published to [chordAnalysisResult].
     * If the input list is empty or an error occurs during analysis, the result is set to null.
     *
     * @param notes The list of notes to be analyzed.
     */
    override fun analyzeChord(notes: List<Note>) {
        try {
            Timber.d("analyzeChord: ${notes.size} notes")

            if (notes.isEmpty()) {
                _chordAnalysisResult.value = null
                return
            }

            val noteNames = notes.sortedBy { it.pitch }.map { it.name }.distinct()
            _chordAnalysisResult.value = chordAnalyzer.analyze(noteNames)
        } catch (e: Exception) {
            Timber.e(e, "analyzeChord: failed")
            _chordAnalysisResult.value = null
        }
    }
}
