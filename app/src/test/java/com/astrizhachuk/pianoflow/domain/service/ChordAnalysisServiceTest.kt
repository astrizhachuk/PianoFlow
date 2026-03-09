package com.astrizhachuk.pianoflow.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChordAnalysisServiceTest {

    private val service = ChordAnalysisService()

    @Test
    fun `processChordAnalysisResult with major chord suffix M returns chord name without it`() {
        val rawResult = "\"CM\""
        val result = service.processChordAnalysisResult(rawResult)
        assertEquals("C", result)
    }

    @Test
    fun `processChordAnalysisResult with minor chord returns correct chord name`() {
        val rawResult = "\"Am\""
        val result = service.processChordAnalysisResult(rawResult)
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with null result returns null`() {
        val rawResult = null
        val result = service.processChordAnalysisResult(rawResult)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with string null returns null`() {
        val rawResult = "\"null\""
        val result = service.processChordAnalysisResult(rawResult)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with empty string returns null`() {
        val rawResult = "\"\""
        val result = service.processChordAnalysisResult(rawResult)
        assertNull(result)
    }

    @Test
    fun `processChordAnalysisResult with no surrounding quotes returns correct chord name`() {
        val rawResult = "Am"
        val result = service.processChordAnalysisResult(rawResult)
        assertEquals("Am", result)
    }

    @Test
    fun `processChordAnalysisResult with valid single note returns note name`() {
        val rawResult = "\"C4\""
        val result = service.processChordAnalysisResult(rawResult)
        assertEquals("C4", result)
    }

    @Test
    fun `processChordAnalysisResult returns null when result is blank`() {
        assertNull(service.processChordAnalysisResult(null))
        assertNull(service.processChordAnalysisResult("\"null\""))
        assertNull(service.processChordAnalysisResult(" "))
    }
}
