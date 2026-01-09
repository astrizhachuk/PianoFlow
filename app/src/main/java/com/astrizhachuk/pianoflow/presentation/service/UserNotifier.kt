package com.astrizhachuk.pianoflow.presentation.service

import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс определяет контракт для компонентов, которые могут отправлять
 * и получать сообщения для отображения их пользователю (например, в виде Toast или Snackbar).
 * Он используется для слабой связности между компонентами, которые генерируют
 * сообщения (например, ViewModel), и компонентами,
 * которые их отображают (например, Activity/Fragment).
 */
interface UserNotifier {
    /**
     * Асинхронный поток [UserMessage], на который могут подписаться UI-компоненты
     * для получения и отображения сообщений.
     */
    val userMessages: Flow<UserMessage>

    /**
     * Отправляет сообщение для отображения пользователю.
     *
     * @param message Объект сообщения, которое необходимо показать.
     */
    fun showMessage(message: UserMessage)
}
