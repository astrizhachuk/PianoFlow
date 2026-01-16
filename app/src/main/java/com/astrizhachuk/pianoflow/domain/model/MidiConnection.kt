package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain модель соединения с MIDI-устройством.
 */
data class MidiConnection(
    val device: MidiDevice,
    val isConnected: Boolean
)



