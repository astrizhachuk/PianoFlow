package com.astrizhachuk.pianoflow.domain.usecase.analysis

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
        val notesJson = """{"treble":[],"bass":[]}"""
        justRun { chordAnalysisRepository.analyzeChord(any()) }

        // Act
        useCase(notesJson)

        // Assert
        verify(exactly = 1) { chordAnalysisRepository.analyzeChord(notesJson) }
    }

    @Test
    fun `invoke should not throw exception when repository fails`() {
        // Arrange
        val notesJson = "invalid_json"
        val exception = RuntimeException("Failed to analyze")
        every { chordAnalysisRepository.analyzeChord(any()) } throws exception

        // Act
        useCase(notesJson)

        // Assert
        verify(exactly = 1) { chordAnalysisRepository.analyzeChord(notesJson) }
    }
}
