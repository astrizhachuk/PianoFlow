package com.astrizhachuk.pianoflow.data.mapper.midi

import android.media.midi.MidiDeviceInfo
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import javax.inject.Inject

/**
 * Реализация интерфейса [MidiDeviceMapper].
 * Преобразует [MidiDeviceInfo] из Android MIDI API в доменную модель [MidiDevice].
 */
class MidiDeviceMapperImpl @Inject constructor() : MidiDeviceMapper {

    override fun toDomain(deviceInfo: MidiDeviceInfo): MidiDevice {
        val properties = deviceInfo.properties
        val name = properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        val vendor = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)

        return MidiDevice(
            id = deviceInfo.id,
            name = name ?: "",
            vendor = vendor ?: ""
        )
    }
}
