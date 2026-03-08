package com.astrizhachuk.pianoflow.domain.usecase.analysis

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AnalyzeChordUseCaseTest {

    private val chordAnalysisRepository: ChordAnalysisRepository = mockk()
    private val useCase = AnalyzeChordUseCase(chordAnalysisRepository)

    @Test
    fun `invoke should call repository to analyze chord`() {
        // Arrange
        val notes = listOf(Note(60, "C4"))
        justRun { chordAnalysisRepository.analyzeChord(any()) }

        // Act
        useCase(notes)

        // Assert
        verify(exactly = 1) { chordAnalysisRepository.analyzeChord(notes) }
    }

    @Test
    fun `invoke should not throw exception when repository fails`() {
        // Arrange
        val notes = listOf(Note(60, "C4"))
        val exception = RuntimeException("Failed to analyze")
        every { chordAnalysisRepository.analyzeChord(any()) } throws exception

        // Act
        useCase(notes)

        // Assert
        verify(exactly = 1) { chordAnalysisRepository.analyzeChord(notes) }
    }
}
