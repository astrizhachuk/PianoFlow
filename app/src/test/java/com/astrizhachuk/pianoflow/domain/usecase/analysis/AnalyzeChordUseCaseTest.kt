package com.astrizhachuk.pianoflow.domain.usecase.analysis

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AnalyzeChordUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var repository: ChordAnalysisRepository

    private lateinit var useCase: AnalyzeChordUseCase

    @Before
    fun setup() {
        useCase = AnalyzeChordUseCase(repository)
    }

    @Test
    fun `invoke should delegate call to repository`() {
        // Arrange
        val notes = listOf(Note(60, "C4"), Note(64, "E4"))
        every { repository.analyzeChord(notes) } returns Unit

        // Act
        useCase(notes)

        // Assert
        verify { repository.analyzeChord(notes) }
    }

    @Test
    fun `invoke when repository throws exception should not crash`() {
        // Arrange
        val notes = listOf(Note(60, "C4"))
        every { repository.analyzeChord(notes) } throws RuntimeException("Repository error")

        // Act
        useCase(notes)

        // Assert
        verify { repository.analyzeChord(notes) }
        // If no exception is re-thrown, the test passes
    }
}
