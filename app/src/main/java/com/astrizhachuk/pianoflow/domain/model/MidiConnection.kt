package com.astrizhachuk.pianoflow.domain.model

/**
 * Представляет состояние подключения к конкретному MIDI-устройству.
 *
 * Этот класс данных моделирует отношение между MIDI-устройством и его текущим
 * статусом подключения в приложении.
 *
 * @property device MIDI-устройство, к которому выполняется подключение.
 * @property isConnected `true`, если соединение активно, в противном случае `false`.
 */
data class MidiConnection(
    val device: MidiDevice,
    val isConnected: Boolean
)
