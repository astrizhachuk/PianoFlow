package com.astrizhachuk.pianoflow.presentation.service

import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class FakeUserNotifier @Inject constructor() : UserNotifier {

    /**
     * Используем replay=1, чтобы Flow хранил последнее отправленное значение.
     * Это делает тест более надежным, устраняя состояние гонки, когда подписчик
     * может активироваться чуть позже, чем было отправлено сообщение.
     */
    private val _messages = MutableSharedFlow<UserMessage>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val messages: Flow<UserMessage> = _messages

    override fun sendMessage(message: UserMessage) {
        // tryEmit никогда не упадет с этой конфигурацией буфера
        _messages.tryEmit(message)
    }
}
