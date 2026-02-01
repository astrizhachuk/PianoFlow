package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
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
 * This ViewModel is responsible for managing the UI state of the piano staff. It observes MIDI
 * messages, converts them into a format suitable for rendering on a musical staff (for example, VexFlow),
 * and updates the UI accordingly. It also handles the display of the currently detected chord name.
 *
 * @param observeMidiMessagesUseCase The use case for observing incoming MIDI messages and converting
 * them into a list of [Note]s.
 */
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    private val observeMidiMessagesUseCase: ObserveMidiMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        Timber.i("Initializing PianoStaffViewModel and starting to observe MIDI messages.")
        viewModelScope.launch {
            observeMidiMessagesUseCase().collect { notes ->
                Timber.d("Received ${notes.size} notes, updating UI state.")
                _uiState.update {
                    it.copy(notesJson = notes.toVexflowJson())
                }
            }
        }
    }

    /**
     * Updates the name of the chord displayed in the UI.
     *
     * This function is called to change the text representing the currently identified chord.
     * It updates the `chordName` property within the `PianoStaffUiState`.
     *
     * @param name The new name of the chord as a [String].
     */
    fun updateChordName(name: String?) {
        Timber.tag("ChordAnalysis").d("ViewModel updating chord name to: '%s'", name)
        _uiState.update { it.copy(chordName = name) }
    }
}
