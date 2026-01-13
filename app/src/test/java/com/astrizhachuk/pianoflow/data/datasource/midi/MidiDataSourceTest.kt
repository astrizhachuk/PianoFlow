package com.astrizhachuk.pianoflow.data.datasource.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Looper
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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

    private val deviceCallbackCaptor = argumentCaptor<MidiManager.DeviceCallback>()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context as Application).setSystemService(Context.MIDI_SERVICE, midiManager)
    }

    @Test
    fun `when MIDI feature is not supported on init then state is Error`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, false)

        val dataSource = MidiDataSource(context, midiDeviceMapper)

        val state = dataSource.connectionState.first()
        assertTrue(state is ConnectionState.Error)
        assertEquals("MIDI API не поддерживается на этом устройстве.", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when no devices are available on init then state is NoDevice`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())

        val dataSource = MidiDataSource(context, midiDeviceMapper)

        val state = dataSource.connectionState.first()
        assertEquals(ConnectionState.NoDevice, state)
    }

    @Test
    fun `when a device is available on init then it is opened`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Test MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))

        MidiDataSource(context, midiDeviceMapper)

        verify(midiManager).openDevice(eq(mockDeviceInfo), any(), anyOrNull())
    }

    @Test
    fun `when a new device is added then it is opened`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        whenever(midiManager.devices).thenReturn(emptyArray())

        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())
        assertEquals(ConnectionState.NoDevice, dataSource.connectionState.value)

        val newDeviceInfo = createMockDeviceInfo("New MIDI")
        whenever(midiManager.devices).thenReturn(arrayOf(newDeviceInfo))
        deviceCallbackCaptor.firstValue.onDeviceAdded(newDeviceInfo)

        verify(midiManager).openDevice(eq(newDeviceInfo), any(), anyOrNull())
    }

    @Test
    fun `when device opens successfully then state is Connected`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Test MIDI")
        val mockMidiDevice = mock<MidiDevice>()
        whenever(midiManager.devices).thenReturn(emptyArray())

        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())

        doAnswer { invocation ->
            val listener = invocation.getArgument<MidiManager.OnDeviceOpenedListener>(1)
            listener.onDeviceOpened(mockMidiDevice)
            null
        }.whenever(midiManager).openDevice(eq(mockDeviceInfo), any(), anyOrNull())

        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())

        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        deviceCallbackCaptor.firstValue.onDeviceAdded(mockDeviceInfo)
        shadowOf(Looper.getMainLooper()).idle()

        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Connected)
    }

    @Test
    fun `when device open fails then state is Error`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Failing MIDI")
        whenever(midiManager.devices).thenReturn(emptyArray())

        doAnswer { invocation ->
            val listener = invocation.getArgument<MidiManager.OnDeviceOpenedListener>(1)
            listener.onDeviceOpened(null)
            null
        }.whenever(midiManager).openDevice(eq(mockDeviceInfo), any(), anyOrNull())

        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())

        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        deviceCallbackCaptor.firstValue.onDeviceAdded(mockDeviceInfo)
        shadowOf(Looper.getMainLooper()).idle()

        val state = dataSource.connectionState.value
        assertTrue(state is ConnectionState.Error)
        assertEquals("Не удалось подключиться к устройству: Failing MIDI", (state as ConnectionState.Error).message)
    }

    @Test
    fun `when a connected device is removed then state is Disconnected`() = runTest {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
        val mockDeviceInfo = createMockDeviceInfo("Connected MIDI", 123)
        val mockMidiDevice = mock<MidiDevice>()
        whenever(mockMidiDevice.info).thenReturn(mockDeviceInfo)
        whenever(midiManager.devices).thenReturn(emptyArray())

        whenever(midiDeviceMapper.toDomain(any())).thenReturn(mock())

        doAnswer { invocation ->
            val listener = invocation.getArgument<MidiManager.OnDeviceOpenedListener>(1)
            listener.onDeviceOpened(mockMidiDevice)
            null
        }.whenever(midiManager).openDevice(eq(mockDeviceInfo), any(), anyOrNull())

        val dataSource = MidiDataSource(context, midiDeviceMapper)
        verify(midiManager).registerDeviceCallback(deviceCallbackCaptor.capture(), any())

        whenever(midiManager.devices).thenReturn(arrayOf(mockDeviceInfo))
        deviceCallbackCaptor.firstValue.onDeviceAdded(mockDeviceInfo)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Pre-condition failed: Device did not connect", dataSource.connectionState.value is ConnectionState.Connected)

        deviceCallbackCaptor.firstValue.onDeviceRemoved(mockDeviceInfo)
        shadowOf(Looper.getMainLooper()).idle()

        verify(mockMidiDevice).close()
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
