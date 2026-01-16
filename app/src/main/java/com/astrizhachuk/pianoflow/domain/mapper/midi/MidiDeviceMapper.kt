package com.astrizhachuk.pianoflow.domain.mapper.midi

import android.media.midi.MidiDeviceInfo
import com.astrizhachuk.pianoflow.domain.model.MidiDevice

/**
 * Интерфейс для маппинга моделей данных MIDI.
 * Определяет контракт для преобразования DTO из слоя данных в доменные модели.
 */
interface MidiDeviceMapper {
    /**
     * Преобразует [MidiDeviceInfo] из слоя данных в [MidiDevice] доменного слоя.
     */
    fun toDomain(deviceInfo: MidiDeviceInfo): MidiDevice
}
