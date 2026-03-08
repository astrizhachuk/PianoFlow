package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.AnalyzeChordUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.ObserveChordAnalysisResultsUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the piano staff screen.
 *
 * This ViewModel manages the UI state by:
 * 1. Observing MIDI messages and converting them to VexFlow JSON format
 * 2. Triggering chord analysis when notes change
 * 3. Automatically updated UI when Repository emits analysis results
 *
 * @param observeMidiMessagesUseCase The use case for observing incoming MIDI messages
 * @param analyzeChordUseCase The use case for triggering chord analysis
 * @param observeChordAnalysisResultsUseCase The service-level use case for observing chord analysis results
 */
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    private val observeMidiMessagesUseCase: ObserveMidiMessagesUseCase,
    private val analyzeChordUseCase: AnalyzeChordUseCase,
    private val observeChordAnalysisResultsUseCase: ObserveChordAnalysisResultsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        Timber.i("Initializing PianoStaffViewModel and starting to observe MIDI messages and chord analysis.")

        // Observe MIDI messages
        viewModelScope.launch {
            observeMidiMessagesUseCase().collect { notes ->
                val notesJson = notes.toVexflowJson()
                _uiState.update {
                    it.copy(notesJson = notesJson)
                }

                // Auto-trigger chord analysis when notes change
                if (notes.isNotEmpty()) {
                    analyzeChordUseCase(notesJson)
                }
            }
        }

        // Observe chord analysis results from repository
        viewModelScope.launch {
            observeChordAnalysisResultsUseCase().collect { result ->
                _uiState.update { state ->
                    state.copy(chordName = result)
                }
            }
        }
    }
}
