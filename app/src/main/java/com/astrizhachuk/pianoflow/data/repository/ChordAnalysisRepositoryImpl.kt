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
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

private const val JS_ANALYZE = "analyze"

/**
 * Implementation of the [ChordAnalysisRepository] interface.
 *
 * This class coordinates the chord analysis process by leveraging the [MusicScriptEngine]
 * for JavaScript-based computation and the [ChordAnalysisService] for post-processing
 * and business logic.
 *
 * Key features:
 * - Executes analysis logic in a JavaScript environment via [javaScriptExecutor].
 * - Decouples analysis execution from the UI using [StateFlow] for result observation.
 * - Handles thread synchronization to ensure results are emitted on the main thread.
 *
 * @property chordAnalysisService Service for processing raw analysis results into domain-specific data.
 * @property javaScriptExecutor Engine responsible for executing the underlying JavaScript analysis logic.
 * @property gson JSON library used to format parameters for script execution.
 */
class ChordAnalysisRepositoryImpl @Inject constructor(
    private val chordAnalysisService: ChordAnalysisService,
    private val javaScriptExecutor: MusicScriptEngine,
    private val gson: Gson
) : ChordAnalysisRepository {

    private val _chordAnalysisResult = MutableStateFlow<String?>(null)
    override val chordAnalysisResult: StateFlow<String?> = _chordAnalysisResult.asStateFlow()
    
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
            Timber.d("analyzeChord: Initializing analysis with ${notes.size} notes")

            if (notes.isEmpty()) {
                _chordAnalysisResult.value = null
                return
            }

            val noteNames = notes.map { it.name }.distinct().sorted()
            val script = buildAnalysisScript(noteNames)

            javaScriptExecutor.execute(script) { rawResult ->
                val finalResult = chordAnalysisService.processChordAnalysisResult(rawResult)
                mainHandler.post {
                    _chordAnalysisResult.value = finalResult
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "analyzeChord: Failed to analyze chord")
            _chordAnalysisResult.value = null
        }
    }

    /**
     * Builds the JavaScript snippet required to execute the chord analysis in the script engine.
     *
     * Uses [Gson] to safely serialize the list of note names into a JSON array,
     * ensuring compatibility with the JavaScript `analyze` function signature.
     *
     * @param notes A list of note names (e.g., ["C4", "E4", "G#4"]) to be passed to the script.
     * @return A string containing the JavaScript function call with serialized arguments.
     */
    private fun buildAnalysisScript(notes: List<String>): String {
        return "$JS_ANALYZE(${gson.toJson(notes)})"
    }
}
