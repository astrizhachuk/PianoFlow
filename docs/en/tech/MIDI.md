# MIDI API in Android

The `android.media.midi` package provides all the necessary tools for interacting with MIDI (Musical Instrument Digital Interface) devices. It allows applications to discover, configure, and exchange data with MIDI keyboards, synthesizers, and other controllers connected via USB, Bluetooth Low Energy (BLE), or operating as virtual (software) devices.

## Getting Started: A Step-by-Step Guide

Below is a sequence of steps for connecting to and interacting with a MIDI device.

### 1. Declaration in the Manifest

To make your application available on Google Play only for devices that support MIDI, you need to add the following declaration to `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.software.midi" android:required="true" />
```

This indicates a requirement for **software** support for the MIDI API (starting with Android 6.0), rather than rare hardware MIDI ports.

### 2. Checking for Support in Code

Although declaring in the manifest will prevent the application from being installed from Google Play on incompatible devices, it is also recommended to check for MIDI support in the code. This is useful in case the application is installed in other ways, or for a more graceful degradation of functionality.

```kotlin
if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
    // MIDI is supported, you can continue working
} else {
    // MIDI is not supported on this device
    // Inform the user or disable MIDI functionality
}
```

### 3. Getting `MidiManager`

`MidiManager` is the starting point for working with MIDI. It provides access to all other API functions.

```kotlin
val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
```

### 4. Selecting a Device (`MidiDevice`)

Connected devices are represented by the `MidiDevice` class. To get a list of them, `MidiManager` is used:

```kotlin
val devices: Array<MidiDeviceInfo> = midiManager.devices
```

Each `MidiDeviceInfo` element contains metadata about the device: properties (name, manufacturer), as well as the number and types of ports.

### 5. Tracking Connection and Disconnection

To dynamically track devices, `MidiManager.DeviceCallback` is used.

```kotlin
midiManager.registerDeviceCallback(object : MidiManager.DeviceCallback() {
    override fun onDeviceAdded(device: MidiDeviceInfo) {
        // Logic for when a new device is connected
    }
    override fun onDeviceRemoved(device: MidiDeviceInfo) {
        // Logic for when a device is disconnected
    }
}, Handler(Looper.getMainLooper()))
```

### 6. Opening a Device and Working with Ports

To exchange data, the device must be opened. This process is asynchronous:

```kotlin
midiManager.openDevice(deviceInfo, {
    // This callback will be called when the device is ready
    val midiDevice = it
    if (midiDevice == null) {
        // Handling the opening error
    } else {
        // The device is successfully opened
    }
}, null)
```

After opening, you get access to the device's ports.

> **Important note on port terminology:**
> The Android MIDI API has a very non-intuitive but critically important feature of port naming. They are named **from the point of view of your application**, not the external device.
> *   **`MidiOutputPort`:** Used to **receive** data *from* a MIDI device. You open it and `connect()` your `MidiReceiver` to it.
> *   **`MidiInputPort`:** Used to **send** data *to* a MIDI device. You open it and call `send()`.
> 
> This inversion is a frequent source of errors.

### 7. Reading Data from a Device

To read data, you need to open an **output port** and pass it an implementation of the `MidiReceiver` class. This is an abstract class that you must inherit to handle incoming MIDI messages.

```kotlin
// 1. Create your own class for handling MIDI messages
class MyMidiReceiver : MidiReceiver() {
    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        // Here you process the incoming MIDI packet (byte array)
    }
}

// 2. Find the port that is the output for the MIDI device
val portInfo = midiDevice.info.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }

// 3. Open this port as an output for our application and connect the handler
val outputPort = midiDevice.openOutputPort(portInfo.portNumber)
outputPort.connect(MyMidiReceiver())
```

### 8. Sending Data to a Device

To send data, an **input port** is opened. You can directly write byte arrays with MIDI commands to it.

```kotlin
// 1. Find the port that is the input for the MIDI device
val portInfo = midiDevice.info.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }

// 2. Open this port as an input for our application
val inputPort = midiDevice.openInputPort(portInfo.portNumber)
val buffer = ByteArray(32)
// ...fill the buffer with a MIDI command...
inputPort.send(buffer, 0, buffer.size)
```

Don't forget to close the ports (`port.close()`) and devices (`device.close()`) when they are no longer needed.

## Structure of MIDI Messages

Data from a MIDI device comes in the form of a byte array (`ByteArray`). To understand what these bytes mean, you need the MIDI 1.0 protocol specification.

Let's look at an example of a function for parsing incoming data:

```kotlin
fun parse(data: ByteArray): Note? {
    if (data.isEmpty()) return null

    // MIDI message status byte (first byte)
    val status = data[0].toInt() and 0xFF
    // Message command (upper 4 bits)
    val command = status and 0xF0
    // MIDI channel (lower 4 bits)
    val channel = status and 0x0F

    // 0x90 - Note On command
    // We only check the command, ignoring the channel
    if (command == 0x90) {
        // Make sure there is enough data in the message
        if (data.size < 3) return null

        val pitch = data[1].toInt()    // Note number (0-127)
        val velocity = data[2].toInt() // Key press velocity (1-127)

        // If velocity == 0, it is equivalent to Note Off
        return if (velocity > 0) {
            Note(pitch = pitch, velocity = velocity, channel = channel)
        } else {
            // This is a Note Off message, return null or a special object
            null
        }
    }

    // You can add handling for other commands here (Note Off 0x80, Control Change 0xB0, etc.)

    return null
}
```

Parsing bytes based on the specification:

*   `val status = data[0].toInt() and 0xFF`: You take the first byte. According to the specification, this is the **Status Byte**.
*   `val command = status and 0xF0`: You use the `0xF0` mask to separate the upper 4 bits that define the command. In the specification table, this corresponds to the "Code (Binary)" column. For example, for *Note On*, this is `1001nnnn`. Your `0xF0` mask (in binary `11110000`) leaves only `10010000`, which is equal to `0x90`.
*   `if (command == 0x90)`: You compare the result with `0x90`. According to the table, this is the status byte for the **Note On** command.
*   `val pitch = data[1].toInt()` and `val velocity = data[2].toInt()`: You read the second and third bytes. The specification confirms that the *Note On* status byte is followed by two data bytes: "Byte 2" (**Key number**) and "Byte 3" (**Velocity**).

### What is a MIDI Channel

In the code above, we also extract the channel number: `val channel = status and 0x0F`. What is it?

Imagine you have an orchestra, but only one conductor (one MIDI connection). MIDI channels are the conductor's way of addressing a specific group of musicians (for example, "violins only" or "drums only").

*   **16 channels in total:** The MIDI 1.0 protocol defines 16 independent channels. In the code, they are numbered **from 0 to 15**, but for users (for example, in synthesizer settings), they are usually displayed as channels **from 1 to 16**.

*   **How it works:** Most MIDI messages (called "channel messages") contain a 4-bit channel number in their first byte (the status byte).
    *   Status byte `1001nnnn` (Note On) or `1000nnnn` (Note Off).
    *   The upper 4 bits (`1001`) define the command itself.
    *   The lower 4 bits (`nnnn`) are the channel number (a value from `0000` to `1111`, i.e., from 0 to 15).
    *   The code `status and 0x0F` isolates these 4 bits.

*   **Why it is needed:** Channels allow you to control up to 16 different instruments (timbres) with a single MIDI cable. For example, a MIDI keyboard can send notes for a piano on channel 1 and for a bass on channel 2. The synthesizer that receives this data will know which sound to play for each note based on its channel number. Channel 10 is traditionally reserved for percussion instruments.

Thus, the information about the structure of the `data` array that is passed to your function comes directly from the official specifications published on the [midi.org](https://www.midi.org/) website.

## Creating a Virtual MIDI Device

The Android MIDI API allows your application to become a MIDI device itself, to which other applications (such as a DAW or sequencer) can connect. To do this, you need to:

1.  **Create a service** that inherits from `MidiDeviceService`.
2.  **Override the `onGetInputPortReceivers()` method**. This method should return an array of `MidiReceiver` that will process the data sent to your virtual device by other applications.
3.  **Declare the service in `AndroidManifest.xml`** with the appropriate intent filter:

    ```xml
    <service android:name=".MyMidiDeviceService"
             android:permission="android.permission.BIND_MIDI_DEVICE_SERVICE">
        <intent-filter>
            <action android:name="android.media.midi.MidiDeviceService" />
        </intent-filter>
    </service>
    ```

## Key Classes of the Package

*   `MidiManager`: The central class for discovering and managing devices.
*   `MidiDevice`: Represents a real or virtual MIDI device.
*   `MidiDeviceInfo`: Contains information about the device (properties, ports).
*   `MidiInputPort` / `MidiOutputPort`: Classes for reading and writing MIDI data.
*   `MidiReceiver`: An abstract class that you implement to receive MIDI data.
*   `MidiSender`: An interface for classes that can send MIDI data (for example, `MidiInputPort`).
*   `MidiDeviceService`: The base class for creating virtual MIDI devices.

## For Latency-Critical Applications (C/C++)

For applications where minimal latency is important (real-time synthesizers, professional DAWs), it is recommended to use the **Native MIDI API (AMidi)**, available through the NDK. It allows you to process MIDI messages in C/C++ code, which significantly reduces latency. At the same time, `MidiManager` from Java/Kotlin is still used for device discovery and connection.

## Official Resources and Links

### MIDI Specification

*   **MIDI 1.0 Message Specification**:
    *   https://midi.org/spec-detail

### Android MIDI API

*   **android.media.midi package overview**:
    *   [https://developer.android.com/reference/android/media/midi/package-summary](https://developer.android.com/reference/android/media/midi/package-summary)
*   **Official documentation for MidiManager**:
    *   [https://developer.android.com/reference/android/media/midi/MidiManager](https://developer.android.com/reference/android/media/midi/MidiManager)
*   **Guide to the Native MIDI API (for C/C++)**:
    *   [https://developer.android.com/ndk/guides/audio/midi](https://developer.android.com/ndk/guides/audio/midi)
*   **Official examples (Best Practices)**:
    *   [https://github.com/android/midi-samples](https://github.com/android/midi-samples)
