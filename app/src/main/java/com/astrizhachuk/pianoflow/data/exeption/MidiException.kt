package com.astrizhachuk.pianoflow.data.exeption

/**
 * Базовое исключение для ошибок работы с MIDI.
 */
sealed class MidiException(
    val messageKey: String,
    cause: Throwable? = null
) : Exception(messageKey, cause) {

    /**
     * Устройство недоступно.
     */
    class DeviceUnavailableException(
        messageKey: String = "error_midi_device_unavailable",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)

    /**
     * Нет разрешения на доступ к MIDI.
     */
    class PermissionDeniedException(
        messageKey: String = "error_midi_permission_denied",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)

    /**
     * Ошибка соединения с устройством.
     */
    class ConnectionException(
        messageKey: String = "error_midi_connection_failed",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)

    /**
     * Устройство занято другим приложением.
     */
    class DeviceBusyException(
        messageKey: String = "error_midi_device_busy",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)

    /**
     * MIDI не поддерживается на устройстве.
     */
    class MidiNotSupportedException(
        messageKey: String = "error_midi_not_supported",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)

    /**
     * Неизвестная ошибка.
     */
    class UnknownException(
        messageKey: String = "error_midi_unknown",
        cause: Throwable? = null
    ) : MidiException(messageKey, cause)
}