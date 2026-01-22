package com.astrizhachuk.pianoflow.data.datasource.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.tracing.trace
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import android.media.midi.MidiDevice as MidiDeviceApi

/**
 * Data source that encapsulates all the logic for working with the Android MIDI API.
 *
 * This class is responsible for:
 * - Registering and unregistering callbacks to track the connection/disconnection of MIDI devices.
 * - Opening and closing connections to MIDI devices.
 * - Providing the connection state as a `Flow` to the rest of the application.
 *
 * It is a singleton class (@Singleton) as it must exist as a single instance throughout
 * the entire application lifecycle to continuously monitor the state of MIDI devices.
 *
 * @param context The application context, necessary for accessing system services like [MidiManager].
 * @param midiDeviceMapper A mapper for converting the system model [MidiDeviceInfo] to the domain model.
 * @param midiMessageParser A parser for extracting data from MIDI messages.
 */
class MidiDataSource @Inject constructor(
    private val context: Context,
    private val midiDeviceMapper: MidiDeviceMapper,
    private val midiMessageParser: MidiMessageParser
) {
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private var openedDevice: MidiDeviceApi? = null
    private var outputPort: MidiOutputPort? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
    private val _notes = MutableSharedFlow<Note>(extraBufferCapacity = 64)

    /**
     * Public flow representing the current state of the MIDI device connection.
     *
     * Subscribers can track changes and react to states:
     * - [ConnectionState.NoDevice]: No devices found.
     * - [ConnectionState.Connected]: The device is successfully connected.
     * - [ConnectionState.Disconnected]: The device has been disconnected.
     * - [ConnectionState.Error]: An error has occurred.
     */
    val connectionState = _connectionState.asStateFlow()

    /**
     * Public flow of received MIDI notes.
     */
    val notes = _notes.asSharedFlow()

    /**
     * A callback for the system [MidiManager] that tracks the physical
     * connection and disconnection of MIDI devices to the Android device.
     */
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        /**
         * Called by the system when a new MIDI device is connected.
         * Starts the process of finding and opening the first available device.
         */
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Timber.i("onDeviceAdded: Device detected: %s", device.deviceName(context))
            openFirstAvailableDevice()
        }

        /**
         * Called by the system when a MIDI device is disconnected.
         * If the disconnected device is the currently open device, it closes it.
         */
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Timber.w("onDeviceRemoved: Device disconnected: %s", device.deviceName(context))
            if (isCurrentDevice(device)) {
                Timber.d("onDeviceRemoved: Disconnected device is current, closing.")
                closeDevice()
            }
        }
    }

    private val midiMessageReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            midiMessageParser.parse(msg.copyOfRange(offset, offset + count))?.let { note ->
                if (!_notes.tryEmit(note)) {
                    Timber.w("Failed to emit note, buffer is full.")
                }
            }
        }
    }

    init {
        trace("MidiDataSource.init") {
            Timber.i("init: Initializing.")
            when {
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI) -> {
                    Timber.w("init: MIDI feature NOT supported.")
                    _connectionState.value = ConnectionState.Error(context.getString(R.string.midi_error_api_unsupported))
                }
                midiManager == null -> {
                    Timber.w("init: MidiManager is null, MIDI system service not available.")
                    _connectionState.value = ConnectionState.NoDevice
                }
                else -> {
                    Timber.i("init: MIDI feature supported.")
                    midiManager.registerDeviceCallbackCompat(deviceCallback, Handler(Looper.getMainLooper()))
                    openFirstAvailableDevice()
                }
            }
        }
    }

    /**
     * Completely releases all resources used by [MidiDataSource].
     * Unregisters the callback and closes the open device.
     *
     * In the current architecture, where [MidiDataSource] is a singleton class (@Singleton),
     * this method is not called in the normal application workflow. However, it provides
     * the necessary mechanism for explicit resource management in cases where
     * the lifecycle of this component needs to be managed manually.
     */
    @Suppress("unused")
    fun close() {
        Timber.i("close: Unregistering callback and closing device.")
        midiManager?.unregisterDeviceCallback(deviceCallback)
        closeDevice()
    }

    /**
     * Finds the first available MIDI device in the system and initiates its opening.
     * If no devices are found, sets the state to [ConnectionState.NoDevice].
     * Safely handles SecurityException if the application does not have permissions.
     */
    private fun openFirstAvailableDevice() {
        Timber.d("openFirstAvailableDevice: Looking for devices.")
        try {
            val device = midiManager!!.getFirstAvailableDevice()
            if (device != null) {
                Timber.i("openFirstAvailableDevice: Found device: %s. Attempting to open.", device.deviceName(context))
                openDevice(device)
            } else {
                Timber.i("openFirstAvailableDevice: No MIDI devices found.")
                _connectionState.value = ConnectionState.NoDevice
            }
        } catch (e: SecurityException) {
            Timber.e(e, "openFirstAvailableDevice: SecurityException while getting devices.")
            _connectionState.value = ConnectionState.Error(context.getString(R.string.midi_error_no_permissions))
        }
    }

    /**
     * Opens a connection to the specified MIDI device.
     * Before opening a new device, it closes any previously opened one.
     * Handles the result of the asynchronous call to [MidiManager.openDevice].
     *
     * @param deviceInfo Information about the MIDI device to be opened.
     */
    private fun openDevice(deviceInfo: MidiDeviceInfo) {
        Timber.i("openDevice: Opening device: %s", deviceInfo.deviceName(context))
        if (isCurrentDevice(deviceInfo)) {
            Timber.d("openDevice: Device already open, skipping.")
            return
        }
        
        closeDevice()
        midiManager!!.openDevice(deviceInfo, { device ->
            if (device == null) {
                Timber.w("openDevice: Failed to open device: %s", deviceInfo.deviceName(context))
                _connectionState.value = ConnectionState.Error(
                    context.getString(R.string.midi_error_connection_failed, deviceInfo.deviceName(context))
                )
                return@openDevice
            }
            openedDevice = device
            Timber.i("openDevice: Device opened successfully.")
            _connectionState.value = ConnectionState.Connected(midiDeviceMapper.toDomain(deviceInfo))
            setupOutputPort(device)
        }, null)
    }

    /**
     * Finds and configures an output port to receive MIDI data from the device.
     *
     * In the context of the Android MIDI API, a device's "output" port is the port from which
     * the application can *read* data (i.e., the device "outputs" data to the application).
     * This method finds the first available port of type [MidiDeviceInfo.PortInfo.TYPE_OUTPUT],
     * opens it, and connects [midiMessageReceiver] to it to listen for incoming MIDI messages.
     *
     * @param device The opened MIDI device ([MidiDeviceApi]) for which the port needs to be configured.
     */
    private fun setupOutputPort(device: MidiDeviceApi) {
        val portInfo = device.info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
            ?: return run { Timber.e("setupOutputPort: Device has no output ports to receive data from.") }

        device.openOutputPort(portInfo.portNumber)?.also { port ->
            outputPort = port
            Timber.i("setupOutputPort: Output port %d opened. Connecting receiver.", portInfo.portNumber)
            port.connect(midiMessageReceiver)
        } ?: Timber.e("setupOutputPort: Failed to open output port %d.", portInfo.portNumber)
    }

    /**
     * Correctly closes the currently open MIDI device, releases resources,
     * and updates the connection state to [ConnectionState.Disconnected].
     */
    private fun closeDevice() {
        openedDevice?.also {
            Timber.i("closeDevice: Closing device.")
            outputPort?.close()
            outputPort = null
            it.close()
            openedDevice = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Checks if the provided [device] is the currently open device.
     *
     * @param device The device to check.
     * @return `true` if the ID of the provided device matches the ID of the currently open device, otherwise `false`.
     */
    private fun isCurrentDevice(device: MidiDeviceInfo): Boolean {
        return openedDevice?.info?.id == device.id
    }
}

/**
 * Registers a callback to track MIDI devices,
 * using the appropriate API depending on the Android version and ensuring execution on a single thread.
 *
 * For Android 13 (API 33) and higher, [MidiManager.registerDeviceCallback] is used
 * specifying the [MidiManager.TRANSPORT_MIDI_BYTE_STREAM] transport. The provided [handler]
 * is adapted into an `Executor` to execute the callback on the correct thread.
 * For older versions, the deprecated [MidiManager.registerDeviceCallback] method is used,
 * to which the [handler] is passed directly.
 *
 * @param callback The callback for device events.
 * @param handler The Handler on whose thread the callbacks will be executed.
 */
private fun MidiManager.registerDeviceCallbackCompat(
    callback: MidiManager.DeviceCallback,
    handler: Handler
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerDeviceCallback(
            MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
            handler::post,
            callback
        )
    } else {
        @Suppress("DEPRECATION")
        registerDeviceCallback(callback, handler)
    }
}

/**
 * Returns the first available MIDI device, using the appropriate API depending on the Android version.
 *
 * For Android 13 (API 33) and higher, [MidiManager.getDevicesForTransport] is used
 * to get devices connected via [MidiManager.TRANSPORT_MIDI_BYTE_STREAM].
 * For older versions, the deprecated [MidiManager.getDevices] method is used.
 */
private fun MidiManager.getFirstAvailableDevice(): MidiDeviceInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        devices.firstOrNull()
    }
}

/**
 * Helper extension property to safely get the name of a [MidiDeviceInfo].
 *
 * Returns the device name from its properties, or `context.getString(R.string.midi_unknown_device)` if the name is missing.
 */
private fun MidiDeviceInfo.deviceName(context: Context): String {
    return properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        ?: context.getString(R.string.midi_unknown_device)
}
