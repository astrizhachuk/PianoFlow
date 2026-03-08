package com.astrizhachuk.pianoflow.domain.usecase.analysis

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for analyzing a chord from a set of musical notes.
 *
 * This class acts as an intermediary between the UI/ViewModel layer and the data layer for
 * chord analysis. It takes a set of notes and delegates the analysis task to the
 * [ChordAnalysisRepository]. The result of the analysis is not returned directly by the `invoke`
 * method; instead, it's exposed through a reactive stream (e.g., a `StateFlow`) in the repository,
 * allowing observers to be notified of the analysis result.
 *
 * @property chordAnalysisRepository The repository that performs the actual chord analysis.
 */
class AnalyzeChordUseCase @Inject constructor(
    private val chordAnalysisRepository: ChordAnalysisRepository
) {

    /**
     * Triggers the analysis of a chord from a set of musical notes.
     *
     * This is a fire-and-forget operation. The result of the analysis will be
     * emitted through the `ChordAnalysisRepository`'s StateFlow.
     *
     * @param notes A list of [Note] objects representing the notes to be analyzed.
     */
    operator fun invoke(
        notes: List<Note>
    ) {
        try {
            Timber.d("invoke: Starting chord analysis")
            chordAnalysisRepository.analyzeChord(notes)
        } catch (e: Exception) {
            Timber.e(e, "invoke: Failed to analyze chord")
        }
    }
}
