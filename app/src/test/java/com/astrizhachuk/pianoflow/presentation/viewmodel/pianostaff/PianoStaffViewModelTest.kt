
package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.util.MainDispatcherRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PianoStaffViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var observeMidiMessagesUseCase: ObserveMidiMessagesUseCase

    private lateinit var midiMessagesFlow: MutableSharedFlow<List<Note>>

    @Before
    fun setUp() {
        midiMessagesFlow = MutableSharedFlow()
        every { observeMidiMessagesUseCase() } returns midiMessagesFlow
    }

    @Test
    fun `uiState starts with initial empty value`() = runTest {
        // Arrange
        val viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)

        // Assert
        viewModel.uiState.test {
            assertEquals(emptyList<Note>(), awaitItem().notes)
        }
    }

    @Test
    fun `when use case emits new notes then uiState is updated`() = runTest {
        // Arrange
        val viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)
        val note1 = mockk<Note>()
        val note2 = mockk<Note>()
        val notes = listOf(note1, note2)

        viewModel.uiState.test {
            // Initial empty state
            awaitItem()

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            assertEquals(notes, awaitItem().notes)
        }
    }

    @Test
    fun `when use case emits an empty list then uiState is updated with empty notes`() = runTest {
        // Arrange
        val viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)
        val initialNotes = listOf(mockk<Note>())

        viewModel.uiState.test {
            awaitItem()

            // Emit some notes first to make sure the state changes
            midiMessagesFlow.emit(initialNotes)
            assertEquals(initialNotes, awaitItem().notes)

            // Act
            val emptyNotes = emptyList<Note>()
            midiMessagesFlow.emit(emptyNotes)

            // Assert
            assertEquals(emptyNotes, awaitItem().notes)
        }
    }

    @Test
    fun `rapid sequential note emissions are processed correctly`() = runTest {
        // Arrange
        val viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)
        val finalNotes = listOf(mockk<Note>())

        viewModel.uiState.test {
            awaitItem()

            // Act
            midiMessagesFlow.emit(listOf(mockk(), mockk()))
            assertEquals(2, awaitItem().notes.size)

            midiMessagesFlow.emit(listOf(mockk()))
            assertEquals(1, awaitItem().notes.size)

            midiMessagesFlow.emit(finalNotes)
            assertEquals(finalNotes, awaitItem().notes)
        }
    }
}
