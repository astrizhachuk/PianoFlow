package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с MIDI-устройствами.
 * Определен в Domain-слое, реализация в Data-слое.
 */
interface MidiRepository {
    
    /**
     * Получить список доступных MIDI-устройств.
     */
    suspend fun getAvailableDevices(): List<MidiDevice>
    
    /**
     * Подключиться к устройству по ID.
     * Подключается к первому найденному устройству, если deviceId не указан.
     */
    suspend fun connectToDevice(deviceId: Int? = null): Result<MidiDevice>
    
    /**
     * Отключиться от текущего устройства.
     */
    suspend fun disconnect()
    
    /**
     * Наблюдать за изменениями состояния подключения.
     */
    fun observeConnectionState(): Flow<ConnectionState>
    
    /**
     * Получить текущее состояние подключения.
     */
    suspend fun getCurrentConnectionState(): ConnectionState
}



