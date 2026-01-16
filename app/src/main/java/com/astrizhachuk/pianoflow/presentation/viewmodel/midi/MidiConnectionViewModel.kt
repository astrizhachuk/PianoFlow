package com.astrizhachuk.pianoflow.presentation.viewmodel.midi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.usecase.midi.ShowConnectionNotificationUseCase
import com.astrizhachuk.pianoflow.domain.usecase.midi.TrackMidiConnectionUseCase
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel, отвечающая за управление и наблюдение за состоянием подключений MIDI-устройств.
 *
 * Эта ViewModel предоставляет текущий [ConnectionState] в виде [StateFlow] и реагирует
 * на изменения подключения, отображая уведомления для пользователя (например, toast-сообщения или snackbar),
 * чтобы информировать его о статусе подключения (подключено, отключено и т.д.).
 *
 * @param trackMidiConnectionUseCase Use case для отслеживания состояния подключения MIDI.
 * @param showConnectionNotificationUseCase Use case для создания понятных пользователю сообщений на основе состояния подключения.
 * @param userNotifier Утилита для отображения сообщений пользователю.
 */
@HiltViewModel
class MidiConnectionViewModel @Inject constructor(
    trackMidiConnectionUseCase: TrackMidiConnectionUseCase,
    private val showConnectionNotificationUseCase: ShowConnectionNotificationUseCase,
    private val userNotifier: UserNotifier
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = trackMidiConnectionUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.NoDevice
        )

    init {
        observeConnectionState()
    }

    /**
     * Наблюдает за потоком [connectionState] и показывает уведомление пользователю при изменении состояния.
     *
     * Эта функция пропускает начальное значение потока, чтобы избежать показа уведомления
     * сразу после создания ViewModel. При каждом последующем изменении состояния она определяет
     * подходящее сообщение для пользователя через [showConnectionNotificationUseCase] и отображает его
     * с помощью [userNotifier].
     */
    private fun observeConnectionState() {
        connectionState
            .drop(1) // Ignore the initial value to prevent showing a notification on startup
            .onEach { state ->
                val userMessage = showConnectionNotificationUseCase(state)
                userNotifier.sendMessage(userMessage)
            }.launchIn(viewModelScope)
    }
}
