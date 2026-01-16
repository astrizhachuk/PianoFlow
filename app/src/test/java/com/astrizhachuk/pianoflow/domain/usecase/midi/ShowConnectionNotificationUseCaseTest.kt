package com.astrizhachuk.pianoflow.domain.usecase.midi

import android.content.Context
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ShowConnectionNotificationUseCaseTest {

    private val context: Context = mockk()
    private val useCase = ShowConnectionNotificationUseCase(context)

    private val disconnectedMessage = "MIDI device disconnected"
    private val connectedMessage = "MIDI device connected: %s"

    @Test
    fun `invoke with Connected state returns connected message from resources`() {
        // Given
        val deviceName = "My MIDI Keyboard"
        val device = MidiDevice(id = 1, name = deviceName, product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)
        every { context.getString(R.string.midi_device_connected, deviceName) } returns connectedMessage.format(deviceName)

        // When
        val result = useCase(state)

        // Then
        assertEquals(connectedMessage.format(deviceName), result.text)
    }

    @Test
    fun `invoke with Connected state and empty device name returns message with empty name`() {
        // Given
        val deviceName = ""
        val device = MidiDevice(id = 1, name = deviceName, product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)
        every { context.getString(R.string.midi_device_connected, deviceName) } returns connectedMessage.format(deviceName)

        // When
        val result = useCase(state)

        // Then
        assertEquals(connectedMessage.format(deviceName), result.text)
    }

    @Test
    fun `invoke with Disconnected state returns disconnected message from resources`() {
        // Given
        val state = ConnectionState.Disconnected
        every { context.getString(R.string.midi_device_disconnected) } returns disconnectedMessage

        // When
        val result = useCase(state)

        // Then
        assertEquals(disconnectedMessage, result.text)
    }

    @Test
    fun `invoke with NoDevice state returns disconnected message from resources`() {
        // Given
        val state = ConnectionState.NoDevice
        every { context.getString(R.string.midi_device_disconnected) } returns disconnectedMessage

        // When
        val result = useCase(state)

        // Then
        assertEquals(disconnectedMessage, result.text)
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
