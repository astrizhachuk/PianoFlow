package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.usecase.analysis.AnalyzeChordUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.ObserveChordAnalysisResultsUseCase
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.octaveLabelResOrNull
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the piano staff screen.
 *
 * This ViewModel manages the UI state by:
 * 1. Observing MIDI messages and converting them to VexFlow JSON format
 * 2. Triggering chord analysis when notes change
 * 3. Computing the displayable chord name based on analysis results and active notes
 *
 * @param context The application context to resolve string resources
 * @param observeMidiMessagesUseCase The use case for observing incoming MIDI messages
 * @param analyzeChordUseCase The use case for triggering chord analysis
 * @param observeChordAnalysisResultsUseCase The use case for observing chord analysis results
 */
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeMidiMessagesUseCase: ObserveMidiMessagesUseCase,
    private val analyzeChordUseCase: AnalyzeChordUseCase,
    private val observeChordAnalysisResultsUseCase: ObserveChordAnalysisResultsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        Timber.i("Initializing PianoStaffViewModel and starting to observe MIDI messages and chord analysis.")

        observeMidiMessagesUseCase()
            .distinctUntilChanged()
            .onEach { notes ->
                // Trigger analysis only when the notes themselves have changed.
                // This occurs before merging with the analysis results,
                // so updating the analysis result won't cause a circular update.
                if (notes.isNotEmpty()) {
                    analyzeChordUseCase(notes)
                }
            }
            // Analysis is currently synchronous (ChordAnalyzer), so the trigger/observe split
            // through a shared StateFlow is a leftover from the earlier design, when note display
            // and analysis were two independent asynchronous components combined here.
            // It is kept as a seam in case analysis becomes asynchronous again (background work,
            // network/ML recognizer, debounce). If that need does not materialize, this can be
            // simplified to a single use case returning the result and a plain map instead of combine.
            .combine(observeChordAnalysisResultsUseCase()) { notes, analysisResult ->
                val notesJson = notes.toVexflowJson()

                val displayChordName = when {
                    analysisResult != null -> analysisResult
                    notes.isNotEmpty() -> context.getString(R.string.chord_not_defined)
                    else -> null
                }

                val octaveName = notes.singleOrNull()
                    ?.let { octaveLabelResOrNull(it.pitch) }
                    ?.let { context.getString(it) }

                PianoStaffUiState(
                    notesJson = notesJson,
                    chordName = displayChordName,
                    octaveName = octaveName
                )
            }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }
}
