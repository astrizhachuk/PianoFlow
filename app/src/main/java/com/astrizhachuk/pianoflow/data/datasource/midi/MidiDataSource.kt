package com.astrizhachuk.pianoflow.data.datasource.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import android.media.midi.MidiDevice as MidiDeviceApi

class MidiDataSource @Inject constructor(
    context: Context,
    private val midiDeviceMapper: MidiDeviceMapper
) {
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private var openedDevice: MidiDeviceApi? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
    val connectionState = _connectionState.asStateFlow()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Timber.i("onDeviceAdded: %s", device.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
            if (_connectionState.value !is ConnectionState.Connected) {
                Timber.d("Current state is not Connected, attempting to open a device.")
                openFirstAvailableDevice()
            }
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Timber.i("onDeviceRemoved: %s", device.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
            if (openedDevice?.info?.id == device.id) {
                Timber.d("The removed device is our current device, closing it.")
                closeDevice()
            }
        }
    }

    init {
        Timber.i("Initializing MidiDataSource.")
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Timber.i("MIDI feature is supported on this device.")
            val handler = Handler(Looper.getMainLooper())
            midiManager?.registerDeviceCallback(deviceCallback, handler)
            openFirstAvailableDevice()
        } else {
            Timber.e("MIDI feature is NOT supported on this device. Check AndroidManifest.xml")
            _connectionState.value = ConnectionState.Error("MIDI API is not supported on this device.")
        }
    }

    private fun openFirstAvailableDevice() {
        Timber.d("openFirstAvailableDevice: looking for devices.")
        val firstDevice = midiManager?.devices?.firstOrNull()
        if (firstDevice != null) {
            Timber.i("Found device: %s, attempting to open.", firstDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
            openDevice(firstDevice)
        } else {
            Timber.i("No MIDI devices found.")
            _connectionState.value = ConnectionState.NoDevice
        }
    }

    private fun openDevice(deviceInfo: MidiDeviceInfo) {
        Timber.i("openDevice: trying to open %s", deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
        if (openedDevice?.info?.id == deviceInfo.id) {
            Timber.d("Device is already open. Skipping.")
            return
        }
        
        closeDevice()

        midiManager?.openDevice(deviceInfo, {
            if (it == null) {
                Timber.e("Failed to open MIDI device.")
                _connectionState.value = ConnectionState.Error("Failed to open MIDI device.")
                return@openDevice
            }
            Timber.i("Successfully opened device. Setting state to Connected.")
            openedDevice = it
            _connectionState.value = ConnectionState.Connected(midiDeviceMapper.toDomain(deviceInfo))
        }, null)
    }

    private fun closeDevice() {
        if (openedDevice != null) {
            Timber.i("closeDevice: Closing current device.")
            openedDevice?.close()
            openedDevice = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun close() {
        Timber.i("close: Unregistering callback and closing device.")
        midiManager?.unregisterDeviceCallback(deviceCallback)
        closeDevice()
    }
}
