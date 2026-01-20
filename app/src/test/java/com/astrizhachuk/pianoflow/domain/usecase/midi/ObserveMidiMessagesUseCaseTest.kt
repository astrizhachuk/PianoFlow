package com.astrizhachuk.pianoflow.domain.usecase.midi

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveMidiMessagesUseCaseTest {

    private companion object {
        const val PITCH_C4 = 60
        const val PITCH_E4 = 64
        const val PITCH_G4 = 67
        const val CHORD_WINDOW_MS = 50L
    }

    private val midiRepository: MidiRepository = mockk()
    private val useCase = ObserveMidiMessagesUseCase(midiRepository)

    @Test
    fun `invoke with single note returns single note`() = runTest {
        // Arrange
        val note = Note(PITCH_C4)
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
        val chord = listOf(Note(PITCH_C4), Note(PITCH_E4), Note(PITCH_G4))

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
        val chord = listOf(Note(PITCH_C4), Note(PITCH_E4))
        val singleNote = Note(PITCH_G4)

        // Act & Assert
        useCase.invoke().test {
            // Отправляем аккорд
            chord.forEach { notesChannel.send(it) }
            // Проверяем, что аккорд пришел как одна группа
            assertEquals("Chord should be emitted first", chord, awaitItem())

            // Отправляем отдельную ноту
            notesChannel.send(singleNote)
            // Проверяем, что нота пришла как новая, отдельная группа
            assertEquals("Single note should be emitted second", listOf(singleNote), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with notes inside chord window returns single chord`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val note1 = Note(PITCH_C4)
        val note2 = Note(PITCH_E4)
        val note3 = Note(PITCH_G4)
        val chord = listOf(note1, note2, note3)

        // Act & Assert
        useCase.invoke().test {
            notesChannel.send(note1)
            delay(CHORD_WINDOW_MS - 10)
            notesChannel.send(note2)
            delay(CHORD_WINDOW_MS - 10)
            notesChannel.send(note3)

            assertEquals(chord, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke with notes outside chord window returns separate notes`() = runTest {
        // Arrange
        val notesChannel = Channel<Note>(Channel.UNLIMITED)
        coEvery { midiRepository.observeNotes() } returns notesChannel.receiveAsFlow()
        val note1 = Note(PITCH_C4)
        val note2 = Note(PITCH_E4)
        val note3 = Note(PITCH_G4)

        // Act & Assert
        useCase.invoke().test {
            notesChannel.send(note1)
            assertEquals(listOf(note1), awaitItem())

            delay(CHORD_WINDOW_MS + 1)
            notesChannel.send(note2)
            assertEquals(listOf(note2), awaitItem())

            delay(CHORD_WINDOW_MS + 1)
            notesChannel.send(note3)
            assertEquals(listOf(note3), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
