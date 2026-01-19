package com.astrizhachuk.pianoflow.data.repository

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.os.Bundle
import app.cash.turbine.test
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiMessageParser
import com.astrizhachuk.pianoflow.data.mapper.midi.MidiDeviceMapperImpl
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class MidiRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private lateinit var repository: MidiRepositoryImpl
    private lateinit var context: Context
    private lateinit var mapper: MidiDeviceMapperImpl

    @RelaxedMockK
    private lateinit var midiDataSource: MidiDataSource
    @RelaxedMockK
    private lateinit var midiManager: MidiManager
    @RelaxedMockK
    private lateinit var midiMessageParser: MidiMessageParser

    @Before
    fun setup() {
        // --- Integration Test Setup ---
        context = RuntimeEnvironment.getApplication()
        shadowOf(context.applicationContext as Application).setSystemService(Context.MIDI_SERVICE, midiManager)
        mapper = MidiDeviceMapperImpl()
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)

        // --- Unit Test Setup ---
        repository = MidiRepositoryImpl(midiDataSource)
    }
    
    // --- Unit Test ---
    
    @Test
    fun `observeConnectionState should proxy call to data source`() = runTest {
        // Arrange
        val expectedState = ConnectionState.NoDevice
        every { midiDataSource.connectionState } returns MutableStateFlow(expectedState)

        // Act
        val actualState = repository.observeConnectionState().first()

        // Assert
        assertEquals(expectedState, actualState)
    }

    @Test
    fun `observeNotes should proxy call to data source`() = runTest {
        // Arrange
        val expectedNote = Note(60)
        val notesFlow = MutableSharedFlow<Note>(replay = 1)
        notesFlow.tryEmit(expectedNote)
        every { midiDataSource.notes } returns notesFlow

        // Act
        val actualNote = repository.observeNotes().first()

        // Assert
        assertEquals(expectedNote, actualNote)
    }

    // --- Integration Tests ---

    private fun createRepository(): MidiRepositoryImpl {
        val dataSource = MidiDataSource(context, mapper, midiMessageParser)
        return MidiRepositoryImpl(dataSource)
    }

    @Test
    fun `given no devices, observeNotes should return empty flow`() = runTest {
        // Arrange
        every { midiManager.getDevicesForTransport(any()) } returns emptySet()
        val repository = createRepository()

        // Act & Assert
        repository.observeNotes().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given device is connected, observeNotes should be ready for MIDI input`() = runTest {
        // Arrange
        val mockDeviceInfo = createMockMidiDeviceInfo(1, "Test Keyboard", "product", "manufacturer")
        val mockNativeDevice = mockk<android.media.midi.MidiDevice>(relaxed = true)
        
        every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        every { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) } answers {
            openListenerSlot.captured.onDeviceOpened(mockNativeDevice)
        }

        // Act
        val repository = createRepository()

        // Assert - flow should be ready without errors
        repository.observeNotes().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given device is removed, observeNotes flow should continue to exist`() = runTest {
        // Arrange
        val mockDeviceInfo = createMockMidiDeviceInfo(1, "Test Keyboard", "product", "manufacturer")
        val mockNativeDevice = mockk<android.media.midi.MidiDevice>(relaxed = true)
        
        every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        every { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) } answers {
            openListenerSlot.captured.onDeviceOpened(mockNativeDevice)
        }

        val repository = createRepository()

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verify { midiManager.registerDeviceCallback(any(), any(), capture(deviceCallbackSlot)) }
        val callback = deviceCallbackSlot.captured

        // Act & Assert
        repository.observeNotes().test {
            callback.onDeviceRemoved(mockDeviceInfo)
            
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given MIDI feature unavailable, should emit correct error`() = runTest {
        // Arrange: Explicitly disable the MIDI feature
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, false)
        val repository = createRepository()

        // Act & Assert
        repository.observeConnectionState().test {
            val state = awaitItem()
            assertTrue("State should be Error, but was $state", state is ConnectionState.Error)
            assertEquals(context.getString(R.string.midi_error_api_unsupported), (state as ConnectionState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given no devices, observeConnectionState should emit NoDevice`() = runTest {
        // Arrange
        every { midiManager.getDevicesForTransport(any()) } returns emptySet()
        val repository = createRepository()

        // Act & Assert
        repository.observeConnectionState().test {
            assertEquals(ConnectionState.NoDevice, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given device is added, observeConnectionState should emit Connected`() = runTest {
        // Arrange
        every { midiManager.getDevicesForTransport(any()) } returns emptySet()
        val repository = createRepository()

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verify { midiManager.registerDeviceCallback(any(), any(), capture(deviceCallbackSlot)) }
        val callback = deviceCallbackSlot.captured

        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val mockDeviceInfo = createMockMidiDeviceInfo(1, "Test Keyboard", "MIDIKeys", "manufacturer")
        val mockNativeDevice = mockk<android.media.midi.MidiDevice>(relaxed = true)

        every { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) } answers {
            openListenerSlot.captured.onDeviceOpened(mockNativeDevice)
        }

        // Act & Assert
        repository.observeConnectionState().test {
            assertEquals("Initial state should be NoDevice", ConnectionState.NoDevice, awaitItem())

            // Update mock: now MidiManager should report the new device
            every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
            callback.onDeviceAdded(mockDeviceInfo)

            val state = awaitItem()
            assertTrue("State should be Connected, but was $state", state is ConnectionState.Connected)
            val connectedState = state as ConnectionState.Connected
            assertEquals(1, connectedState.device.id)
            assertEquals("Test Keyboard", connectedState.device.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given connected device is removed, observeConnectionState should emit Disconnected`() = runTest {
        // Arrange: Connect a device first
        val mockDeviceInfo = createMockMidiDeviceInfo(1, "Test Keyboard", "MIDIKeys", "manufacturer")
        val mockNativeDevice = mockk<android.media.midi.MidiDevice>(relaxed = true) {
            every { info } returns mockDeviceInfo
        }
        every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        every { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) } answers {
            openListenerSlot.captured.onDeviceOpened(mockNativeDevice)
        }

        // Create repository, which will trigger the initial connection
        val repository = createRepository()

        // Capture the device callback for later use
        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verify { midiManager.registerDeviceCallback(any(), any(), capture(deviceCallbackSlot)) }
        val callback = deviceCallbackSlot.captured

        // Act & Assert
        repository.observeConnectionState().test {
            val initialState = awaitItem()
            assertTrue("Initial state should be Connected, but was $initialState", initialState is ConnectionState.Connected)

            callback.onDeviceRemoved(mockDeviceInfo)

            assertEquals(ConnectionState.Disconnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given device fails to open, observeConnectionState should emit Error`() = runTest {
        // Arrange
        every { midiManager.getDevicesForTransport(any()) } returns emptySet()
        val repository = createRepository()

        val deviceCallbackSlot = slot<MidiManager.DeviceCallback>()
        verify { midiManager.registerDeviceCallback(any(), any(), capture(deviceCallbackSlot)) }
        val callback = deviceCallbackSlot.captured

        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        val mockDeviceInfo = createMockMidiDeviceInfo(1, "Failing Keyboard", "MIDIKeys", "manufacturer")

        every { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) } answers {
            openListenerSlot.captured.onDeviceOpened(null) // Simulate failure
        }

        // Act & Assert
        repository.observeConnectionState().test {
            assertEquals(ConnectionState.NoDevice, awaitItem())

            // Update mock: now MidiManager should report the new device
            every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
            callback.onDeviceAdded(mockDeviceInfo)

            val state = awaitItem()
            assertTrue("State should be Error, but was $state", state is ConnectionState.Error)
            val errorMsg = (state as ConnectionState.Error).message
            val expectedMsg = context.getString(R.string.midi_error_connection_failed, "Failing Keyboard")
            assertEquals(expectedMsg, errorMsg)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createMockMidiDeviceInfo(
        id: Int,
        name: String,
        product: String,
        manufacturer: String
    ): MidiDeviceInfo {
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_NAME, name)
            putString(MidiDeviceInfo.PROPERTY_PRODUCT, product)
            putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, manufacturer)
        }
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.properties } returns properties
        }
    }
}
