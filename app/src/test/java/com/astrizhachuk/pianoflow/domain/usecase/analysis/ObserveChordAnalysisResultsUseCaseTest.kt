package com.astrizhachuk.pianoflow.domain.usecase.analysis

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ObserveChordAnalysisResultsUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var repository: ChordAnalysisRepository

    private lateinit var useCase: ObserveChordAnalysisResultsUseCase

    @Before
    fun setup() {
        useCase = ObserveChordAnalysisResultsUseCase(repository)
    }

    @Test
    fun `invoke should return state flow from repository`() = runTest {
        // Arrange
        val expectedChord = "C Major"
        val flow = MutableStateFlow<String?>(expectedChord)
        every { repository.chordAnalysisResult } returns flow

        // Act & Assert
        useCase().test {
            assertEquals(expectedChord, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
