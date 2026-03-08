package com.astrizhachuk.pianoflow.data.repository

import android.os.Handler
import android.os.Looper
import com.astrizhachuk.pianoflow.data.datasource.analysis.MusicScriptEngine
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.service.ChordAnalysisService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

private const val JS_ANALYZE = "analyze"

/**
 * Implementation of the ChordAnalysisRepository interface.
 *
 * This class orchestrates chord analysis using:
 * - ChordAnalysisService for business logic (parsing, processing)
 * - MusicScriptEngine for JavaScript execution (no UI involvement)
 *
 * Results are emitted through StateFlow, eliminating callback chains.
 */
class ChordAnalysisRepositoryImpl @Inject constructor(
    private val chordAnalysisService: ChordAnalysisService,
    private val javaScriptExecutor: MusicScriptEngine
) : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult
    
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Analyzes a chord from a JSON string containing notes.
     *
     * No UI callbacks needed - JavaScript execution is handled internally.
     * Results are emitted through [chordAnalysisResult] StateFlow.
     *
     * @param notesJson A JSON string with "treble" and "bass" keys containing notes.
     */
    override fun analyzeChord(
        notesJson: String
    ) {
        try {
            Timber.d("Starting chord analysis with notesJson: %s", notesJson)

            // Parse notes using Domain service
            val notes = chordAnalysisService.parseNotesFromJson(notesJson)

            if (notes.isEmpty()) {
                _chordAnalysisResult.value = null
                return
            }

            // Build JavaScript command (unified analysis)
            val script = buildAnalysisScript(notes)
            
            // Execute JavaScript using Data layer executor (no UI involvement)
            javaScriptExecutor.execute(script) { rawResult ->
                // Process result using Domain service
                val finalResult = chordAnalysisService.processChordAnalysisResult(rawResult)
                
                // Post to main thread to ensure StateFlow update is processed on UI thread
                mainHandler.post {
                    _chordAnalysisResult.value = finalResult
                }
            }
        } catch (e: Exception) {
            Timber.tag("ChordAnalysis").e(e, "Failed to analyze chord")
            _chordAnalysisResult.value = null
        }
    }

    /**
     * Builds the JavaScript code for chord analysis.
     *
     * @param notes List of note names (e.g., ["C4", "E4", "G4"] or ["F#3"])
     * @return JavaScript code string to execute
     */
    private fun buildAnalysisScript(notes: List<String>): String {
        val notesJsArray = notes.joinToString(prefix = "['", separator = "','", postfix = "']")
        return "$JS_ANALYZE($notesJsArray)"
    }
}
