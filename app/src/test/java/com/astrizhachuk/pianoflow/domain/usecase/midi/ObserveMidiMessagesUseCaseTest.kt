package com.astrizhachuk.pianoflow.domain.usecase.midi

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
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
}
