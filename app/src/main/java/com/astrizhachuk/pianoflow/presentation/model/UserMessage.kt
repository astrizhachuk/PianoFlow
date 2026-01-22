package com.astrizhachuk.pianoflow.presentation.model

/**
 * Модель данных для сообщения, отображаемого пользователю.
 *
 * Этот класс используется для передачи информации о сообщении между компонентами,
 * например, от ViewModel к UI-слою через [com.astrizhachuk.pianoflow.presentation.service.UserNotifier].
 *
 * @property text Текстовое содержимое сообщения, которое будет показано пользователю.
 */
data class UserMessage(val text: String)
