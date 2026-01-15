package com.astrizhachuk.pianoflow.data.mapper.midi

import android.media.midi.MidiDeviceInfo
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import javax.inject.Inject

/**
 * Реализация интерфейса [MidiDeviceMapper], отвечающая за преобразование данных
 * из слоя данных (Data) в доменный слой (Domain).
 *
 * Основная задача этого класса — преобразовать системную модель [MidiDeviceInfo],
 * специфичную для Android MIDI API, в чистую доменную модель [MidiDevice],
 * которая не зависит от каких-либо фреймворков.
 */
class MidiDeviceMapperImpl @Inject constructor() : MidiDeviceMapper {

    /**
     * Преобразует объект [MidiDeviceInfo] из Android MIDI API в доменную модель [MidiDevice].
     *
     * Извлекает ключевые свойства из `Bundle`, содержащегося в [MidiDeviceInfo]:
     * - Имя устройства из [MidiDeviceInfo.PROPERTY_NAME].
     * - Имя производителя из [MidiDeviceInfo.PROPERTY_MANUFACTURER].
     *
     * Если какое-либо из этих свойств отсутствует или равно `null`, метод подставляет
     * пустую строку `""` в качестве значения по умолчанию, обеспечивая стабильность
     * и предсказуемость доменной модели.
     *
     * @param deviceInfo Системный объект с информацией о MIDI-устройстве.
     * @return Объект доменной модели [MidiDevice], готовый для использования в бизнес-логике.
     */
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
