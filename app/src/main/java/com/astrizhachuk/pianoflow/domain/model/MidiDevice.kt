package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain модель MIDI-устройства.
 * Не зависит от Android-специфичных классов.
 */
data class MidiDevice(
    val id: Int,
    val name: String,
    val manufacturer: String? = null,
    val isInput: Boolean = true,
    val isOutput: Boolean = false
)



