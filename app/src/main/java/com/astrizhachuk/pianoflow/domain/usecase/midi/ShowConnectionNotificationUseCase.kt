package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.exception.MidiException
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.NotificationMessage
import javax.inject.Inject

/**
 * Use Case для отображения уведомлений о состоянии подключения (UC-002).
 * 
 * Преобразует состояния подключения в понятные сообщения для пользователя.
 */
class ShowConnectionNotificationUseCase @Inject constructor() {
    
    /**
     * Преобразует состояние подключения в сообщение для уведомления.
     * 
     * @param state состояние подключения
     * @return сообщение для уведомления или null, если уведомление не требуется
     */
    operator fun invoke(state: ConnectionState): NotificationMessage? {
        return when (state) {
            is ConnectionState.Connected -> {
                NotificationMessage(
                    message = "MIDI-клавиатура подключена",
                    type = NotificationMessage.NotificationType.SUCCESS
                )
            }
            
            is ConnectionState.Disconnected -> {
                NotificationMessage(
                    message = "MIDI-клавиатура отключена",
                    type = NotificationMessage.NotificationType.INFO
                )
            }
            
            is ConnectionState.Connecting -> {
                // Не показываем уведомление при подключении
                null
            }
            
            is ConnectionState.Error -> {
                val message = getErrorMessage(state.exception)
                NotificationMessage(
                    message = message,
                    type = NotificationMessage.NotificationType.ERROR
                )
            }
        }
    }
    
    /**
     * Получает понятное сообщение об ошибке на основе типа исключения.
     */
    private fun getErrorMessage(exception: MidiException): String {
        return when (exception) {
            is MidiException.DeviceUnavailableException -> exception.message ?: "Устройство недоступно. Проверьте подключение."
            is MidiException.PermissionDeniedException -> exception.message ?: "Нет разрешения на доступ к MIDI-устройству."
            is MidiException.ConnectionException -> exception.message ?: "Ошибка подключения к устройству. Попробуйте переподключить."
            is MidiException.DeviceBusyException -> exception.message ?: "Устройство уже используется другим приложением."
            is MidiException.MidiNotSupportedException -> exception.message ?: "MIDI не поддерживается на данном устройстве."
            is MidiException.UnknownException -> exception.message ?: "Произошла ошибка при подключении к устройству."
        }
    }
}



