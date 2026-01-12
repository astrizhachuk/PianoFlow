package com.astrizhachuk.pianoflow.data.datasource.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import android.media.midi.MidiDevice as MidiDeviceApi

private const val TAG = "MidiDataSource"

class MidiDataSource @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val midiDeviceMapper: MidiDeviceMapper
) {
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private var openedDevice: MidiDeviceApi? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
    val connectionState = _connectionState.asStateFlow()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Log.d(TAG, "onDeviceAdded: ${device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
            if (_connectionState.value !is ConnectionState.Connected) {
                Log.d(TAG, "Current state is not Connected, attempting to open a device.")
                openFirstAvailableDevice()
            }
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Log.d(TAG, "onDeviceRemoved: ${device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
            if (openedDevice?.info?.id == device.id) {
                Log.d(TAG, "The removed device is our current device, closing it.")
                closeDevice()
            }
        }
    }

    init {
        Log.d(TAG, "Initializing MidiDataSource.")
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Log.d(TAG, "MIDI feature is supported on this device.")
            val handler = Handler(Looper.getMainLooper())
            midiManager?.registerDeviceCallback(deviceCallback, handler)
            openFirstAvailableDevice()
        } else {
            Log.e(TAG, "MIDI feature is NOT supported on this device. Check AndroidManifest.xml")
            _connectionState.value = ConnectionState.Error("MIDI API is not supported on this device.")
        }
    }

    private fun openFirstAvailableDevice() {
        Log.d(TAG, "openFirstAvailableDevice: looking for devices.")
        val firstDevice = midiManager?.devices?.firstOrNull()
        if (firstDevice != null) {
            Log.d(TAG, "Found device: ${firstDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}, attempting to open.")
            openDevice(firstDevice)
        } else {
            Log.d(TAG, "No MIDI devices found.")
            _connectionState.value = ConnectionState.NoDevice
        }
    }

    private fun openDevice(deviceInfo: MidiDeviceInfo) {
        Log.d(TAG, "openDevice: trying to open ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        if (openedDevice?.info?.id == deviceInfo.id) {
            Log.d(TAG, "Device is already open. Skipping.")
            return
        }
        
        closeDevice()

        midiManager?.openDevice(deviceInfo, {
            if (it == null) {
                Log.e(TAG, "Failed to open MIDI device.")
                _connectionState.value = ConnectionState.Error("Failed to open MIDI device.")
                return@openDevice
            }
            Log.d(TAG, "Successfully opened device. Setting state to Connected.")
            openedDevice = it
            _connectionState.value = ConnectionState.Connected(midiDeviceMapper.toDomain(deviceInfo))
        }, null)
    }

    private fun closeDevice() {
        if (openedDevice != null) {
            Log.d(TAG, "closeDevice: Closing current device.")
            openedDevice?.close()
            openedDevice = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun close() {
        Log.d(TAG, "close: Unregistering callback and closing device.")
        midiManager?.unregisterDeviceCallback(deviceCallback)
        closeDevice()
    }
}
