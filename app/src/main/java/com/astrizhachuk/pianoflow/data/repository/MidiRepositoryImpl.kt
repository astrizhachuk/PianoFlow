package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.exception.MidiException
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Реализация MidiRepository для работы с Android MIDI API.
 */
@Singleton
class MidiRepositoryImpl @Inject constructor(
    private val midiDataSource: MidiDataSource
) : MidiRepository {
    
    override suspend fun getAvailableDevices(): List<MidiDevice> {
        return midiDataSource.getAvailableDevices()
            .map { it.toDomainModel() }
    }
    
    override suspend fun connectToDevice(deviceId: Int?): Result<MidiDevice> {
        return suspendCancellableCoroutine { continuation ->
            val targetDeviceId = if (deviceId != null) {
                deviceId
            } else {
                // Подключаемся к первому доступному устройству
                val devices = midiDataSource.getAvailableDevices()
                if (devices.isEmpty()) {
                    continuation.resume(
                        Result.failure(MidiException.DeviceUnavailableException())
                    )
                    return@suspendCancellableCoroutine
                }
                devices.first().id
            }
            
            midiDataSource.connectToDevice(targetDeviceId) { result ->
                result.fold(
                    onSuccess = { deviceInfo ->
                        val domainDevice = deviceInfo.toDomainModel()
                        continuation.resume(Result.success(domainDevice))
                    },
                    onFailure = { exception ->
                        continuation.resume(Result.failure(exception))
                    }
                )
            }
        }
    }
    
    override suspend fun disconnect() {
        midiDataSource.disconnect()
    }
    
    override fun observeConnectionState(): Flow<ConnectionState> {
        return midiDataSource.connectionState
    }
    
    override suspend fun getCurrentConnectionState(): ConnectionState {
        return midiDataSource.getCurrentConnectionState()
    }
    
    /**
     * Расширение для преобразования MidiDeviceInfo в Domain модель.
     */
    private fun android.media.midi.MidiDeviceInfo.toDomainModel(): MidiDevice {
        val properties = properties
        val name = properties.getString(android.media.midi.MidiDeviceInfo.PROPERTY_NAME) 
            ?: "Unknown Device"
        val manufacturer = properties.getString(android.media.midi.MidiDeviceInfo.PROPERTY_MANUFACTURER)
        val isInput = inputPortCount > 0
        val isOutput = outputPortCount > 0
        
        return MidiDevice(
            id = id,
            name = name,
            manufacturer = manufacturer,
            isInput = isInput,
            isOutput = isOutput
        )
    }
}

