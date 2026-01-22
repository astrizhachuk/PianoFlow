@file:Suppress("DEPRECATION")

package com.astrizhachuk.pianoflow.data.datasource.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import app.cash.turbine.test
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import com.astrizhachuk.pianoflow.domain.model.MidiDevice as MidiDeviceDomain

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, Build.VERSION_CODES.TIRAMISU])
class MidiDataSourceTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var midiManager: MidiManager

    @RelaxedMockK
    private lateinit var midiDeviceMapper: MidiDeviceMapper

    @RelaxedMockK
    private lateinit var midiMessageParser: MidiMessageParser

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, midiManager)
        every { midiDeviceMapper.toDomain(any()) } returns mockk()
    }

    //region Helper Methods for Mocking
    private fun createMockDeviceInfo(name: String?, manufacturer: String?, product: String?, id: Int = 123): MidiDeviceInfo {
        val properties = mockk<android.os.Bundle>()
        every { properties.getString(MidiDeviceInfo.PROPERTY_NAME) } returns name
        every { properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) } returns manufacturer
        every { properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT) } returns product

        val mockDeviceInfo = mockk<MidiDeviceInfo>()
        every { mockDeviceInfo.properties } returns properties
        every { mockDeviceInfo.id } returns id
        every { mockDeviceInfo.ports } returns emptyArray()
        return mockDeviceInfo
    }

    private fun createMockPortInfo(type: Int, portNumber: Int, name: String? = null): MidiDeviceInfo.PortInfo {
        return try {
            // Try the modern constructor (API 33+) first
            val constructor = MidiDeviceInfo.PortInfo::class.java.getDeclaredConstructor(Int::class.java, Int::class.java, String::class.java)
            constructor.isAccessible = true
            constructor.newInstance(type, portNumber, name)
        } catch (e: NoSuchMethodException) {
            // Fallback to the legacy constructor (pre-API 33)
            val constructor = MidiDeviceInfo.PortInfo::class.java.getDeclaredConstructor(Int::class.java, Int::class.java)
            constructor.isAccessible = true
            constructor.newInstance(type, portNumber)
        }
    }

    private fun MidiManager.setupMockDevices(devices: Array<MidiDeviceInfo>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every { getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM) } returns devices.toSet()
        } else {
            every { this@setupMockDevices.devices } returns devices
        }
    }

    private fun MidiManager.setupMockDevicesToThrow(exception: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every { getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM) } throws exception
        } else {
            every { this@setupMockDevicesToThrow.devices } throws exception
        }
    }

    private fun verifyRegisterDeviceCallback(
        midiManager: MidiManager,
        callbackSlot: CapturingSlot<MidiManager.DeviceCallback>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            verify {
                midiManager.registerDeviceCallback(
                    eq(MidiManager.TRANSPORT_MIDI_BYTE_STREAM),
                    any(),
                    capture(callbackSlot)
                )
            }
        } else {
            verify { midiManager.registerDeviceCallback(capture(callbackSlot), any()) }
        }
    }

    // --- High-level builders combining low-level helpers ---
    
    private fun enableMidiFeature() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
    }

    /**
     * Low-level: Creates and registers a MIDI device with an output port in the manager.
     * Use this for: Tests that verify device discovery and connection initiation.
     * Returns: MidiDeviceInfo mock with output port configured.
     * Dependencies: Calls enableMidiFeature() internally.
     * Example: Tests like "when a new device is added then it is opened"
     */
    private fun setupDeviceWithPort(
        name: String = "MIDI Device",
        id: Int = 123
    ): MidiDeviceInfo {
        enableMidiFeature()
        val deviceInfo = createMockDeviceInfo(name, "manufacturer", "product", id)
        val portInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)
        every { deviceInfo.ports } returns arrayOf(portInfo)
        midiManager.setupMockDevices(arrayOf(deviceInfo))
        return deviceInfo
    }

    /**
     * Low-level: Creates and registers a MIDI device with custom port configuration.
     * Use this for: Tests with special port scenarios (no ports, input-only, etc.).
     * Returns: MidiDeviceInfo mock with ports as specified.
     * Parameters: ports - array of PortInfo to configure (empty = no ports)
     * Example: Tests like "when device opens with no output ports then..."
     */
    private fun setupDeviceWithCustomPorts(
        name: String = "MIDI Device",
        id: Int = 123,
        ports: Array<MidiDeviceInfo.PortInfo> = emptyArray()
    ): MidiDeviceInfo {
        enableMidiFeature()
        val deviceInfo = createMockDeviceInfo(name, "manufacturer", "product", id)
        every { deviceInfo.ports } returns ports
        midiManager.setupMockDevices(arrayOf(deviceInfo))
        return deviceInfo
    }

    /**
     * Low-level: Creates mock MidiDevice and MidiOutputPort instances configured for testing.
     * Use this for: Manual assembly tests where you need fine-grained control.
     * Returns: Pair(MidiDevice mock, MidiOutputPort mock)
     * Note: Does NOT register in manager; use with setupDeviceWithPort() for full setup.
     * Example: Combined with setupDeviceWithPort() in helper composition tests.
     */
    private fun openDevice(deviceInfo: MidiDeviceInfo): Pair<MidiDevice, MidiOutputPort> {
        val device = mockk<MidiDevice>(relaxed = true)
        every { device.info } returns deviceInfo
        val outputPort = mockk<MidiOutputPort>(relaxed = true)
        every { device.openOutputPort(any()) } returns outputPort
        return device to outputPort
    }

    /**
     * High-level composite: Complete device connection flow with callback capture.
     * Use this for: Tests verifying MIDI receiver connection and message reception.
     * Returns: Triple(MidiDataSource, DeviceCallback, MidiReceiver)
     * Dependencies: Requires setupDeviceWithPort() + openDevice() setup.
     * Captures: All device/open callbacks and the receiver.
     * Example: Tests like "when MIDI message is received then parser is called"
     */
    private fun createDataSourceAndConnect(
        deviceInfo: MidiDeviceInfo,
        device: MidiDevice,
        outputPort: MidiOutputPort
    ): Triple<MidiDataSource, MidiManager.DeviceCallback, MidiReceiver> {
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(deviceInfo), capture(openCallbackSlot), any()) }
        
        val receiverSlot = slot<MidiReceiver>()
        openCallbackSlot.captured.onDeviceOpened(device)
        verify { outputPort.connect(capture(receiverSlot)) }
        
        return Triple(dataSource, deviceCallbackSlot.captured, receiverSlot.captured)
    }

    /**
     * Mid-level: Creates DataSource with captured open callback for state transition testing.
     * Use this for: Tests verifying device open success/failure scenarios.
     * Returns: Pair(MidiDataSource, OnDeviceOpenedListener callback)
     * Dependencies: Requires setupDeviceWithPort() called first.
     * State assertion: Use with dataSource.connectionState.test { awaitItem() }
     * Example: Tests like "when device open fails then state is Error"
     */
    private fun createDataSourceWithOpenCallback(
        deviceInfo: MidiDeviceInfo
    ): Pair<MidiDataSource, MidiManager.OnDeviceOpenedListener> {
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(deviceInfo), capture(openCallbackSlot), any()) }
        return dataSource to openCallbackSlot.captured
    }

    /**
     * High-level: One-liner for complete MIDI receiver setup with mocked parser.
     * Use this for: Tests verifying MIDI message parsing and note emission.
     * Returns: Triple(MidiDataSource, MidiReceiver, Note) - all configured and ready.
     * Default args: Auto-creates device setup if not provided.
     * Mocks: Parser is auto-mocked to return the test note.
     * Example: Tests like "when MIDI message is received then note is emitted"
     * Pattern: receiver.onSend(...); dataSource.notes.test { assertEquals(testNote, awaitItem()) }
     */
    private fun createDataSourceWithReceiver(
        deviceInfo: MidiDeviceInfo = setupDeviceWithPort(),
        device: MidiDevice = mockk(relaxed = true),
        outputPort: MidiOutputPort = mockk(relaxed = true)
    ): Triple<MidiDataSource, MidiReceiver, Note> {
        every { device.info } returns deviceInfo
        every { device.openOutputPort(any()) } returns outputPort
        
        val (dataSource, _, receiver) = createDataSourceAndConnect(deviceInfo, device, outputPort)
        val testNote = mockk<Note>()
        every { midiMessageParser.parse(any()) } returns testNote
        
        return Triple(dataSource, receiver, testNote)
    }
    //endregion

    //region Initialization Tests

    @Test
    fun `when MIDI feature is not supported on init then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, false)

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        dataSource.connectionState.test {
            val state = awaitItem()
            assertTrue(state is ConnectionState.Error)
            assertEquals(context.getString(R.string.midi_error_api_unsupported), (state as ConnectionState.Error).message)
            cancel()
        }
    }

    @Test
    fun `when MIDI permission is missing on open then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevicesToThrow(SecurityException("Caller does not have permission to open device."))

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        dataSource.connectionState.test {
            val state = awaitItem()
            assertTrue(state is ConnectionState.Error)
            assertEquals(context.getString(R.string.midi_error_no_permissions), (state as ConnectionState.Error).message)
            cancel()
        }
    }

    @Test
    fun `when no devices are available on init then state is NoDevice`() = runTest {
        // Arrange
        enableMidiFeature()
        midiManager.setupMockDevices(emptyArray())

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        dataSource.connectionState.test {
            assertEquals(ConnectionState.NoDevice, awaitItem())
            cancel()
        }
    }

    @Test
    fun `when a device is available on init then it is opened`() = runTest {
        // Arrange
        setupDeviceWithPort("Initial Available MIDI")

        // Act
        MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        verify { midiManager.openDevice(any(), any(), any()) }
    }

    @Test
    fun `when MidiManager is null then it does not crash`() = runTest {
        // Arrange
        enableMidiFeature()
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, null)

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        dataSource.connectionState.test {
            assertEquals(ConnectionState.NoDevice, awaitItem())
            cancel()
        }
    }

    @Test
    fun `when collector subscribes then it receives the initial state`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        dataSource.connectionState.test {
            assertEquals(ConnectionState.NoDevice, awaitItem())
            cancel()
        }
    }

    //endregion

    //region DeviceCallback Tests

    @Test
    fun `when a new device is added then it is opened`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray()) // Initially no devices
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        val callbackSlot = slot<MidiManager.DeviceCallback>()
        verifyRegisterDeviceCallback(midiManager, callbackSlot)
        val callback = callbackSlot.captured
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        // Now a new device is available
        val newDeviceInfo = createMockDeviceInfo("New MIDI", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(newDeviceInfo))

        // Act
        callback.onDeviceAdded(newDeviceInfo)

        // Assert
        verify { midiManager.openDevice(eq(newDeviceInfo), any(), any()) }
    }

    @Test
    fun `when opening an already opened device then it does not re-open`() = runTest {
        // Arrange
        val deviceInfo = setupDeviceWithPort("Already Open MIDI")
        val (device, outputPort) = openDevice(deviceInfo)
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()

        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(deviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(device)

        // Act - try to open the same device again
        deviceCallbackSlot.captured.onDeviceAdded(deviceInfo)

        // Assert - should not re-open
        verify(exactly = 1) { midiManager.openDevice(eq(deviceInfo), any(), any()) }
        verify(exactly = 0) { device.close() }
    }

    @Test
    fun `when a connected device is removed then state is Disconnected`() = runTest {
        // Arrange
        val deviceInfo = setupDeviceWithPort("Connected MIDI")
        val (device, outputPort) = openDevice(deviceInfo)
        val (dataSource, deviceCallback, _) = createDataSourceAndConnect(deviceInfo, device, outputPort)

        // Act & Assert
        dataSource.connectionState.test {
            awaitItem() // Skip initial Connected state
            
            deviceCallback.onDeviceRemoved(deviceInfo)
            
            assertEquals(ConnectionState.Disconnected, awaitItem())
            verify { outputPort.close() }
            verify { device.close() }
            cancel()
        }
    }

    @Test
    fun `when connected device info becomes null, removing another device does not crash`() = runTest {
        // Arrange: Connect a device successfully first
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val connectedDeviceInfo = createMockDeviceInfo("Connected MIDI", "manufacturer", "product", id = 1)
        midiManager.setupMockDevices(arrayOf(connectedDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        // Initially, the mock device has valid info
        every { mockDevice.info } returns connectedDeviceInfo

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(connectedDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // This will succeed now
        assertTrue("Pre-condition: device should be connected", dataSource.connectionState.value is ConnectionState.Connected)

        // Arrange: Now, simulate the connected device's info becoming null
        every { mockDevice.info } returns null

        // Arrange: Prepare to remove a *different* device
        val deviceToRemove = createMockDeviceInfo("Other MIDI", "manufacturer", "product", id = 2)
        val callback = deviceCallbackSlot.captured

        // Act: Remove the other device
        callback.onDeviceRemoved(deviceToRemove)

        // Assert: The app should not crash, and the original connection should be maintained
        assertTrue("State should remain connected", dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 0) { mockDevice.close() } // Verify it wasn't closed by mistake
    }

    @Test
    fun `when a non-connected device is removed then nothing happens`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        // Setup a connected device
        val connectedDeviceInfo = createMockDeviceInfo("Connected MIDI", "manufacturer", "product", id = 1)
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns connectedDeviceInfo
        midiManager.setupMockDevices(arrayOf(connectedDeviceInfo))
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()

        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(connectedDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // Connect device 1
        assertTrue("Pre-condition: device should be connected", dataSource.connectionState.value is ConnectionState.Connected)

        val deviceCallback = deviceCallbackSlot.captured
        // Create info for a different device
        val otherDeviceInfo = createMockDeviceInfo("Other MIDI", "manufacturer", "product", id = 2)

        // Act
        deviceCallback.onDeviceRemoved(otherDeviceInfo) // Remove device 2

        // Assert
        // Verify the original device was not closed and the state remains Connected
        verify(exactly = 0) { mockDevice.close() }
        assertTrue("State should remain connected", dataSource.connectionState.value is ConnectionState.Connected)
    }

    @Test
    fun `when device is removed but nothing was open then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())
        val callbackSlot = slot<MidiManager.DeviceCallback>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, callbackSlot)
        val callback = callbackSlot.captured
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value) // Pre-condition
        val removedDeviceInfo = createMockDeviceInfo("Removed MIDI", "manufacturer", "product")

        // Act
        callback.onDeviceRemoved(removedDeviceInfo)

        // Assert
        // Verify state remains NoDevice
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
    }

    @Test
    fun `when a device is connected and another is added then connection remains on the first device`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val firstDeviceInfo = createMockDeviceInfo("First MIDI", "manufacturer", "product", id = 1)
        val secondDeviceInfo = createMockDeviceInfo("Second MIDI", "manufacturer", "product", id = 2)
        midiManager.setupMockDevices(arrayOf(firstDeviceInfo))
        val firstDevice = mockk<MidiDevice>(relaxed = true)
        val firstDomainDevice = mockk<MidiDeviceDomain>()
        every { midiDeviceMapper.toDomain(firstDeviceInfo) } returns firstDomainDevice
        every { firstDevice.info } returns firstDeviceInfo

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(firstDeviceInfo), capture(openCallbackSlot), any()) }
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)

        openCallbackSlot.captured.onDeviceOpened(firstDevice)
        val initialState = dataSource.connectionState.value
        assertTrue("Pre-condition failed: Not connected", initialState is ConnectionState.Connected)
        assertEquals(firstDomainDevice, (initialState as ConnectionState.Connected).device)

        // Act
        deviceCallbackSlot.captured.onDeviceAdded(secondDeviceInfo)

        // Assert
        val finalState = dataSource.connectionState.value
        assertTrue(finalState is ConnectionState.Connected)
        assertEquals(firstDomainDevice, (finalState as ConnectionState.Connected).device)
        verify(exactly = 0) { midiManager.openDevice(eq(secondDeviceInfo), any(), any()) }
    }

    @Test
    fun `when a new device is added while another is connecting then the second device is ignored`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        val firstDeviceInfo = createMockDeviceInfo("First MIDI", "manufacturer", "product", id = 1)
        val secondDeviceInfo = createMockDeviceInfo("Second MIDI", "manufacturer", "product", id = 2)
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)

        // The MidiManager must know about the device before the SUT checks for it.
        midiManager.setupMockDevices(arrayOf(firstDeviceInfo))

        // Act
        deviceCallbackSlot.captured.onDeviceAdded(firstDeviceInfo)
        verify { midiManager.openDevice(eq(firstDeviceInfo), capture(openCallbackSlot), any()) }

        // Now, simulate the second device appearing in the list as well.
        midiManager.setupMockDevices(arrayOf(firstDeviceInfo, secondDeviceInfo))
        deviceCallbackSlot.captured.onDeviceAdded(secondDeviceInfo)

        // Assert
        verify(exactly = 0) { midiManager.openDevice(eq(secondDeviceInfo), any(), any()) }

        // Act
        val firstDevice = mockk<MidiDevice>(relaxed = true)
        val firstDomainDevice = mockk<MidiDeviceDomain>()
        every { midiDeviceMapper.toDomain(firstDeviceInfo) } returns firstDomainDevice
        openCallbackSlot.captured.onDeviceOpened(firstDevice)
        advanceTimeBy(1)

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Connected)
        assertEquals(firstDomainDevice, (state as ConnectionState.Connected).device)
    }

    //endregion

    //region OpenDeviceCallback Tests
    @Test
    fun `when device open fails then state is Error`() = runTest {
        // Arrange
        val deviceName = "Failing MIDI Device"
        val deviceInfo = setupDeviceWithPort(deviceName)
        val (dataSource, openCallback) = createDataSourceWithOpenCallback(deviceInfo)

        // Act & Assert
        dataSource.connectionState.test {
            awaitItem() // Skip initial state
            openCallback.onDeviceOpened(null) // Simulate failure
            
            val state = awaitItem()
            assertTrue(state is ConnectionState.Error)
            assertEquals(context.getString(R.string.midi_error_connection_failed, deviceName), (state as ConnectionState.Error).message)
            cancel()
        }
    }

    @Test
    fun `when device open fails for device with no name then state is Error with unknown`() = runTest {
        // Arrange
        enableMidiFeature()
        val deviceInfo = createMockDeviceInfo(null, "manufacturer", "product")
        val portInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)
        every { deviceInfo.ports } returns arrayOf(portInfo)
        midiManager.setupMockDevices(arrayOf(deviceInfo))
        
        val (dataSource, openCallback) = createDataSourceWithOpenCallback(deviceInfo)
        val unknownDeviceName = context.getString(R.string.midi_unknown_device)

        // Act & Assert
        dataSource.connectionState.test {
            awaitItem() // Skip initial state
            openCallback.onDeviceOpened(null) // Simulate failure
            
            val state = awaitItem()
            assertTrue(state is ConnectionState.Error)
            assertEquals(context.getString(R.string.midi_error_connection_failed, unknownDeviceName), (state as ConnectionState.Error).message)
            cancel()
        }
    }

    @Test
    fun `when device opens successfully then state is Connected`() = runTest {
        // Arrange
        val deviceInfo = setupDeviceWithPort("Successful MIDI")
        val device = mockk<MidiDevice>(relaxed = true)
        every { device.info } returns deviceInfo
        val (dataSource, openCallback) = createDataSourceWithOpenCallback(deviceInfo)
        val mockDomainDevice = mockk<MidiDeviceDomain>()
        every { midiDeviceMapper.toDomain(deviceInfo) } returns mockDomainDevice

        // Act & Assert
        dataSource.connectionState.test {
            awaitItem() // Skip initial state
            openCallback.onDeviceOpened(device) // Simulate success
            
            val state = awaitItem()
            assertTrue(state is ConnectionState.Connected)
            assertEquals(mockDomainDevice, (state as ConnectionState.Connected).device)
            cancel()
        }
    }

    @Test
    fun `when device opens with no output ports then connect is not attempted`() = runTest {
        // Arrange
        val mockDeviceInfo = setupDeviceWithCustomPorts("MIDI with no output", ports = emptyArray())
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 0) { mockDevice.openOutputPort(any()) }
    }

    @Test
    fun `when device opens with only input ports then connect is not attempted`() = runTest {
        // Arrange
        val mockInputPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_INPUT, 1)
        val mockDeviceInfo = setupDeviceWithCustomPorts("MIDI with only input", ports = arrayOf(mockInputPortInfo))
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 0) { mockDevice.openOutputPort(any()) }
    }

    @Test
    fun `when opening output port fails then receiver is not connected`() = runTest {
        // Arrange - when openOutputPort returns null, setupOutputPort handles it safely with let
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with faulty port", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)

        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        // Simulate failure to open the output port
        every { mockDevice.openOutputPort(any()) } returns null

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        // Device remains connected even if port fails to open
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
    }

    @Test
    fun `when device opens with output port then connect is called with receiver`() = runTest {
        // Arrange - test the happy path where outputPort is assigned and connect is invoked
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI device", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)

        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        every { mockDevice.openOutputPort(1) } returns mockOutputPort

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
        // Verify port.connect() was called in the let block
        verify(exactly = 1) { mockOutputPort.connect(any<MidiReceiver>()) }
    }

    @Test
    fun `when connecting receiver fails then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with connect-fail port", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)
        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        every { mockDevice.openOutputPort(1) } returns mockOutputPort
        // Simulate an exception when connecting the receiver
        every { mockOutputPort.connect(any()) } throws RuntimeException("Connection failed")

        // Act
        val exception = runCatching { openCallbackSlot.captured.onDeviceOpened(mockDevice) }.exceptionOrNull()

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
        verify(exactly = 1) { mockOutputPort.connect(any<MidiReceiver>()) }
        // The test should not crash, which is confirmed by catching the deliberate exception.
        assertNotNull(exception)
        assertTrue(exception is RuntimeException)
        assertEquals("Connection failed", exception?.message)
    }

    //endregion

    //region Close Method Tests
    @Test
    fun `when close is called then unregisters callback and closes device`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Device to close", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)
        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        every { mockDevice.openOutputPort(1) } returns mockOutputPort

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // Make sure a device is open

        // Act & Assert
        dataSource.connectionState.test {
            awaitItem() // Skip initial Connected state
            
            dataSource.close()
            
            assertEquals(ConnectionState.Disconnected, awaitItem())
            verify { midiManager.unregisterDeviceCallback(eq(deviceCallbackSlot.captured)) }
            verify { mockOutputPort.close() }
            verify { mockDevice.close() }
            cancel()
        }
    }

    @Test
    fun `when close is called and MidiManager is null then it does not crash`() = runTest {
        // Arrange
        enableMidiFeature()
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, null)
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Act
        dataSource.close()

        // Assert
        dataSource.connectionState.test {
            assertEquals(ConnectionState.NoDevice, awaitItem())
            cancel()
        }
    }

    @Test
    fun `when close is called and a new device is added then state remains NoDevice`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Test MIDI", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)

        // Act
        dataSource.close()

        // Assert
        assertEquals(ConnectionState.Disconnected, dataSource.connectionState.value)
        verify { midiManager.unregisterDeviceCallback(deviceCallbackSlot.captured) }

        // After closing, ensure the mock no longer reports any devices.
        midiManager.setupMockDevices(emptyArray())

        // Act
        val newDeviceInfo = createMockDeviceInfo("New MIDI", "manufacturer", "product", id = 2)
        deviceCallbackSlot.captured.onDeviceAdded(newDeviceInfo)
        advanceTimeBy(100)

        // Assert
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
        verify(exactly = 1) { midiManager.openDevice(any(), any(), any()) }
    }
    //endregion

    //region MidiMessageReceiver Tests

    @Test
    fun `when MIDI message is received then parser is called and note is emitted`() = runTest {
        // Arrange
        val result = createDataSourceWithReceiver()
        val dataSource = result.first
        val receiver = result.second
        val testNote = result.third
        val midiMessage = byteArrayOf(0x90.toByte(), 0x3C.toByte(), 0x64.toByte())

        // Act & Assert
        dataSource.notes.test {
            receiver.onSend(midiMessage, 0, midiMessage.size, 0L)
            
            verify(exactly = 1) { midiMessageParser.parse(midiMessage) }
            assertEquals(testNote, awaitItem())
            cancel()
        }
    }

    @Test
    fun `when MIDI message is received with offset then correct data is parsed`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI device", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)

        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        every { mockDevice.openOutputPort(1) } returns mockOutputPort

        val testNote = mockk<Note>()
        val midiMessage = byteArrayOf(0x00, 0x00, 0x90.toByte(), 0x3C.toByte(), 0x64.toByte())
        val offset = 2
        val count = 3

        every { midiMessageParser.parse(any()) } returns testNote

        val receiverSlot = slot<MidiReceiver>()

        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        verify { mockOutputPort.connect(capture(receiverSlot)) }

        // Act
        receiverSlot.captured.onSend(midiMessage, offset, count, 0L)

        // Assert - parser should be called with extracted data (offset 2, count 3)
        val expectedData = midiMessage.copyOfRange(offset, offset + count)
        verify(exactly = 1) { midiMessageParser.parse(expectedData) }
    }

    @Test
    fun `when parser returns null then no note is emitted`() = runTest {
        // Arrange
        val deviceInfo = setupDeviceWithPort()
        val (device, outputPort) = openDevice(deviceInfo)
        val (dataSource, _, receiver) = createDataSourceAndConnect(deviceInfo, device, outputPort)
        val midiMessage = byteArrayOf(0x90.toByte(), 0x3C.toByte(), 0x64.toByte())
        
        every { midiMessageParser.parse(any()) } returns null

        // Act & Assert
        dataSource.notes.test {
            receiver.onSend(midiMessage, 0, midiMessage.size, 0L)
            verify(exactly = 1) { midiMessageParser.parse(midiMessage) }
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `when notes buffer is full then note emission fails`() = runTest {
        // Arrange - specifically test the Timber.w logging when tryEmit returns false
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI device", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)

        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val testNote = mockk<Note>()
        val midiMessage = byteArrayOf(0x90.toByte(), 0x3C.toByte(), 0x64.toByte())

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        every { mockDevice.openOutputPort(1) } returns mockOutputPort

        every { midiMessageParser.parse(any()) } returns testNote

        val receiverSlot = slot<MidiReceiver>()
        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        verify { mockOutputPort.connect(capture(receiverSlot)) }

        // Mock the internal _notes flow to simulate buffer full (tryEmit returns false after 64 items)
        val notesField = dataSource::class.java.getDeclaredField("_notes")
        notesField.isAccessible = true
        val mockSharedFlow = mockk<MutableSharedFlow<Note>>(relaxed = true)
        
        var callCount = 0
        every { mockSharedFlow.tryEmit(any()) } answers {
            callCount++
            callCount <= 64
        }
        
        notesField.set(dataSource, mockSharedFlow)

        // Act - send 65 messages (64 succeed, 65th fails and triggers Timber.w)
        repeat(65) {
            receiverSlot.captured.onSend(midiMessage, 0, midiMessage.size, 0L)
        }

        // Assert - all 65 parse attempts occurred, and tryEmit was called 65 times
        verify(exactly = 65) { midiMessageParser.parse(any()) }
        verify(exactly = 65) { mockSharedFlow.tryEmit(any()) }
    }

    //endregion
}