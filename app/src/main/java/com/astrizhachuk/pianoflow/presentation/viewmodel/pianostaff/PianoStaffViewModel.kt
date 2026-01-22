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
        Timber.i("init: ViewModel created.")
        viewModelScope.launch {
            observeMidiMessagesUseCase().collect { notes ->
                Timber.d("collect: Received ${notes.size} notes.")
                val (bassNotes, trebleNotes) = notes.partition { it.pitch < 60 }
                Timber.d("collect: Partitioned notes - Bass: ${bassNotes.size}, Treble: ${trebleNotes.size}")

                _uiState.update {
                    val newTrebleJson = trebleNotes.toVexflowJson()
                    val newBassJson = bassNotes.toVexflowJson()
                    Timber.d("collect: Updating UI state - Treble JSON: $newTrebleJson, Bass JSON: $newBassJson")
                    it.copy(
                        trebleNotesJson = newTrebleJson,
                        bassNotesJson = newBassJson
                    )
                }
            }
        }
    }

    /**
     * Вызывается, когда ViewModel больше не используется и будет уничтожена.
     *
     * Этот метод переопределен для добавления логирования в целях отладки. Он фиксирует
     * момент уничтожения ViewModel, что помогает отслеживать ее жизненный цикл и выявлять
     * возможные утечки памяти или некорректное поведение.
     * Все корутины, запущенные в `viewModelScope`, автоматически отменяются при вызове этого метода.
     */
    override fun onCleared() {
        super.onCleared()
        Timber.i("onCleared: ViewModel destroyed.")
    }
}
