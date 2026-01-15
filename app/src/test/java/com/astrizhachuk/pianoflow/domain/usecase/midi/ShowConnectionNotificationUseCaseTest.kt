package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class ShowConnectionNotificationUseCaseTest {

    private val useCase = ShowConnectionNotificationUseCase()

    @Test
    fun `invoke with Connected state returns connected message`() {
        // Given
        val deviceName = "My MIDI Keyboard"
        val device = MidiDevice(id = 1, name = deviceName, product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)

        // When
        val result = useCase(state)

        // Then
        assertEquals("MIDI device connected: $deviceName", result.text)
    }

    @Test
    fun `invoke with Connected state and empty device name returns message with empty name`() {
        // Given
        val device = MidiDevice(id = 1, name = "", product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)

        // When
        val result = useCase(state)

        // Then
        assertEquals("MIDI device connected: ", result.text)
    }

    @Test
    fun `invoke with Disconnected state returns disconnected message`() {
        // Given
        val state = ConnectionState.Disconnected

        // When
        val result = useCase(state)

        // Then
        assertEquals("MIDI device disconnected", result.text)
    }

    @Test
    fun `invoke with NoDevice state returns disconnected message`() {
        // Given
        val state = ConnectionState.NoDevice

        // When
        val result = useCase(state)

        // Then
        assertEquals("MIDI device disconnected", result.text)
    }

    @Test
    fun `invoke with Error state and message returns error message`() {
        // Given
        val errorMessage = "Something went wrong"
        val state = ConnectionState.Error(errorMessage)

        // When
        val result = useCase(state)

        // Then
        assertEquals(errorMessage, result.text)
    }
}
