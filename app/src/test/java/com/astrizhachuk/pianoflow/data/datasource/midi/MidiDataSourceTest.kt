package com.astrizhachuk.pianoflow.data.datasource.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import com.astrizhachuk.pianoflow.domain.model.MidiDevice as MidiDeviceDomain

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [23])
class MidiDataSourceTest {

    private lateinit var context: Context
    private val midiManager = mock<MidiManager>()
    private val midiDeviceMapper = mock<MidiDeviceMapper>()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, midiManager)
    }

    @Test
    fun `when MIDI feature is not supported on init then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, false)

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Assert
        val state = dataSource.connectionState.first()
        assertTrue(state is ConnectionState.Error)
        assertEquals("MIDI API не поддерживается на этом устройстве.", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when MIDI permission is missing on open then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenThrow(SecurityException("Caller does not have permission to open device."))

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Assert
        val state = dataSource.connectionState.first()
        assertTrue(state is ConnectionState.Error)
        assertEquals("Отсутствуют необходимые разрешения для работы с MIDI.", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when SecurityException on device added then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        val callbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        verify(midiManager).registerDeviceCallback(callbackCaptor.capture(), any())
        val callback = callbackCaptor.value
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value) // Pre-condition

        // Now, setup the SecurityException
        whenever(midiManager.devices).thenThrow(SecurityException("Device list not available"))
        val newDeviceInfo = createMockDeviceInfo("New Problematic MIDI")

        // Act
        callback.onDeviceAdded(newDeviceInfo)

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals("Отсутствуют необходимые разрешения для работы с MIDI.", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when device open fails then state is Error`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Failing MIDI Device")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())

        // Act
        openCallbackCaptor.value.onDeviceOpened(null) // Simulate failure by passing null

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals("Не удалось подключиться к устройству: Failing MIDI Device", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when device open fails for device with no name then state is Error with unknown`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        // Create device with null name
        val properties = mock<android.os.Bundle>()
        whenever(properties.getString(MidiDeviceInfo.PROPERTY_NAME)).thenReturn(null)
        val mockDeviceInfo = mock<MidiDeviceInfo>()
        whenever(mockDeviceInfo.properties).thenReturn(properties)

        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())

        // Act
        openCallbackCaptor.value.onDeviceOpened(null) // Simulate failure

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals("Не удалось подключиться к устройству: Unknown Device", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when no devices are available on init then state is NoDevice`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())

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
        val mockDeviceInfo = createMockDeviceInfo("Initial Available MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))

        // Act
        MidiDataSource(context, midiDeviceMapper)

        // Assert
        verify(midiManager).openDevice(eq(mockDeviceInfo), any(), anyOrNull())
    }

    @Test
    fun `when a new device is added then it is opened`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        val callbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        verify(midiManager).registerDeviceCallback(callbackCaptor.capture(), any())
        val callback = callbackCaptor.value
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
        val newDeviceInfo = createMockDeviceInfo("New MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(newDeviceInfo))

        // Act
        callback.onDeviceAdded(newDeviceInfo)

        // Assert
        verify(midiManager).openDevice(eq(newDeviceInfo), any(), anyOrNull())
    }

    @Test
    fun `when device opens successfully then state is Connected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Successful Connection MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock<MidiDeviceDomain>())
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())

        // Act
        openCallbackCaptor.value.onDeviceOpened(mock())

        // Assert
        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Connected)
    }

    @Test
    fun `when opening an already opened device then it does not re-open`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Already Open MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock<MidiDeviceDomain>())
        val mockDevice = mock<MidiDevice>()
        whenever(mockDevice.info).thenReturn(mockDeviceInfo)

        val deviceCallbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)

        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Capture callbacks and simulate first successful connection
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())
        openCallbackCaptor.value.onDeviceOpened(mockDevice)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        // Act
        // Trigger a re-open attempt by calling onDeviceAdded again
        val deviceCallback = deviceCallbackCaptor.value
        deviceCallback.onDeviceAdded(mockDeviceInfo)

        // Assert
        // Verify openDevice was called only once initially.
        // The second onDeviceAdded should result in a quick exit from openDevice, so no second call.
        verify(midiManager, times(1)).openDevice(any(), any(), anyOrNull())
        // Crucially, verify that the original device was never closed, proving we exited early.
        verify(mockDevice, never()).close()
    }

    @Test
    fun `when a connected device is removed then state is Disconnected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Connected MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock<MidiDeviceDomain>())
        val mockDevice = mock<MidiDevice>()
        whenever(mockDevice.info).thenReturn(mockDeviceInfo)
        val deviceCallbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())
        openCallbackCaptor.value.onDeviceOpened(mockDevice)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)
        val deviceCallback = deviceCallbackCaptor.value

        // Act
        deviceCallback.onDeviceRemoved(mockDeviceInfo)

        // Assert
        assertEquals(ConnectionState.Disconnected, dataSource.connectionState.value)
        verify(mockDevice).close()
    }

    @Test
    fun `when removed device has null info then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("MIDI with null info")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())

        // Create a mock device that will have a null `info` property
        val mockDeviceWithNullInfo = mock<MidiDevice>()
        whenever(mockDeviceWithNullInfo.info).thenReturn(null)

        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val deviceCallbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)

        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Capture the callback and simulate a connection
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())
        openCallbackCaptor.value.onDeviceOpened(mockDeviceWithNullInfo)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        // The device that is being "removed" by the system
        val removedDeviceInfo = createMockDeviceInfo("Some other device")
        val callback = deviceCallbackCaptor.value

        // Act
        callback.onDeviceRemoved(removedDeviceInfo)

        // Assert
        // Verify that because openedDevice.info is null, the close method is never reached.
        verify(mockDeviceWithNullInfo, never()).close()
        // And the state remains connected because the check did not pass
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
    }

    @Test
    fun `when a non-connected device is removed then nothing happens`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        // Setup a connected device
        val connectedDeviceInfo = createMockDeviceInfo("Connected MIDI", id = 1)
        val mockDevice = mock<MidiDevice>()
        whenever(mockDevice.info).thenReturn(connectedDeviceInfo)
        whenever(midiManager.devices).thenReturn(arrayOf(connectedDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock<MidiDeviceDomain>()) // FIX
        val deviceCallbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)

        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())
        verify(midiManager).openDevice(any(), openCallbackCaptor.capture(), anyOrNull())
        openCallbackCaptor.value.onDeviceOpened(mockDevice) // Connect device 1
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected) // Pre-condition

        val deviceCallback = deviceCallbackCaptor.value
        // Create info for a different device
        val otherDeviceInfo = createMockDeviceInfo("Other MIDI", id = 2)

        // Act
        deviceCallback.onDeviceRemoved(otherDeviceInfo) // Remove device 2

        // Assert
        // Verify the original device was not closed and the state remains Connected
        verify(mockDevice, never()).close()
        assertTrue(dataSource.connectionState.value is ConnectionState.Connected)
    }

    @Test
    fun `when device is removed but nothing was open then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())
        val callbackCaptor = ArgumentCaptor.forClass(MidiManager.DeviceCallback::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(callbackCaptor.capture(), any())
        val callback = callbackCaptor.value
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value) // Pre-condition
        val removedDeviceInfo = createMockDeviceInfo("Removed MIDI")

        // Act
        callback.onDeviceRemoved(removedDeviceInfo)

        // Assert
        // Verify state remains NoDevice
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
    }

    @Test
    fun `when close is called then resources are released`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Device to Close")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock<MidiDeviceDomain>())
        val mockDevice = mock<MidiDevice>()
        whenever(mockDevice.info).thenReturn(mockDeviceInfo)

        val openCallbackCaptor = ArgumentCaptor.forClass(MidiManager.OnDeviceOpenedListener::class.java)
        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Simulate a successful connection to have something to close
        verify(midiManager).openDevice(eq(mockDeviceInfo), openCallbackCaptor.capture(), anyOrNull())
        openCallbackCaptor.value.onDeviceOpened(mockDevice)
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        // Act
        dataSource.close()

        // Assert
        verify(midiManager).unregisterDeviceCallback(any())
        verify(mockDevice).close()
        assertEquals(ConnectionState.Disconnected, dataSource.connectionState.value)
    }

    @Test
    fun `when close is called with no device open then it does not crash`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())
        val dataSource = MidiDataSource(context, midiDeviceMapper)
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        // Act & Assert
        dataSource.close()
        verify(midiManager).unregisterDeviceCallback(any())
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)
    }

    private fun createMockDeviceInfo(name: String, id: Int = 1): MidiDeviceInfo {
        val properties = mock<android.os.Bundle>()
        whenever(properties.getString(MidiDeviceInfo.PROPERTY_NAME)).thenReturn(name)
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(properties)
        whenever(deviceInfo.id).thenReturn(id)
        return deviceInfo
    }
}
