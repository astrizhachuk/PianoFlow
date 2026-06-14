package com.astrizhachuk.pianoflow.data.repository

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.service.analysis.ChordAnalyzer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChordAnalysisRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var chordAnalyzer: ChordAnalyzer

    private lateinit var repository: ChordAnalysisRepositoryImpl

    @Before
    fun setup() {
        repository = ChordAnalysisRepositoryImpl(chordAnalyzer)
    }

    @Test
    fun `initial chordAnalysisResult is null`() = runTest {
        repository.chordAnalysisResult.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeChord with empty list emits null without calling analyzer`() = runTest {
        repository.analyzeChord(emptyList())

        repository.chordAnalysisResult.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { chordAnalyzer.analyze(any()) }
    }

    @Test
    fun `analyzeChord passes sorted distinct note names to analyzer`() {
        val notes = listOf(
            Note(67, "G4"),
            Note(60, "C4"),
            Note(60, "C4"),
            Note(64, "E4")
        )
        every { chordAnalyzer.analyze(listOf("C4", "E4", "G4")) } returns "C"

        repository.analyzeChord(notes)

        verify { chordAnalyzer.analyze(listOf("C4", "E4", "G4")) }
    }

    @Test
    fun `analyzeChord orders note names by pitch so the lowest note is the bass`() {
        // C7: C4, E4, G4, A#4 — A# sorts before C lexicographically, but C is the bass.
        val notes = listOf(
            Note(70, "A#4"),
            Note(60, "C4"),
            Note(67, "G4"),
            Note(64, "E4")
        )
        every { chordAnalyzer.analyze(listOf("C4", "E4", "G4", "A#4")) } returns "C7"

        repository.analyzeChord(notes)

        verify { chordAnalyzer.analyze(listOf("C4", "E4", "G4", "A#4")) }
    }

    @Test
    fun `analyzeChord emits result from analyzer synchronously`() = runTest {
        val notes = listOf(Note(60, "C4"), Note(64, "E4"), Note(67, "G4"))
        every { chordAnalyzer.analyze(listOf("C4", "E4", "G4")) } returns "C"

        repository.analyzeChord(notes)

        assertEquals("C", repository.chordAnalysisResult.value)
    }

    @Test
    fun `analyzeChord emits null when analyzer returns null`() = runTest {
        val notes = listOf(Note(60, "C4"), Note(62, "D4"), Note(64, "E4"))
        every { chordAnalyzer.analyze(any()) } returns null

        repository.analyzeChord(notes)

        assertNull(repository.chordAnalysisResult.value)
    }

    @Test
    fun `analyzeChord emits null when analyzer throws exception`() = runTest {
        val notes = listOf(Note(60, "C4"))
        every { chordAnalyzer.analyze(any()) } throws RuntimeException("unexpected")

        repository.analyzeChord(notes)

        assertNull(repository.chordAnalysisResult.value)
    }
}
