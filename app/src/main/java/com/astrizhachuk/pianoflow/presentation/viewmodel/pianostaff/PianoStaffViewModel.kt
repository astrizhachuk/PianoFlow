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
import javax.inject.Inject

/**
 * ViewModel для экрана нотного стана.
 *
 * Эта ViewModel отвечает за получение MIDI-сообщений, их обработку в музыкальные ноты
 * и предоставление состояния для UI, чтобы отображать ноты на нотном стане.
 * Она разделяет ноты на скрипичный и басовый ключи и преобразует их в JSON-формат,
 * подходящий для библиотеки рендеринга, такой как VexFlow.
 *
 * @param observeMidiMessagesUseCase Use case для получения входящих MIDI-сообщений.
 */
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    observeMidiMessagesUseCase: ObserveMidiMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMidiMessagesUseCase().collect { notes ->
                val (bassNotes, trebleNotes) = notes.partition { it.pitch < 60 }

                _uiState.update {
                    it.copy(
                        trebleNotesJson = trebleNotes.toVexflowJson(),
                        bassNotesJson = bassNotes.toVexflowJson()
                    )
                }
            }
        }
    }
}
