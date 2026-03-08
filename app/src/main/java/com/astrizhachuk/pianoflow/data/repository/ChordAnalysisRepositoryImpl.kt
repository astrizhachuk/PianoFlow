package com.astrizhachuk.pianoflow.data.repository

import android.os.Handler
import android.os.Looper
import com.astrizhachuk.pianoflow.data.datasource.analysis.MusicScriptEngine
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.service.ChordAnalysisService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

private const val JS_ANALYZE = "analyze"

/**
 * Implementation of the ChordAnalysisRepository interface.
 *
 * This class orchestrates chord analysis using:
 * - ChordAnalysisService for business logic (processing result)
 * - MusicScriptEngine for JavaScript execution (no UI involvement)
 *
 * Results are emitted through StateFlow, eliminating callback chains.
 */
class ChordAnalysisRepositoryImpl @Inject constructor(
    private val chordAnalysisService: ChordAnalysisService,
    private val javaScriptExecutor: MusicScriptEngine,
    private val gson: Gson
) : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult
    
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Analyzes a chord from a list of [Note] objects.
     *
     * No UI callbacks needed - JavaScript execution is handled internally.
     * Results are emitted through [chordAnalysisResult] StateFlow.
     *
     * @param notes A list of [Note] objects to be analyzed.
     */
    override fun analyzeChord(
        notes: List<Note>
    ) {
        try {
            Timber.d("Starting chord analysis with ${notes.size} notes")

            if (notes.isEmpty()) {
                _chordAnalysisResult.value = null
                return
            }

            val noteNames = notes.map { it.name }.distinct().sorted()
            val script = buildAnalysisScript(noteNames)
            
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
     * Builds the JavaScript code for chord analysis using Gson for safe array formatting.
     *
     * @param notes List of note names (e.g., ["C4", "E4", "F#4"])
     * @return JavaScript code string to execute
     */
    private fun buildAnalysisScript(notes: List<String>): String {
        return "$JS_ANALYZE(${gson.toJson(notes)})"
    }
}
