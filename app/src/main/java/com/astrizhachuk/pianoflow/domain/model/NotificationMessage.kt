package com.astrizhachuk.pianoflow.domain.model

/**
 * Сообщение для уведомления пользователя.
 */
data class NotificationMessage(
    val message: String,
    val type: NotificationType
) {
    enum class NotificationType {
        SUCCESS,
        ERROR,
        INFO
    }
}



