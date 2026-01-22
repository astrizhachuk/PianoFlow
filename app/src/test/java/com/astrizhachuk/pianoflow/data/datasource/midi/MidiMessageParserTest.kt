
package com.astrizhachuk.pianoflow.data.datasource.midi

import com.astrizhachuk.pianoflow.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MidiMessageParserTest {

    private companion object {
        // MIDI Commands (Status Bytes without channel)
        const val NOTE_ON_COMMAND = 0x90
        const val NOTE_OFF_COMMAND = 0x80
        const val CONTROL_CHANGE_COMMAND = 0xB0

        // MIDI Note Pitches
        const val PITCH_MIN: Byte = 0
        const val PITCH_MIDDLE_C: Byte = 60
        const val PITCH_D4: Byte = 62
        const val PITCH_E4: Byte = 64
        const val PITCH_MAX: Byte = 127

        // MIDI Note Velocities
        const val VELOCITY_ZERO: Byte = 0
        const val VELOCITY_MIN_POSITIVE: Byte = 1
        const val VELOCITY_FORTE: Byte = 100
        const val VELOCITY_FF: Byte = 110
        const val VELOCITY_FFF: Byte = 120
        const val VELOCITY_MAX: Byte = 127

        // Helper to create a status byte with a channel
        fun noteOn(channel: Int) = (NOTE_ON_COMMAND or channel).toByte()
    }

    private lateinit var parser: MidiMessageParser

    @Before
    fun setUp() {
        parser = MidiMessageParser()
    }

    @Test
    fun `when valid Note On message is parsed then Note object is returned`() {
        // Arrange
        val message = byteArrayOf(noteOn(0), PITCH_MIDDLE_C, VELOCITY_FORTE)

        // Act
        val result = parser.parse(message)

        // Assert
        assertNotNull(result)
        assertEquals(Note(pitch = PITCH_MIDDLE_C.toInt()), result)
    }

    @Test
    fun `when Note On with zero velocity is parsed then null is returned`() {
        // Arrange
        // A velocity of 0 is often interpreted as a 'Note Off' event.
        val message = byteArrayOf(noteOn(0), PITCH_MIDDLE_C, VELOCITY_ZERO)

        // Act
        val result = parser.parse(message)

        // Assert
        assertNull(result)
    }

    @Test
    fun `when non Note On message is parsed then null is returned`() {
        // Arrange
        val noteOffMessage = byteArrayOf(NOTE_OFF_COMMAND.toByte(), PITCH_MIDDLE_C, VELOCITY_FORTE)
        val controlChangeMessage = byteArrayOf(CONTROL_CHANGE_COMMAND.toByte(), 1, VELOCITY_MAX)

        // Act
        val noteOffResult = parser.parse(noteOffMessage)
        val controlChangeResult = parser.parse(controlChangeMessage)

        // Assert
        assertNull(noteOffResult)
        assertNull(controlChangeResult)
    }

    @Test
    fun `when empty byte array is parsed then null is returned`() {
        // Arrange
        val message = byteArrayOf()

        // Act
        val result = parser.parse(message)

        // Assert
        assertNull(result)
    }

    @Test
    fun `when Note On with insufficient data is parsed then null is returned`() {
        // Arrange
        val shortMessage = byteArrayOf(noteOn(0), PITCH_MIDDLE_C)

        // Act
        val result = parser.parse(shortMessage)

        // Assert
        assertNull(result)
    }

    @Test
    fun `when Note On on any channel is parsed then it is parsed correctly`() {
        // Arrange
        // We test a common channel and a boundary channel (15) to ensure all are handled.
        val messageChannel1 = byteArrayOf(noteOn(1), PITCH_MIDDLE_C, VELOCITY_FORTE)
        val messageChannel15 = byteArrayOf(noteOn(15), PITCH_E4, VELOCITY_FFF)

        // Act
        val result1 = parser.parse(messageChannel1)
        val result15 = parser.parse(messageChannel15)

        // Assert
        assertEquals(Note(pitch = PITCH_MIDDLE_C.toInt()), result1)
        assertEquals(Note(pitch = PITCH_E4.toInt()), result15)
    }

    @Test
    fun `when message with max pitch and velocity is parsed then correct Note is returned`() {
        // Arrange
        val message = byteArrayOf(noteOn(0), PITCH_MAX, VELOCITY_MAX)

        // Act
        val result = parser.parse(message)

        // Assert
        assertEquals(Note(pitch = PITCH_MAX.toInt()), result)
    }

    @Test
    fun `when message with min pitch and velocity is parsed then correct Note is returned`() {
        // Arrange
        val message = byteArrayOf(noteOn(0), PITCH_MIN, VELOCITY_MIN_POSITIVE)

        // Act
        val result = parser.parse(message)

        // Assert
        assertEquals(Note(pitch = PITCH_MIN.toInt()), result)
    }

    @Test
    fun `when message with negative byte values is parsed then it is handled correctly`() {
        // Arrange
        // Raw byte values can be > 127, which are negative in signed representation.
        val pitch: Byte = 76
        val velocity: Byte = 90
        val message = byteArrayOf(noteOn(0), pitch, velocity)

        // Act
        val result = parser.parse(message)

        // Assert
        assertNotNull(result)
        assertEquals(pitch.toInt(), result!!.pitch)
    }

    @Test
    fun `when Note On with extra data is parsed then it is parsed correctly`() {
        // Arrange
        val message = byteArrayOf(noteOn(0), PITCH_MIDDLE_C, VELOCITY_FORTE, noteOn(0), PITCH_D4, VELOCITY_FF)

        // Act
        val result = parser.parse(message)

        // Assert
        // The parser should only read the first valid message.
        assertEquals(Note(pitch = PITCH_MIDDLE_C.toInt()), result)
    }
}
