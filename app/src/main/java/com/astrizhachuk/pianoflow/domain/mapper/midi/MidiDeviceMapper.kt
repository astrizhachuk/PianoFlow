package com.astrizhachuk.pianoflow.domain.mapper.midi

import android.media.midi.MidiDeviceInfo
import com.astrizhachuk.pianoflow.domain.model.MidiDevice

/**
 * Interface for mapping MIDI data models.
 * Defines the contract for converting DTOs from the data layer to domain models.
 */
interface MidiDeviceMapper {
    /**
     * Converts [MidiDeviceInfo] from the data layer to [MidiDevice] of the domain layer.
     */
    fun toDomain(deviceInfo: MidiDeviceInfo): MidiDevice
}
