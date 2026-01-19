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
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
@Config(sdk = [23, 33])
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
    //endregion

    //region Initialization Tests

    @Test
    fun `when MIDI feature is not supported on init then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, false)

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        val state = dataSource.connectionState.first()
        assertTrue(state is ConnectionState.Error)
        assertEquals(context.getString(R.string.midi_error_api_unsupported), (state as ConnectionState.Error).message)
    }

    @Test
    fun `when MIDI permission is missing on open then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevicesToThrow(SecurityException("Caller does not have permission to open device."))

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        val state = dataSource.connectionState.first()
        assertTrue(state is ConnectionState.Error)
        assertEquals(context.getString(R.string.midi_error_no_permissions), (state as ConnectionState.Error).message)
    }

    @Test
    fun `when no devices are available on init then state is NoDevice`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        val state = dataSource.connectionState.first()
        assertEquals(ConnectionState.NoDevice, state)
    }

    @Test
    fun `when a device is available on init then it is opened`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Initial Available MIDI", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        // Act
        MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        verify { midiManager.openDevice(eq(mockDeviceInfo), any(), any()) }
    }

    @Test
    fun `when MidiManager is null then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        // Important: Set the mock midiManager to null to simulate it being unavailable
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, null)

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        val state = dataSource.connectionState.value
        // Expect that it gracefully sets to NoDevice as it can't find any
        assertEquals(ConnectionState.NoDevice, state)
    }

    @Test
    fun `when collector subscribes then it receives the initial state`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Assert
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.first())
    }

    //endregion

    //region DeviceCallback Tests

    @Test
    fun `when SecurityException on device added then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray()) // Initially no devices
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        val callbackSlot = slot<MidiManager.DeviceCallback>()
        verifyRegisterDeviceCallback(midiManager, callbackSlot)
        val callback = callbackSlot.captured
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value) // Pre-condition

        // Now, setup the SecurityException for the next device scan
        midiManager.setupMockDevicesToThrow(SecurityException("Device list not available"))
        val newDeviceInfo = createMockDeviceInfo("New Problematic MIDI", "manufacturer", "product")

        // Act
        callback.onDeviceAdded(newDeviceInfo)

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals(context.getString(R.string.midi_error_no_permissions), (state as ConnectionState.Error).message)
    }

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
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Already Open MIDI", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()

        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)

        // Capture callbacks and simulate first successful connection
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        // Act
        // Trigger a re-open attempt by calling onDeviceAdded again
        val deviceCallback = deviceCallbackSlot.captured
        deviceCallback.onDeviceAdded(mockDeviceInfo)

        // Assert
        // Verify openDevice was called only once initially.
        // The second onDeviceAdded should result in a quick exit from openDevice, so no second call.
        verify(exactly = 1) { midiManager.openDevice(eq(mockDeviceInfo), any(), any()) }
        // Crucially, verify that the original device was never closed, proving we exited early.
        verify(exactly = 0) { mockDevice.close() }
    }

    @Test
    fun `when a connected device is removed then state is Disconnected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Connected MIDI", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)
        val deviceCallback = deviceCallbackSlot.captured

        // Act
        deviceCallback.onDeviceRemoved(mockDeviceInfo)

        // Assert
        assertEquals(ConnectionState.Disconnected, dataSource.connectionState.value)
        verify { mockDevice.close() }
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
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected) // Pre-condition

        val deviceCallback = deviceCallbackSlot.captured
        // Create info for a different device
        val otherDeviceInfo = createMockDeviceInfo("Other MIDI", "manufacturer", "product", id = 2)

        // Act
        deviceCallback.onDeviceRemoved(otherDeviceInfo) // Remove device 2

        // Assert
        // Verify the original device was not closed and the state remains Connected
        verify(exactly = 0) { mockDevice.close() }
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
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
    fun `when device connection is slow then state remains unchanged until connection completes`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray())
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        val mockDeviceInfo = createMockDeviceInfo("Slow MIDI", "manufacturer", "product")
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)

        // Before triggering onDeviceAdded, ensure that the MidiManager mock will report the new device.
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        // Act
        deviceCallbackSlot.captured.onDeviceAdded(mockDeviceInfo)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        assertEquals("State should not change before onDeviceOpened", ConnectionState.NoDevice, dataSource.connectionState.value)

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        openCallbackSlot.captured.onDeviceOpened(mockDevice)
        advanceTimeBy(1)

        // Assert
        assertTrue("State should be Connected after callback", dataSource.connectionState.value is ConnectionState.Connected)
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
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val deviceName = "Failing MIDI Device"
        val mockDeviceInfo = createMockDeviceInfo(deviceName, "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        // Act
        openCallbackSlot.captured.onDeviceOpened(null) // Simulate failure by passing null

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals(context.getString(R.string.midi_error_connection_failed, deviceName), (state as ConnectionState.Error).message)
    }

    @Test
    fun `when device open fails for device with no name then state is Error with unknown`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        // Create device with null name
        val mockDeviceInfo = createMockDeviceInfo(null, "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        val unknownDeviceName = context.getString(R.string.midi_unknown_device)

        // Act
        openCallbackSlot.captured.onDeviceOpened(null) // Simulate failure

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals(context.getString(R.string.midi_error_connection_failed, unknownDeviceName), (state as ConnectionState.Error).message)
    }

    @Test
    fun `when device opens successfully then state is Connected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Successful MIDI", "manufacturer", "product")
        val mockDomainDevice = mockk<MidiDeviceDomain>()
        every { midiDeviceMapper.toDomain(mockDeviceInfo) } returns mockDomainDevice
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        val mockDevice = mockk<MidiDevice>(relaxed = true)

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // Simulate success

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Connected)
        assertEquals(mockDomainDevice, (state as ConnectionState.Connected).device)
    }

    @Test
    fun `when device opens with no output ports then connect is not attempted`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with no output", "manufacturer", "product")
        // Ensure the device has no ports
        every { mockDeviceInfo.ports } returns emptyArray()
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        // Link the mock device back to its info
        every { mockDevice.info } returns mockDeviceInfo

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        // Verify that since there were no output ports, we never tried to open one.
        verify(exactly = 0) { mockDevice.openOutputPort(any()) }
    }

    @Test
    fun `when device opens with only input ports then connect is not attempted`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with only input", "manufacturer", "product")
        val mockInputPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_INPUT, 1)
        every { mockDeviceInfo.ports } returns arrayOf(mockInputPortInfo) // Only an input port
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

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
        // Arrange
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
        // Verify we tried to open the port but didn't proceed to connect a receiver
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
    }

    @Test
    fun `when output port opens successfully then receiver is connected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with good port", "manufacturer", "product")
        val mockPortInfo = createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 1)

        every { mockDeviceInfo.ports } returns arrayOf(mockPortInfo)
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }

        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo
        val mockOutputPort = mockk<MidiOutputPort>(relaxed = true)
        // Simulate successful port opening
        every { mockDevice.openOutputPort(1) } returns mockOutputPort

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        // Verify we opened the port and connected our receiver
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
        verify(exactly = 1) { mockOutputPort.connect(any()) }
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
        openCallbackSlot.captured.onDeviceOpened(mockDevice)

        // Assert
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
        verify(exactly = 1) { mockDevice.openOutputPort(1) }
        verify(exactly = 1) { mockOutputPort.connect(any<MidiReceiver>()) }
        // The main assertion is that no unhandled exception was thrown during the test execution.
    }

    //endregion

    //region Close Method Tests
    @Test
    fun `when close is called then unregisters callback and closes device`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Device to close", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))
        val mockDevice = mockk<MidiDevice>(relaxed = true)
        every { mockDevice.info } returns mockDeviceInfo

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // Make sure a device is open

        // Act
        dataSource.close()

        // Assert
        verify { midiManager.unregisterDeviceCallback(eq(deviceCallbackSlot.captured)) }
        verify { mockDevice.close() }
        assertEquals(ConnectionState.Disconnected, dataSource.connectionState.value)
    }

    @Test
    fun `when close is called and MidiManager is null then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, null)
        val dataSource = MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        // Act
        dataSource.close()

        // Assert
        // The main assertion is that no exception is thrown.
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
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
}