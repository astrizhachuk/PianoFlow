package com.astrizhachuk.pianoflow.domain.model

/**
 * Represents a connected MIDI device.
 *
 * This data class holds information that identifies a specific MIDI input or output device,
 * such as a digital piano, keyboard, or synthesizer.
 *
 * @property id A unique, system-assigned identifier for the device.
 * @property name The name of the device. For USB devices, this may consist of the manufacturer and product name.
 * @property product The product name reported by the device itself.
 * @property manufacturer The name of the device manufacturer.
 */
data class MidiDevice(val id: Int, val name: String, val product: String, val manufacturer: String)
