package com.astrizhachuk.pianoflow.domain.exception

/**
 * Базовое исключение для ошибок работы с MIDI.
 */
sealed class MidiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Устройство недоступно.
     */
    class DeviceUnavailableException(
        message: String = "Устройство недоступно. Проверьте подключение.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
    
    /**
     * Нет разрешения на доступ к MIDI.
     */
    class PermissionDeniedException(
        message: String = "Нет разрешения на доступ к MIDI-устройству.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
    
    /**
     * Ошибка соединения с устройством.
     */
    class ConnectionException(
        message: String = "Ошибка подключения к устройству. Попробуйте переподключить.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
    
    /**
     * Устройство занято другим приложением.
     */
    class DeviceBusyException(
        message: String = "Устройство уже используется другим приложением.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
    
    /**
     * MIDI не поддерживается на устройстве.
     */
    class MidiNotSupportedException(
        message: String = "MIDI не поддерживается на данном устройстве.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
    
    /**
     * Неизвестная ошибка.
     */
    class UnknownException(
        message: String = "Произошла ошибка при подключении к устройству.",
        cause: Throwable? = null
    ) : MidiException(message, cause)
}



