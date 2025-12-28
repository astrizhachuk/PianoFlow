package com.astrizhachuk.pianoflow.data.datasource.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.astrizhachuk.pianoflow.domain.exception.MidiException
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Source для работы с Android MIDI API.
 * Инкапсулирует работу с MidiManager и MidiDevice.
 */
@Singleton
class MidiDataSource @Inject constructor(
    private val midiManager: MidiManager?
) {
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var currentDevice: MidiDevice? = null
    private var currentDeviceInfo: MidiDeviceInfo? = null
    
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            // Автоматически подключаемся к первому найденному устройству
            if (_connectionState.value is ConnectionState.Disconnected) {
                connectToFirstAvailableDevice()
            }
        }
        
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            // Если отключилось текущее устройство, обновляем состояние
            if (currentDeviceInfo?.id == device.id) {
                disconnect()
            }
        }
    }
    
    init {
        if (midiManager == null) {
            _connectionState.value = ConnectionState.Error(
                MidiException.MidiNotSupportedException()
            )
        } else {
            // Регистрируем callback для отслеживания изменений устройств
            midiManager.registerDeviceCallback(deviceCallback, null)
            
            // Проверяем наличие уже подключенных устройств при инициализации
            checkForConnectedDevices()
        }
    }
    
    /**
     * Проверить наличие уже подключенных устройств.
     */
    private fun checkForConnectedDevices() {
        val devices = getAvailableDevices()
        if (devices.isNotEmpty() && _connectionState.value is ConnectionState.Disconnected) {
            // Автоматически подключаемся к первому устройству
            connectToFirstAvailableDevice()
        }
    }
    
    /**
     * Подключиться к первому доступному устройству.
     */
    private fun connectToFirstAvailableDevice() {
        val devices = getAvailableDevices()
        if (devices.isNotEmpty()) {
            connectToDevice(devices.first().id) {}
        }
    }
    
    /**
     * Получить список доступных MIDI-устройств.
     */
    fun getAvailableDevices(): List<MidiDeviceInfo> {
        if (midiManager == null) {
            return emptyList()
        }
        return try {
            midiManager.devices.toList()
        } catch (e: Exception) {
            handleException(e)
            emptyList()
        }
    }
    
    /**
     * Подключиться к устройству по ID.
     */
    fun connectToDevice(deviceId: Int, callback: (Result<MidiDeviceInfo>) -> Unit) {
        if (midiManager == null) {
            val exception = MidiException.MidiNotSupportedException()
            _connectionState.value = ConnectionState.Error(exception)
            callback(Result.failure(exception))
            return
        }
        
        try {
            val deviceInfo = midiManager.devices.find { it.id == deviceId }
                ?: run {
                    callback(Result.failure(MidiException.DeviceUnavailableException()))
                    return
                }
            
            _connectionState.value = ConnectionState.Connecting
            
            midiManager.openDevice(deviceInfo, { device ->
                if (device == null) {
                    _connectionState.value = ConnectionState.Error(
                        MidiException.ConnectionException()
                    )
                    callback(Result.failure(MidiException.ConnectionException()))
                    return@openDevice
                }
                
                currentDevice = device
                currentDeviceInfo = deviceInfo
                _connectionState.value = ConnectionState.Connected(
                    deviceInfo.toDomainModel()
                )
                callback(Result.success(deviceInfo))
            }, null)
            
        } catch (e: SecurityException) {
            val exception = MidiException.PermissionDeniedException(cause = e)
            _connectionState.value = ConnectionState.Error(exception)
            callback(Result.failure(exception))
        } catch (e: Exception) {
            val exception = handleException(e)
            _connectionState.value = ConnectionState.Error(exception)
            callback(Result.failure(exception))
        }
    }
    
    /**
     * Отключиться от текущего устройства.
     */
    fun disconnect() {
        currentDevice?.close()
        currentDevice = null
        currentDeviceInfo = null
        _connectionState.value = ConnectionState.Disconnected
    }
    
    /**
     * Освобождение ресурсов.
     */
    fun cleanup() {
        midiManager?.unregisterDeviceCallback(deviceCallback)
        disconnect()
    }
    
    /**
     * Получить текущее состояние подключения.
     */
    fun getCurrentConnectionState(): ConnectionState {
        return _connectionState.value
    }
    
    /**
     * Обработка исключений и преобразование в Domain исключения.
     */
    private fun handleException(e: Exception): MidiException {
        return when (e) {
            is SecurityException -> MidiException.PermissionDeniedException(cause = e)
            is UnsupportedOperationException -> MidiException.MidiNotSupportedException(cause = e)
            else -> MidiException.UnknownException(cause = e)
        }
    }
    
    /**
     * Расширение для преобразования MidiDeviceInfo в Domain модель.
     */
    private fun MidiDeviceInfo.toDomainModel(): com.astrizhachuk.pianoflow.domain.model.MidiDevice {
        val properties = properties
        val name = properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown Device"
        val manufacturer = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
        val isInput = inputPortCount > 0
        val isOutput = outputPortCount > 0
        
        return com.astrizhachuk.pianoflow.domain.model.MidiDevice(
            id = id,
            name = name,
            manufacturer = manufacturer,
            isInput = isInput,
            isOutput = isOutput
        )
    }
}

