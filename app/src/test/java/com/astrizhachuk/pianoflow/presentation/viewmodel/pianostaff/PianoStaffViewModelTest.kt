package com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff

import android.content.Context
import app.cash.turbine.test
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.usecase.analysis.AnalyzeChordUseCase
import com.astrizhachuk.pianoflow.domain.usecase.analysis.ObserveChordAnalysisResultsUseCase
import com.astrizhachuk.pianoflow.domain.usecase.midi.ObserveMidiMessagesUseCase
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.toVexflowJson
import com.astrizhachuk.pianoflow.util.MainDispatcherRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @RelaxedMockK
    private lateinit var context: Context

    @MockK
    private lateinit var observeMidiMessagesUseCase: ObserveMidiMessagesUseCase

    @MockK
    private lateinit var analyzeChordUseCase: AnalyzeChordUseCase

    @MockK
    private lateinit var observeChordAnalysisResultsUseCase: ObserveChordAnalysisResultsUseCase

    private lateinit var midiMessagesFlow: MutableSharedFlow<List<Note>>
    private lateinit var chordAnalysisFlow: MutableStateFlow<String?>

    private lateinit var viewModel: PianoStaffViewModel

    private val mapperFile = "com.astrizhachuk.pianoflow.presentation.ui.pianostaff.VexflowNoteMapperKt"
    private val undefinedChordString = "Not defined"

    @Before
    fun setUp() {
        midiMessagesFlow = MutableSharedFlow()
        chordAnalysisFlow = MutableStateFlow(null)

        every { observeMidiMessagesUseCase() } returns midiMessagesFlow
        every { observeChordAnalysisResultsUseCase() } returns chordAnalysisFlow
        every { analyzeChordUseCase(any()) } returns Unit
        every { context.getString(R.string.chord_not_defined) } returns undefinedChordString

        // Мокируем статическую функцию-расширение
        mockkStatic(mapperFile)
    }

    private fun initViewModel() {
        viewModel = PianoStaffViewModel(
            context,
            observeMidiMessagesUseCase,
            analyzeChordUseCase,
            observeChordAnalysisResultsUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(mapperFile)
    }

    @Test
    fun `uiState starts with initial empty state`() = runTest {
        // Arrange
        val emptyJson = "{\"treble\":[], \"bass\":[]}"
        every { emptyList<Note>().toVexflowJson() } returns emptyJson
        
        // Act
        initViewModel()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(emptyJson, state.notesJson)
            assertEquals(null, state.chordName)
        }
    }

    @Test
    fun `when notes are emitted, ViewModel triggers analysis and updates JSON`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4"))
        val expectedJson = "mock_json"
        every { notes.toVexflowJson() } returns expectedJson

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            val state = awaitItem()
            assertEquals(expectedJson, state.notesJson)
            verify { analyzeChordUseCase(notes) }
        }
    }

    @Test
    fun `when analysis result is updated, uiState reflects the chord name`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4"), Note(64, "E4"), Note(67, "G4"))
        val analysisResult = "C Major"
        
        every { notes.toVexflowJson() } returns "json"

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act
            midiMessagesFlow.emit(notes)
            awaitItem() // Skip update from notes
            
            chordAnalysisFlow.value = analysisResult

            // Assert
            assertEquals(analysisResult, awaitItem().chordName)
        }
    }

    @Test
    fun `when notes exist but analysis is null, show undefined chord string`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4"))
        every { notes.toVexflowJson() } returns "json"
        chordAnalysisFlow.value = null

        viewModel.uiState.test {
            awaitItem() // Skip initial

            // Act
            midiMessagesFlow.emit(notes)

            // Assert
            assertEquals(undefinedChordString, awaitItem().chordName)
        }
    }

    @Test
    fun `when list is empty, chord name should be null`() = runTest {
        // Arrange
        initViewModel()
        val notes = listOf(Note(60, "C4"))
        every { notes.toVexflowJson() } returns "json"
        every { emptyList<Note>().toVexflowJson() } returns "empty_json"

        viewModel.uiState.test {
            awaitItem() // Skip initial
            
            // Emit notes first
            midiMessagesFlow.emit(notes)
            awaitItem()
            
            // Act: emit empty list
            midiMessagesFlow.emit(emptyList())

            // Assert
            assertEquals(null, awaitItem().chordName)
        }
    }
}
