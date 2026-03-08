package com.astrizhachuk.pianoflow.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ChordAnalysisServiceTest {

    private val service = ChordAnalysisService()

    @Test
    fun `parseNotesFromJson with empty json returns empty list`() {
        val json = "{}"
        val result = service.parseNotesFromJson(json)
        assertEquals(emptyList<String>(), result)
    }

    @Test(expected = com.google.gson.JsonSyntaxException::class)
    fun `parseNotesFromJson with invalid json throws JsonSyntaxException`() {
        val json = "not a json"
        service.parseNotesFromJson(json)
    }

    @Test
    fun `parseNotesFromJson with literal null string triggers elvis operator and returns empty list`() {
        val json = "null"
        val result = service.parseNotesFromJson(json)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesFromJson with missing fields in json returns empty list`() {
        val json = "{\"random_field\": 123}"
        val result = service.parseNotesFromJson(json)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesFromJson with empty notes returns empty list`() {
        val json = "{\"treble\":[], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesFromJson with notes in both clefs returns sorted list`() {
        val json = "{\"treble\":[{\"keys\":[\"c/5\"]}], \"bass\":[{\"keys\":[\"c/4\"]}]}"
        val result = service.parseNotesFromJson(json)
        val expected = listOf("C4", "C5")
        assertEquals(expected, result)
    }

    @Test
    fun `parseNotesFromJson with duplicate notes returns distinct list`() {
        val json = "{\"treble\":[{\"keys\":[\"c/4\"]}], \"bass\":[{\"keys\":[\"c/4\"]}]}"
        val result = service.parseNotesFromJson(json)
        val expected = listOf("C4")
        assertEquals(expected, result)
    }

    @Test
    fun `parseNotesFromJson with lowercase notes returns uppercase list`() {
        val json = "{\"treble\":[{\"keys\":[\"c/4\", \"d#/5\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        val expected = listOf("C4", "D#5")
        assertEquals(expected, result)
    }

    @Test
    fun `parseNotesFromJson with invalid note format returns note name as is`() {
        val json = "{\"treble\":[{\"keys\":[\"C5\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("C5"), result)
    }

    @Test
    fun `parseNotesFromJson with full Vexflow-like JSON returns correct notes`() {
        val json = """
            {
                "treble": [{"keys": ["c/5", "e/5"], "duration": "w"}],
                "bass": [{"keys": ["c/4"], "duration": "w", "ghost": true}]
            }
        """.trimIndent()
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("C4", "C5", "E5"), result)
    }

    @Test
    fun `formatNoteForTonal handles sharps and flats correctly`() {
        val json = "{\"treble\":[{\"keys\":[\"f#/4\", \"bb/3\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("Bb3", "F#4"), result)
    }

    @Test
    fun `processChordAnalysisResult with major chord suffix M returns chord name without it`() {
        val rawResult = "\"CM\""
        val result = service.processChordAnalysisResult(rawResult, isChord = true)
        assertEquals("C", result)
    }

    @Test
    fun `processChordAnalysisResult with minor chord returns correct chord name`() {
        val rawResult = "\"Am\""
        val result = service.processChordAnalysisResult(rawResult, isChord = true)
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with null result for a chord returns chordNotDefined`() {
        val rawResult = null
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with null result for a single note returns null`() {
        val rawResult = null
        val result = service.processChordAnalysisResult(rawResult, isChord = false)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with string null for a chord returns chordNotDefined`() {
        val rawResult = "\"null\""
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted null string for a chord returns chordNotDefined`() {
        val rawResult = "null"
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with empty string for a chord returns chordNotDefined`() {
        val rawResult = "\"\""
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with blank string for a chord returns chordNotDefined`() {
        val rawResult = "\" \""
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with no surrounding quotes returns correct chord name`() {
        val rawResult = "Am"
        val result = service.processChordAnalysisResult(rawResult, isChord = true)
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with whitespace result for a chord returns chordNotDefined`() {
        val rawResult = " "
        val chordNotDefined = "N/A"
        val result = service.processChordAnalysisResult(rawResult, isChord = true, chordNotDefined = chordNotDefined)
        assertEquals(chordNotDefined, result)
    }

    @Test
    fun `processChordAnalysisResult with unquoted null string for a single note returns null`() {
        val rawResult = "null"
        val result = service.processChordAnalysisResult(rawResult, isChord = false)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with only M suffix returns empty string`() {
        val rawResult = "\"M\""
        val result = service.processChordAnalysisResult(rawResult, isChord = true)
        assertEquals("", result)
    }

    @Test
    fun `processChordAnalysisResult with valid single note returns note name`() {
        val rawResult = "\"C4\""
        val result = service.processChordAnalysisResult(rawResult, isChord = false)
        assertEquals("C4", result)
    }

    @Test
    fun `processChordAnalysisResult with empty string for a single note returns null`() {
        val rawResult = ""
        val result = service.processChordAnalysisResult(rawResult, isChord = false)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with blank string for a single note returns null`() {
        val rawResult = " "
        val result = service.processChordAnalysisResult(rawResult, isChord = false)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult returns null when cleanedChord is null and isChord is false`() {
        val result = service.processChordAnalysisResult(null, isChord = false)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult returns null for string null when isChord is false`() {
        val result = service.processChordAnalysisResult("\"null\"", isChord = false)
        assertNull(result)
    }

    @Test
    fun `formatNoteForTonal coverage for empty note name part`() {
        val json = "{\"treble\":[{\"keys\":[\"/4\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("4"), result)
    }

    @Test
    fun `formatNoteForTonal coverage for already uppercase note name`() {
        val json = "{\"treble\":[{\"keys\":[\"C/4\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("C4"), result)
    }

    @Test
    fun `formatNoteForTonal coverage for more than two parts`() {
        val json = "{\"treble\":[{\"keys\":[\"c/4/5\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("c/4/5"), result)
    }

    @Test
    fun `formatNoteForTonal coverage for non-alphabetic note name`() {
        val json = "{\"treble\":[{\"keys\":[\"#/4\"]}], \"bass\":[]}"
        val result = service.parseNotesFromJson(json)
        assertEquals(listOf("#4"), result)
    }

    @Test
    fun `processChordAnalysisResult exhaustive coverage for takeIf branches`() {
        // Case: it.isNotBlank() is true AND it != "null" is true
        assertEquals("Am", service.processChordAnalysisResult("Am", false))
        
        // Case: it.isNotBlank() is true AND it != "null" is false
        assertNull(service.processChordAnalysisResult("null", false))
        assertNull(service.processChordAnalysisResult("\"null\"", false))
        
        // Case: it.isNotBlank() is false (short-circuits it != "null")
        assertNull(service.processChordAnalysisResult("", false))
        assertNull(service.processChordAnalysisResult(" ", false))
        assertNull(service.processChordAnalysisResult("\"\"", false))
        assertNull(service.processChordAnalysisResult("\" \"", false))
        
        // Case: rawResult is null (short-circuits takeIf entirely)
        assertNull(service.processChordAnalysisResult(null, false))
    }
}
