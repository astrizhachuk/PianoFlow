package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexflowNoteMapperTest {

    @Test
    fun `toVexflowJson for empty list returns empty staves`() {
        // Arrange
        val notes = emptyList<Note>()
        // Act
        val json = notes.toVexflowJson()
        // Assert
        assertEquals("{\"treble\":[], \"bass\":[]}", json)
    }

    @Test
    fun `toVexflowJson for middle C returns as primary on treble and ghost on bass`() {
        // Arrange
        val notes = listOf(Note(pitch = 60)) // Middle C
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/4\"], \"duration\":\"w\"}], \"bass\":[{\"keys\":[\"c/4\"], \"duration\":\"w\", \"ghost\":true}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for a high note returns only on treble`() {
        // Arrange
        val notes = listOf(Note(pitch = 84)) // C6
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/6\"], \"duration\":\"w\"}], \"bass\":[]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for a low note returns only on bass`() {
        // Arrange
        val notes = listOf(Note(pitch = 35)) // B1, below ghost range
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "{\"treble\":[], \"bass\":[{\"keys\":[\"b/1\"], \"duration\":\"w\"}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for a low note in ghost range returns as primary on bass and ghost on treble`() {
        // Arrange
        val notes = listOf(Note(pitch = 59)) // B3
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "{\"treble\":[{\"keys\":[\"b/3\"], \"duration\":\"w\", \"ghost\":true}], \"bass\":[{\"keys\":[\"b/3\"], \"duration\":\"w\"}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for chord on treble staff returns correct json`() {
        // Arrange
        val notes = listOf(Note(pitch = 72), Note(pitch = 76), Note(pitch = 79)) // C5, E5, G5
        // Act
        val json = notes.toVexflowJson()
        // Assert
        // C5 is in ghost range, so it appears on bass staff as a ghost note
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/5\", \"e/5\", \"g/5\"], \"duration\":\"w\"}], \"bass\":[{\"keys\":[\"c/5\"], \"duration\":\"w\", \"ghost\":true}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for chord on bass staff returns correct json`() {
        // Arrange
        val notes = listOf(Note(pitch = 48), Note(pitch = 52), Note(pitch = 55)) // C3, E3, G3
        // Act
        val json = notes.toVexflowJson()
        // Assert
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/3\", \"e/3\", \"g/3\"], \"duration\":\"w\", \"ghost\":true}], \"bass\":[{\"keys\":[\"c/3\", \"e/3\", \"g/3\"], \"duration\":\"w\"}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson for mixed chord returns notes on both staves`() {
        // Arrange
        val notes = listOf(Note(pitch = 55), Note(pitch = 67)) // G3 (bass), G4 (treble)
        // Act
        val json = notes.toVexflowJson()
        // Assert
        // Both notes are in ghost range
        val expectedJson = "{\"treble\":[{\"keys\":[\"g/4\"], \"duration\":\"w\"},{\"keys\":[\"g/3\"], \"duration\":\"w\", \"ghost\":true}], \"bass\":[{\"keys\":[\"g/3\"], \"duration\":\"w\"},{\"keys\":[\"g/4\"], \"duration\":\"w\", \"ghost\":true}]}"
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
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/4\"], \"duration\":\"w\"}], \"bass\":[{\"keys\":[\"c/4\"], \"duration\":\"w\", \"ghost\":true}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `toVexflowJson with only invalid pitch values should return empty staves`() {
        // Arrange
        val notes = listOf(
            Note(pitch = -1),
            Note(pitch = 128)
        )
        // Act
        val json = notes.toVexflowJson()
        // Assert
        assertEquals("{\"treble\":[], \"bass\":[]}", json)
    }

    @Test
    fun `toVexflowJson for notes at ghost range boundaries assigns staves correctly`() {
        // Arrange
        val notes = listOf(
            // Bass notes: one just outside and one just inside the ghost range
            Note(pitch = 35), // B1, below range -> Primary on bass, no ghost
            Note(pitch = 36), // C2, bottom of range -> Primary on bass, ghost on treble

            // Treble notes: one just inside and one just outside the ghost range
            Note(pitch = 72), // C5, top of range -> Primary on treble, ghost on bass
            Note(pitch = 73)  // C#5, above range -> Primary on treble, no ghost
        )

        // Act
        val json = notes.toVexflowJson()

        // Assert
        // Treble staff should have: Primary [c/5, c#/5], Ghost [c/2]
        // Bass staff should have: Primary [b/1, c/2], Ghost [c/5]
        val expectedJson = "{\"treble\":[{\"keys\":[\"c/5\", \"c#/5\"], \"duration\":\"w\"},{\"keys\":[\"c/2\"], \"duration\":\"w\", \"ghost\":true}], \"bass\":[{\"keys\":[\"b/1\", \"c/2\"], \"duration\":\"w\"},{\"keys\":[\"c/5\"], \"duration\":\"w\", \"ghost\":true}]}"
        assertEquals(expectedJson, json)
    }

    @Test
    fun `parseNotesForAnalysis with empty json returns empty list`() {
        // Arrange
        val json = "{}"

        // Act
        val result = parseNotesForAnalysis(json)

        // Assert
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesForAnalysis with empty notes returns empty list`() {
        // Arrange
        val json = "{\"treble\":[], \"bass\":[]}"

        // Act
        val result = parseNotesForAnalysis(json)

        // Assert
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesForAnalysis with notes in both clefs returns sorted list`() {
        // Arrange
        val json = "{\"treble\":[{\"keys\":[\"c/5\"]}], \"bass\":[{\"keys\":[\"c/4\"]}]}"

        // Act
        val result = parseNotesForAnalysis(json)

        // Assert
        val expected = listOf("C4", "C5")
        assertEquals(expected, result)
    }

    @Test
    fun `parseNotesForAnalysis with duplicate notes returns distinct list`() {
        // Arrange
        val json = "{\"treble\":[{\"keys\":[\"c/4\"]}], \"bass\":[{\"keys\":[\"c/4\"]}]}"

        // Act
        val result = parseNotesForAnalysis(json)

        // Assert
        val expected = listOf("C4")
        assertEquals(expected, result)
    }

    @Test
    fun `parseNotesForAnalysis with lowercase notes returns uppercase list`() {
        // Arrange
        val json = "{\"treble\":[{\"keys\":[\"c/4\", \"d#/5\"]}], \"bass\":[]}"

        // Act
        val result = parseNotesForAnalysis(json)

        // Assert
        val expected = listOf("C4", "D#5")
        assertEquals(expected, result)
    }

    @Test(expected = JsonSyntaxException::class)
    fun `parseNotesForAnalysis with invalid json throws exception`() {
        // Arrange
        val json = "invalid json"

        // Act
        parseNotesForAnalysis(json)
    }

    @Test
    fun `processChordAnalysisResult with major chord suffix M returns chord name without it`() {
        // Arrange
        val rawResult = "\"CM\""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals("C", result)
    }

    @Test
    fun `processChordAnalysisResult with minor chord returns correct chord name`() {
        // Arrange
        val rawResult = "\"Am\""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with null result for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = null
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with null result for a single note returns null`() {
        // Arrange
        val rawResult = null
        val isChord = false
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with string null for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = "\"null\""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with blank result for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = "\"\""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with no surrounding quotes returns correct chord name`() {
        // Arrange
        val rawResult = "Am" // No surrounding quotes
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with whitespace result for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = "\" \""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted null string for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = "null"
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals( chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with empty raw string for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = ""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with empty raw string for a single note returns null`() {
        // Arrange
        val rawResult = ""
        val isChord = false
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with only M suffix returns empty string`() {
        // Arrange
        val rawResult = "\"M\""
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted whitespace for a chord returns chordNotDefined`() {
        // Arrange
        val rawResult = " "
        val isChord = true
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted whitespace for a single note returns null`() {
        // Arrange
        val rawResult = " "
        val isChord = false
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted null string for a single note returns null`() {
        // Arrange
        val rawResult = "null"
        val isChord = false
        val chordNotDefined = "N/A"

        // Act
        val result = processChordAnalysisResult(rawResult, isChord, chordNotDefined)

        // Assert
        assertNull(result)
    }
}
