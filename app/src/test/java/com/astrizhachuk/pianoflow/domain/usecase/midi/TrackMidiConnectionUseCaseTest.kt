package com.astrizhachuk.pianoflow.domain.usecase.midi

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMidiConnectionUseCaseTest {

    private val midiRepository: MidiRepository = mockk()
    private val useCase = TrackMidiConnectionUseCase(midiRepository)

    @Test
    fun `invoke calls observeConnectionState and returns flow`() = runTest {
        // Given
        val mockDevice = MidiDevice(id = 1, name = "Test Device", product = "Test Product", manufacturer = "Test Manufacturer")
        val expectedState = ConnectionState.Connected(mockDevice)
        val expectedFlow = flowOf(expectedState)
        every { midiRepository.observeConnectionState() } returns expectedFlow

        // When
        val resultFlow = useCase()

        // Then
        resultFlow.test {
            assertEquals(expectedState, awaitItem())
            awaitComplete()
        }
        verify(exactly = 1) { midiRepository.observeConnectionState() }
    }
}
