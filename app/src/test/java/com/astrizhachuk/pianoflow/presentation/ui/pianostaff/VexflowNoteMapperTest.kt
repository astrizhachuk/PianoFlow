package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class VexflowNoteMapperTest {

    @Test
    fun `toVexflowJson for empty list returns empty json array`() {
        // Arrange
        val notes = emptyList<Note>()
        // Act
        val json = notes.toVexflowJson()
        // Assert
        assertEquals("[]", json)
    }

    @Test
    fun `toVexflowJson for a single note returns correct json`() {
        // Arrange
        val notes = listOf(Note(pitch = 60)) // Middle C
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "[{\\\"keys\\\":[\\\"c/4\\\"],\\\"duration\\\":\\\"w\\\"}]"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for a chord returns correct sorted json`() {
        // Arrange
        // Unsorted C Major chord
        val notes = listOf(Note(pitch = 67), Note(pitch = 60), Note(pitch = 64)) // G4, C4, E4
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "[{\\\"keys\\\":[\\\"c/4\\\", \\\"e/4\\\", \\\"g/4\\\"],\\\"duration\\\":\\\"w\\\"}]"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for various notes returns correct json implicitly testing private function`() {
        // Arrange
        // A mix of notes including sharps and different octaves, unsorted
        val notes = listOf(
            Note(pitch = 79), // G5
            Note(pitch = 59), // B3
            Note(pitch = 61)  // C#4
        )
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "[{\\\"keys\\\":[\\\"b/3\\\", \\\"c#/4\\\", \\\"g/5\\\"],\\\"duration\\\":\\\"w\\\"}]"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson with invalid pitch values should filter them out`() {
        // Arrange
        val notes = listOf(
            Note(pitch = -1),
            Note(pitch = 60),
            Note(pitch = 128)
        )
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "[{\\\"keys\\\":[\\\"c/4\\\"],\\\"duration\\\":\\\"w\\\"}]"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson with only invalid pitch values should return empty array`() {
        // Arrange
        val notes = listOf(
            Note(pitch = -1),
            Note(pitch = 128)
        )
        // Act
        val json = notes.toVexflowJson()
        // Assert
        assertEquals("[]", json)
    }
}
