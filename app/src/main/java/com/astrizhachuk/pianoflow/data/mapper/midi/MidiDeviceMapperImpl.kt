package com.astrizhachuk.pianoflow.data.mapper.midi

import android.media.midi.MidiDeviceInfo
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import javax.inject.Inject

/**
 * Implementation of the [MidiDeviceMapper] interface, responsible for converting data
 * from the data layer to the domain layer.
 *
 * The main task of this class is to convert the system model [MidiDeviceInfo],
 * specific to the Android MIDI API, into a clean domain model [MidiDevice],
 * which does not depend on any frameworks.
 */
class MidiDeviceMapperImpl @Inject constructor() : MidiDeviceMapper {

    /**
     * Converts a [MidiDeviceInfo] object from the Android MIDI API to the domain model [MidiDevice].
     *
     * Extracts key properties from the `Bundle` contained in [MidiDeviceInfo]:
     * - Device name from [MidiDeviceInfo.PROPERTY_NAME]. For USB devices: manufacturer name + product name.
     * - Manufacturer name from [MidiDeviceInfo.PROPERTY_MANUFACTURER].
     * - Product name from [MidiDeviceInfo.PROPERTY_PRODUCT].
     *
     * If any of these properties are missing or `null`, the method substitutes
     * an empty string `""` as the default value, ensuring the stability
     * and predictability of the domain model.
     *
     * @param deviceInfo System object with information about the MIDI device.
     * @return A [MidiDevice] domain model object, ready for use in business logic.
     */
    override fun toDomain(deviceInfo: MidiDeviceInfo): MidiDevice {
        val properties = deviceInfo.properties
        val name = properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        val manufacturer = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
        val product = properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)

        return MidiDevice(
            id = deviceInfo.id,
            name = name ?: "",
            manufacturer = manufacturer ?: "",
            product = product ?: ""
        )
    }
}
