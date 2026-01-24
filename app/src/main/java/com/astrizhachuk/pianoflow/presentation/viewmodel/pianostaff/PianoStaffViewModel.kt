
package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.trace
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

@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    observeMidiMessagesUseCase: ObserveMidiMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        trace("PianoStaffViewModel.init") {
            Timber.i("init: ViewModel created.")
            viewModelScope.launch {
                observeMidiMessagesUseCase().collect { notes ->
                    _uiState.update {
                        val newNotesJson = notes.toVexflowJson()
                        Timber.d("collect: Updating UI state - Notes JSON: $newNotesJson")
                        it.copy(notesJson = newNotesJson)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("onCleared: ViewModel destroyed.")
    }
}
