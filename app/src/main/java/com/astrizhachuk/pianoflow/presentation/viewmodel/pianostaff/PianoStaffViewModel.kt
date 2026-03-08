package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.AnalyzeChordUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.ObserveChordAnalysisResultsUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

        // Combine MIDI messages and Chord Analysis results to form the final UI state
        combine(
            observeMidiMessagesUseCase(),
            observeChordAnalysisResultsUseCase()
        ) { notes, analysisResult ->
            val notesJson = notes.toVexflowJson()
            
            // Trigger analysis if notes are present
            if (notes.isNotEmpty()) {
                analyzeChordUseCase(notesJson)
            }

            val displayChordName = when {
                analysisResult != null -> analysisResult
                notes.isNotEmpty() -> context.getString(R.string.chord_not_defined)
                else -> null
            }

            _uiState.update { state ->
                state.copy(
                    notesJson = notesJson,
                    chordName = displayChordName
                )
            }
        }.launchIn(viewModelScope)
    }
}
