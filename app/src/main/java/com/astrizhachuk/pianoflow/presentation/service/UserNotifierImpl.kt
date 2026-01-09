package com.astrizhachuk.pianoflow.presentation.service

import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [UserNotifier], использующая [MutableSharedFlow] для создания
 * шины событий для отправки и получения сообщений.
 *
 * Класс помечен как [@Singleton], чтобы гарантировать существование единственного
 * экземпляра в течение всего жизненного цикла приложения. Это обеспечивает, что все
 * компоненты общаются через один и тот же канал сообщений.
 */
@Singleton
class UserNotifierImpl @Inject constructor() : UserNotifier {

    /**
     * Приватный изменяемый поток, который используется для отправки сообщений.
     * Это "служебное свойство" (backing property), которое скрывает детали реализации.
     *
     * - `replay = 0`: Новые подписчики не получают предыдущие сообщения.
     * - `extraBufferCapacity = 1`: Хранит одно сообщение, если подписчик не успевает его обработать.
     * - `onBufferOverflow = BufferOverflow.DROP_OLDEST`: Отбрасывает самое старое сообщение
     * при переполнении буфера.
     */
    private val _userMessages = MutableSharedFlow<UserMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Неизменяемый [Flow], который предоставляется внешним подписчикам.
     *
     * Использование [asSharedFlow] обеспечивает инкапсуляцию, позволяя внешним
     * классам только подписываться на сообщения, но не отправлять их.
     */
    override val userMessages: Flow<UserMessage> = _userMessages.asSharedFlow()

    /**
     * Отправляет сообщение для отображения пользователю.
     *
     * Внутри использует [tryEmit] для неблокирующей отправки сообщения в приватный
     * поток [_userMessages]. Сообщение будет доставлено всем активным подписчикам.
     */
    override fun showMessage(message: UserMessage) {
        _userMessages.tryEmit(message)
    }
}
