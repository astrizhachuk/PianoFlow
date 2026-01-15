package com.astrizhachuk.pianoflow.domain.usecase.midi

import android.content.Context
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case, который преобразует состояние подключения [ConnectionState] в сообщение [UserMessage] для пользователя.
 *
 * Инкапсулирует бизнес-логику определения, какой именно текст сообщения должен быть показан
 * для каждого конкретного состояния подключения MIDI-устройства.
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
