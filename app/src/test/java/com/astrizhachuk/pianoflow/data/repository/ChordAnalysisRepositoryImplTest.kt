package com.astrizhachuk.pianoflow.data.repository

import android.os.Build
import android.os.Looper
import app.cash.turbine.test
import com.astrizhachuk.pianoflow.data.datasource.analysis.MusicScriptEngine
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.service.ChordAnalysisService
import com.google.gson.Gson
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.invoke
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, Build.VERSION_CODES.TIRAMISU])
class ChordAnalysisRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var chordAnalysisService: ChordAnalysisService

    @RelaxedMockK
    private lateinit var javaScriptExecutor: MusicScriptEngine

    private val gson = Gson()

    private lateinit var repository: ChordAnalysisRepositoryImpl

    @Before
    fun setup() {
        repository = ChordAnalysisRepositoryImpl(
            chordAnalysisService,
            javaScriptExecutor,
            gson
        )
    }

    @Test
    fun `initial chordAnalysisResult should be null`() = runTest {
        // Assert
        repository.chordAnalysisResult.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeChord with empty list should update state to null`() = runTest {
        // Act
        repository.analyzeChord(emptyList())

        // Assert
        repository.chordAnalysisResult.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeChord with notes should execute script and update result on main thread`() = runTest {
        // Arrange
        val notes = listOf(
            Note(60, "C4"),
            Note(64, "E4"),
            Note(67, "G4")
        )
        val expectedScript = "analyze([\"C4\",\"E4\",\"G4\"])"
        val rawResult = "\"C\""
        val processedResult = "C"

        every { chordAnalysisService.processChordAnalysisResult(rawResult) } returns processedResult
        every { javaScriptExecutor.execute(eq(expectedScript), any()) } answers {
            secondArg<(String?) -> Unit>().invoke(rawResult)
        }

        // Act
        repository.analyzeChord(notes)
        
        // Process pending posts on MainLooper (Handler.post)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertEquals(processedResult, repository.chordAnalysisResult.value)
        
        verify { javaScriptExecutor.execute(expectedScript, any()) }
        verify { chordAnalysisService.processChordAnalysisResult(rawResult) }
    }

    @Test
    fun `analyzeChord should sort and distinct note names before building script`() = runTest {
        // Arrange
        val notes = listOf(
            Note(67, "G4"),
            Note(60, "C4"),
            Note(60, "C4"),
            Note(64, "E4")
        )
        // Distinct and sorted: C4, E4, G4
        val expectedScript = "analyze([\"C4\",\"E4\",\"G4\"])"
        
        every { javaScriptExecutor.execute(any(), any()) } just runs

        // Act
        repository.analyzeChord(notes)

        // Assert
        verify { javaScriptExecutor.execute(eq(expectedScript), any()) }
    }

    @Test
    fun `analyzeChord when script execution throws exception should update state to null`() = runTest {
        // Arrange
        val notes = listOf(Note(60, "C4"))
        every { javaScriptExecutor.execute(any(), any()) } throws RuntimeException("JS Engine Error")

        // Act
        repository.analyzeChord(notes)

        // Assert
        assertNull(repository.chordAnalysisResult.value)
    }

    @Test
    fun `analyzeChord when JS returns result should handle null from processing service`() = runTest {
        // Arrange
        val notes = listOf(Note(60, "C4"))
        val rawResult = "null"
        
        every { chordAnalysisService.processChordAnalysisResult(rawResult) } returns null
        every { javaScriptExecutor.execute(any(), any()) } answers {
            secondArg<(String?) -> Unit>().invoke(rawResult)
        }

        // Act
        repository.analyzeChord(notes)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertNull(repository.chordAnalysisResult.value)
    }
}
