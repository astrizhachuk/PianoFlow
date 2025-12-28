package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case для отслеживания подключения MIDI-клавиатуры (UC-001).
 * 
 * Автоматически отслеживает подключение и отключение MIDI-устройств,
 * подключается к первому найденному устройству.
 */
class TrackMidiConnectionUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    
    /**
     * Начать отслеживание подключений.
     * Возвращает Flow с изменениями состояния подключения.
     */
    operator fun invoke(): Flow<ConnectionState> {
        return midiRepository.observeConnectionState()
    }
    
    /**
     * Инициализировать отслеживание при запуске приложения.
     * Проверяет наличие уже подключенных устройств.
     */
    suspend fun initialize() {
        val currentState = midiRepository.getCurrentConnectionState()
        if (currentState is ConnectionState.Disconnected) {
            // Пытаемся подключиться к первому доступному устройству
            val devices = midiRepository.getAvailableDevices()
            if (devices.isNotEmpty()) {
                midiRepository.connectToDevice(devices.first().id)
            }
        }
    }
}



