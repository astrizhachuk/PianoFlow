
package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel для экрана нотного стана.
 *
 * Эта ViewModel отвечает за получение MIDI-сообщений (в виде списка нот) от `ObserveMidiMessagesUseCase`,
 * их преобразование в JSON-формат для VexFlow и предоставление этого JSON в виде [PianoStaffUiState]
 * для отрисовки на экране.
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
}
