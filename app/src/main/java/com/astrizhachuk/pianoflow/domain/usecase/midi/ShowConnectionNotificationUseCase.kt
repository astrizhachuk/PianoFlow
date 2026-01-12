package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import javax.inject.Inject

/**
 * Use case that transforms a [ConnectionState] into a user-facing [UserMessage].
 * This encapsulates the business logic of what message to show for each connection state.
 */
class ShowConnectionNotificationUseCase @Inject constructor() {

    operator fun invoke(state: ConnectionState): UserMessage? {
        val messageText = when (state) {
            is ConnectionState.Connected -> "MIDI device connected: ${state.device.name ?: "Unknown Device"}"
            is ConnectionState.Disconnected, is ConnectionState.NoDevice -> "MIDI device disconnected"
            is ConnectionState.Error -> state.message
        }
        return messageText?.let { UserMessage(text = it) }
    }
}
