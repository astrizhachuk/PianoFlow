package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel для экрана с нотным станом.
 *
 * Эта ViewModel отвечает за получение потока MIDI-нот из `ObserveMidiMessagesUseCase`
 * и его преобразование в состояние UI (`PianoStaffUiState`), которое может быть
 * использовано `Composable`-функцией для отрисовки.
 */
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    observeMidiMessagesUseCase: ObserveMidiMessagesUseCase
) : ViewModel() {

    /**
     * `StateFlow`, который выдает актуальное состояние UI для экрана нотного стана.
     *
     * - `observeMidiMessagesUseCase()`: Запускает use case, который возвращает `Flow<List<Note>>`,
     *   где каждый список представляет собой аккорд или одиночную ноту.
     * - `.map { notes -> PianoStaffUiState(notes = notes) }`: Преобразует каждый список нот
     *   в объект состояния UI.
     * - `.stateIn(...)`: Превращает "холодный" Flow в "горячий" `StateFlow`, который
     *   хранит последнее значение и передает его новым подписчикам. Поток активен, пока
     *   есть подписчики, и останавливается через 5 секунд после исчезновения последнего подписчика,
     *   что экономит ресурсы.
     */
    val uiState: StateFlow<PianoStaffUiState> = observeMidiMessagesUseCase()
        .map { notes -> PianoStaffUiState(notes = notes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = PianoStaffUiState()
        )
}