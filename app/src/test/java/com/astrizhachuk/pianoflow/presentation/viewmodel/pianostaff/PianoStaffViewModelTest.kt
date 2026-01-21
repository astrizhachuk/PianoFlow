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
        viewModel = PianoStaffViewModel(observeMidiMessagesUseCase)
        // Мокируем статическую функцию-расширение
        mockkStatic(mapperFile)
    }

    @After
    fun tearDown() {
        // Отменяем мок после каждого теста, чтобы не влиять на другие тесты
        unmockkStatic(mapperFile)
    }

    @Test
    fun `uiState starts with initial empty json state`() = runTest {
        // Assert
        viewModel.uiState.test {
            val expectedState = PianoStaffUiState(trebleNotesJson = "[]", bassNotesJson = "[]")
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `when notes are emitted, ViewModel partitions them and uses mapper`() = runTest {
        // Arrange
        val bassNote = Note(pitch = 59)
        val trebleNote = Note(pitch = 60)
        val notes = listOf(bassNote, trebleNote)

        // Задаем поведение для мока: при вызове toVexflowJson возвращать заглушки
        every { listOf(bassNote).toVexflowJson() } returns "bass_json_mock"
        every { listOf(trebleNote).toVexflowJson() } returns "treble_json_mock"

        viewModel.uiState.test {
            assertEquals(PianoStaffUiState(), awaitItem()) // Проверяем начальное состояние

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            // Проверяем, что ViewModel правильно использовала результаты от мок-маппера
            val newState = awaitItem()
            assertEquals("treble_json_mock", newState.trebleNotesJson)
            assertEquals("bass_json_mock", newState.bassNotesJson)
        }
    }

    @Test
    fun `when only treble notes are emitted, bass json is empty`() = runTest {
        // Arrange
        val trebleChord = listOf(Note(60), Note(64), Note(67))
        val emptyBassList = emptyList<Note>()

        every { trebleChord.toVexflowJson() } returns "chord_json_mock"
        // Логика для пустого списка простая, но для чистоты изоляции мокируем и ее
        every { emptyBassList.toVexflowJson() } returns "[]"

        viewModel.uiState.test {
            assertEquals(PianoStaffUiState(), awaitItem())

            // Act
            midiMessagesFlow.emit(trebleChord)

            // Assert
            val newState = awaitItem()
            assertEquals("chord_json_mock", newState.trebleNotesJson)
            assertEquals("[]", newState.bassNotesJson)
        }
    }

    @Test
    fun `when use case emits an empty list then uiState is updated with empty json`() = runTest {
        // Этот тест не требует сложной логики моков, так как проверяет простой путь,
        // но мы оставляем мок для консистентности

        // Arrange
        every { listOf(Note(pitch = 60)).toVexflowJson() } returns "any_json"
        every { emptyList<Note>().toVexflowJson() } returns "[]"

        viewModel.uiState.test {
            assertEquals(PianoStaffUiState(), awaitItem())

            // Сначала эмитируем непустой список, чтобы состояние изменилось
            midiMessagesFlow.emit(listOf(Note(pitch = 60)))
            awaitItem() // Пропускаем это состояние

            // Act: Эмитируем пустой список
            midiMessagesFlow.emit(emptyList())

            // Assert
            val finalState = awaitItem()
            assertEquals("[]", finalState.trebleNotesJson)
            assertEquals("[]", finalState.bassNotesJson)
        }
    }
}
