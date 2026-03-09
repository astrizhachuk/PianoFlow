package com.astrizhachuk.pianoflow.domain.usecase.analysis

import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for observing the results of chord analysis.
 *
 * This use case provides a reactive stream of chord names (or null) emitted by the
 * [ChordAnalysisRepository].
 */
class ObserveChordAnalysisResultsUseCase @Inject constructor(
    private val chordAnalysisRepository: ChordAnalysisRepository
) {
    /**
     * Returns a [StateFlow] that emits the latest chord analysis result.
     */
    operator fun invoke(): StateFlow<String?> = chordAnalysisRepository.chordAnalysisResult
}
