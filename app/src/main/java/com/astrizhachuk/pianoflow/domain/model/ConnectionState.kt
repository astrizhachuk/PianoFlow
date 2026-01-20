package com.astrizhachuk.pianoflow.domain.model

/**
 * Представляет различные состояния подключения MIDI-устройства.
 *
 * Этот запечатанный интерфейс используется для моделирования различных
 * возможных исходов при попытке подключения или взаимодействия с MIDI-устройством.
 */
sealed interface ConnectionState {
    /**
     * Указывает на успешное подключение к MIDI-устройству.
     *
     * @property device Информация о подключенном устройстве.
     */
    data class Connected(val device: MidiDevice) : ConnectionState

    /**
     * Указывает, что MIDI-устройство было отключено.
     *
     * Это состояние наступает, когда активное соединение было прервано.
     */
    data object Disconnected : ConnectionState

    /**
     * Указывает на возникновение ошибки в процессе подключения или взаимодействия.
     *
     * @property message Сообщение, описывающее ошибку.
     */
    data class Error(val message: String) : ConnectionState

    /**
     * Указывает на отсутствие доступных для подключения MIDI-устройств.
     *
     * Это начальное состояние или состояние, когда все устройства были отключены
     * и новых не найдено.
     */
    data object NoDevice : ConnectionState
}
