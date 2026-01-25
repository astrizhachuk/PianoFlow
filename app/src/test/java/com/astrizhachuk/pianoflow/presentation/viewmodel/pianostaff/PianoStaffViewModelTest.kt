package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson
import com.astrizhachuk.pianoflow.util.MainDispatcherRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
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

    private lateinit var viewModel: PianoStaffViewModel

    private val mapperFile = "com.astrizhachuk.pianoflow.presentation.ui.pianostaff.VexflowNoteMapperKt"

    @Before
    fun setUp() {
        midiMessagesFlow = MutableSharedFlow()
        every { observeMidiMessagesUseCase() } returns midiMessagesFlow
        // Мокируем статическую функцию-расширение
        mockkStatic(mapperFile)
        viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)
    }

    @After
    fun tearDown() {
        // Отменяем мок после каждого теста
        unmockkStatic(mapperFile)
    }

    @Test
    fun `uiState starts with initial empty json state`() = runTest {
        // Assert
        viewModel.uiState.test {
            val expectedState = PianoStaffUiState(notesJson = "{\"treble\":[], \"bass\":[]}")
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when notes are emitted, ViewModel uses mapper to update notesJson`() = runTest {
        // Arrange
        val notes = listOf(Note(pitch = 59), Note(pitch = 60))
        val expectedJson = "mock_json_for_notes"

        // Задаем поведение для мока: при вызове toVexflowJson с любым списком возвращать заглушку
        every { notes.toVexflowJson() } returns expectedJson

        viewModel.uiState.test {
            assertEquals(PianoStaffUiState(), awaitItem()) // Проверяем начальное состояние

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            val newState = awaitItem()
            assertEquals(expectedJson, newState.notesJson)
        }
    }

    @Test
    fun `when use case emits an empty list then uiState is updated with empty json`() = runTest {
        // Arrange
        val initialNotes = listOf(Note(pitch = 60))
        val initialJson = "initial_json"
        val emptyJson = "{\"treble\":[], \"bass\":[]}"

        every { initialNotes.toVexflowJson() } returns initialJson
        every { emptyList<Note>().toVexflowJson() } returns emptyJson

        viewModel.uiState.test {
            assertEquals(PianoStaffUiState(), awaitItem())

            // Сначала эмитируем непустой список, чтобы состояние изменилось
            midiMessagesFlow.emit(initialNotes)
            assertEquals(initialJson, awaitItem().notesJson)

            // Act: Эмитируем пустой список
            midiMessagesFlow.emit(emptyList())

            // Assert
            val finalState = awaitItem()
            assertEquals(emptyJson, finalState.notesJson)
        }
    }
}
