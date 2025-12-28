package com.astrizhachuk.pianoflow.domain.model

import com.astrizhachuk.pianoflow.domain.exception.MidiException

/**
 * Состояние подключения MIDI-устройства.
 */
sealed class ConnectionState {
    /**
     * Устройство не подключено.
     */
    object Disconnected : ConnectionState()
    
    /**
     * Идет процесс подключения.
     */
    object Connecting : ConnectionState()
    
    /**
     * Устройство успешно подключено.
     */
    data class Connected(val device: MidiDevice) : ConnectionState()
    
    /**
     * Ошибка подключения.
     */
    data class Error(val exception: MidiException) : ConnectionState()
}

