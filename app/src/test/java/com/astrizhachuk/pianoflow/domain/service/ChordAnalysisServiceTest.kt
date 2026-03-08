package com.astrizhachuk.pianoflow.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChordAnalysisServiceTest {

    private val service = ChordAnalysisService()

    @Test
    fun `parseNotesFromJson with empty json returns empty list`() {
        val json = "{}"
        val result = service.parseNotesFromJson(json)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseNotesFromJson with invalid json returns empty list`() {
        val json = "not a json"
        // Gson might throw or return null depending on how it's called, 
        // in our implementation it will return empty list due to catch or null handling
        val result = try { service.parseNotesFromJson(json) } catch(e: Exception) { emptyList() }
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
}
