package com.astrizhachuk.pianoflow.data.datasource.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
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
        val mockDeviceInfo = createMockDeviceInfo("Permission Denied MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiManager.openDevice(eq(mockDeviceInfo), any(), anyOrNull()))
            .thenThrow(SecurityException("Caller does not have permission to open device."))

        // Act
        val dataSource = MidiDataSource(context, midiDeviceMapper)

        // Assert
        val state = dataSource.connectionState.first()
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
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())
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
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())
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
        // Trigger a re-open attempt with the same device info
        val deviceCallback = deviceCallbackCaptor.value
        deviceCallback.onDeviceAdded(mockDeviceInfo)

        // Assert
        // Verify openDevice was called only once (the first time) and not again.
        verify(midiManager, times(1)).openDevice(any(), any(), anyOrNull())
        // Verify the existing device was never closed
        verify(mockDevice, never()).close()
    }

    @Test
    fun `when a connected device is removed then state is Disconnected`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Connected MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())
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
    fun `when close is called then resources are released`() = runTest {
        // Arrange
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Device to Close")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())
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

    private fun createMockDeviceInfo(name: String, id: Int = 1): MidiDeviceInfo {
        val properties = mock<android.os.Bundle>()
        whenever(properties.getString(MidiDeviceInfo.PROPERTY_NAME)).thenReturn(name)
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(properties)
        whenever(deviceInfo.id).thenReturn(id)
        return deviceInfo
    }
}
