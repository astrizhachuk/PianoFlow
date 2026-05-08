@file:Suppress("DEPRECATION")

package com.astrizhachuk.pianoflow.domain.usecase.midi

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Bundle
import app.cash.turbine.test
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiMessageParser
import com.astrizhachuk.pianoflow.data.mapper.midi.MidiDeviceMapperImpl
import com.astrizhachuk.pianoflow.data.repository.MidiRepositoryImpl
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class ObserveMidiMessagesUseCaseTest {

    private companion object {
        const val PITCH_C4 = 60
        const val PITCH_E4 = 64
        const val PITCH_G4 = 67
        const val CHORD_WINDOW_MS = 50L
        
        val NOTE_C4 = Note(PITCH_C4, "C4")
        val NOTE_E4 = Note(PITCH_E4, "E4")
        val NOTE_G4 = Note(PITCH_G4, "G4")
    }

    private val midiRepository: MidiRepository = mockk()
    private val useCase = ObserveMidiMessagesUseCase(midiRepository)

    @Test
    fun `invoke with single note returns single note`() = runTest {
        // Arrange
        val note = NOTE_C4
        coEvery { midiRepository.observeNotes() } returns flowOf(note)

        // Act & Assert
        useCase.invoke().test {
            assertEquals(listOf(note), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke with multiple notes in chord window returns a chord`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val chord = listOf(NOTE_C4, NOTE_E4, NOTE_G4)

        // Act & Assert
        useCase.invoke().test {
            chord.forEach { notesChannel.send(it) }
            assertEquals(chord, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with a chord then a single note returns separate lists`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val chord = listOf(NOTE_C4, NOTE_E4)
        val singleNote = NOTE_G4

        // Act & Assert
        useCase.invoke().test {
            // Act
            chord.forEach { notesChannel.send(it) }
            // Assert
            assertEquals("Chord should be emitted first", chord, awaitItem())

            // Act
            notesChannel.send(singleNote)
            // Assert
            assertEquals("Single note should be emitted second", listOf(singleNote), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with notes inside chord window returns single chord`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val note1 = NOTE_C4
        val note2 = NOTE_E4
        val note3 = NOTE_G4
        val chord = listOf(note1, note2, note3)

        // Act & Assert
        useCase.invoke().test {
            // Act
            notesChannel.send(note1)
            delay(CHORD_WINDOW_MS - 10)
            notesChannel.send(note2)
            delay(CHORD_WINDOW_MS - 10)
            notesChannel.send(note3)

            // Assert
            assertEquals(chord, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with notes outside chord window returns separate notes`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val note1 = NOTE_C4
        val note2 = NOTE_E4
        val note3 = NOTE_G4

        // Act & Assert
        useCase.invoke().test {
            // Act
            notesChannel.send(note1)
            // Assert
            assertEquals(listOf(note1), awaitItem())

            // Act
            delay(CHORD_WINDOW_MS + 1)
            notesChannel.send(note2)
            // Assert
            assertEquals(listOf(note2), awaitItem())

            // Act
            delay(CHORD_WINDOW_MS + 1)
            notesChannel.send(note3)
            // Assert
            assertEquals(listOf(note3), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, Build.VERSION_CODES.TIRAMISU])
class ObserveMidiMessagesIntegrationTest {

    private companion object {
        const val PITCH_C4 = 60
        const val VELOCITY_100 = 100
        const val NOTE_ON_CHANNEL_0: Byte = 0x90.toByte()
    }

    @get:Rule
    val mockkRule = MockKRule(this)

    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var midiManager: MidiManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context.applicationContext as Application).setSystemService(Context.MIDI_SERVICE, midiManager)
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_MIDI, true)
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

    @Test
    fun `midi note on message is received and processed correctly`() = runTest {
        // Arrange
        val receiverSlot = slot<MidiReceiver>()
        val mockDeviceInfo = mockk<MidiDeviceInfo>(relaxed = true) {
            every { properties } returns Bundle().apply {
                putString(MidiDeviceInfo.PROPERTY_NAME, "Test Keyboard")
            }
            every { ports } returns arrayOf(createMockPortInfo(MidiDeviceInfo.PortInfo.TYPE_OUTPUT, 0))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every { midiManager.getDevicesForTransport(any()) } returns setOf(mockDeviceInfo)
        } else {
            every { midiManager.devices } returns arrayOf(mockDeviceInfo)
        }

        val mockOutputPort = mockk<android.media.midi.MidiOutputPort>(relaxed = true) {
            every { connect(capture(receiverSlot)) } returns Unit
        }

        val mockNativeDevice = mockk<android.media.midi.MidiDevice>(relaxed = true) {
            every { openOutputPort(any()) } returns mockOutputPort
            every { info } returns mockDeviceInfo
        }

        // This will trigger the call to midiManager.openDevice
        val dataSource = MidiDataSource(context, MidiDeviceMapperImpl(), MidiMessageParser())
        val repository = MidiRepositoryImpl(dataSource)
        val useCase = ObserveMidiMessagesUseCase(repository)

        // Now, verify the call and capture the listener
        val openListenerSlot = slot<MidiManager.OnDeviceOpenedListener>()
        verify { midiManager.openDevice(eq(mockDeviceInfo), capture(openListenerSlot), any()) }

        // Manually trigger the callback to continue the connection flow
        openListenerSlot.captured.onDeviceOpened(mockNativeDevice)

        // Now the receiver should be captured
        val capturedReceiver = receiverSlot.captured

        val noteOnMessage = byteArrayOf(NOTE_ON_CHANNEL_0, PITCH_C4.toByte(), VELOCITY_100.toByte())
        val expectedNote = Note(pitch = PITCH_C4, name = "C4")

        // Act & Assert
        useCase.invoke().test {
            capturedReceiver.send(noteOnMessage, 0, noteOnMessage.size)

            // Assert
            assertEquals(listOf(expectedNote), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
