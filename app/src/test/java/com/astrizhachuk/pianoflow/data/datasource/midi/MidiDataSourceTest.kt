@file:Suppress("DEPRECATION")

package com.astrizhachuk.pianoflow.data.datasource.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice as MidiDeviceDomain
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.flow.first
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
        return mockDeviceInfo
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)

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
        val dataSource = MidiDataSource(context, midiDeviceMapper)

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
        val dataSource = MidiDataSource(context, midiDeviceMapper)

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
        MidiDataSource(context, midiDeviceMapper)

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
        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Assert
        val state = dataSource.connectionState.value
        // Expect that it gracefully sets to NoDevice as it can't find any
        assertEquals(ConnectionState.NoDevice, state)
    }

    //endregion

    //region DeviceCallback Tests

    @Test
    fun `when SecurityException on device added then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        midiManager.setupMockDevices(emptyArray()) // Initially no devices
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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

        val dataSource = MidiDataSource(context, midiDeviceMapper)

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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
    fun `when removed device has null info then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with null info", "manufacturer", "product")
        midiManager.setupMockDevices(arrayOf(mockDeviceInfo))

        // Create a mock device that will have a null `info` property
        val mockDeviceWithNullInfo = mockk<MidiDevice>(relaxed = true)
        every { mockDeviceWithNullInfo.info } returns null

        val openCallbackSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()

        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Capture the callback and simulate a connection
        verifyRegisterDeviceCallback(midiManager, deviceCallbackSlot)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        openCallbackSlot.captured.onDeviceOpened(mockDeviceWithNullInfo)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        // The device that is being "removed" by the system
        val removedDeviceInfo = createMockDeviceInfo("Some other device", "manufacturer", "product")
        val callback = deviceCallbackSlot.captured

        // Act
        callback.onDeviceRemoved(removedDeviceInfo)

        // Assert
        // Verify that because openedDevice.info is null, the close method is never reached.
        verify(exactly = 0) { mockDeviceWithNullInfo.close() }
        // And the state remains connected because the check did not pass
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
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

        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openCallbackSlot), any()) }
        val mockDevice = mockk<MidiDevice>(relaxed = true)

        // Act
        openCallbackSlot.captured.onDeviceOpened(mockDevice) // Simulate success

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Connected)
        assertEquals(mockDomainDevice, (state as ConnectionState.Connected).device)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
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
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        // Act
        dataSource.close()

        // Assert
        // The main assertion is that no exception is thrown.
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
    }
    //endregion
}
