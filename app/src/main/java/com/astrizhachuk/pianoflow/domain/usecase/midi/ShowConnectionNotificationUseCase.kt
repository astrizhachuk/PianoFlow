package com.astrizhachuk.pianoflow.domain.usecase.midi

import android.content.Context
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * A use case that creates a user-facing message based on the MIDI device's connection state.
 *
 * This class translates a [ConnectionState] into a human-readable [UserMessage],
 * encapsulating the logic for which text to display for each specific connection status
 * (e.g., connected, disconnected, or error).
 *
 * @property context The application context, used to resolve string resources.
 */
class ShowConnectionNotificationUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(state: ConnectionState): UserMessage {
        val messageText = when (state) {
            is ConnectionState.Connected -> context.getString(R.string.midi_device_connected, state.device.name)
            is ConnectionState.Disconnected, is ConnectionState.NoDevice -> context.getString(R.string.midi_device_disconnected)
            is ConnectionState.Error -> state.message
        }
        return UserMessage(text = messageText)
    }
}
