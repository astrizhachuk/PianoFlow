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

    private fun observeConnectionState() {
        connectionState
            .drop(1) // Ignore the initial value to prevent showing a notification on startup
            .onEach { state ->
                val userMessage = showConnectionNotificationUseCase(state)
                userMessage?.let { userNotifier.sendMessage(it) }
            }.launchIn(viewModelScope)
    }
}
