package com.astrizhachuk.pianoflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionTest {

    @Test
    fun `pitchToName valid lower boundary pitch 0`() {
        assertEquals("C-1", Note.pitchToName(0))
    }

    @Test
    fun `pitchToName valid upper boundary pitch 127`() {
        assertEquals("G9", Note.pitchToName(127))
    }

    @Test
    fun `pitchToName invalid lower out of bounds`() {
        assertEquals("", Note.pitchToName(-1))
    }

    @Test
    fun `pitchToName invalid upper out of bounds`() {
        assertEquals("", Note.pitchToName(128))
    }

    @Test
    fun `pitchToName middle C reference check`() {
        assertEquals("C4", Note.pitchToName(60))
    }

    @Test
    fun `pitchToName concert pitch A check`() {
        assertEquals("A4", Note.pitchToName(69))
    }

    @Test
    fun `pitchToName accidental note check`() {
        assertEquals("C#4", Note.pitchToName(61))
    }

    @Test
    fun `pitchToName octave transition boundary lower`() {
        assertEquals("B-1", Note.pitchToName(11))
        assertEquals("C0", Note.pitchToName(12))
    }

    @Test
    fun `pitchToName octave transition boundary high`() {
        assertEquals("B8", Note.pitchToName(119))
        assertEquals("C9", Note.pitchToName(120))
    }

    @Test
    fun `pitchToName extreme integer inputs`() {
        assertEquals("", Note.pitchToName(Int.MIN_VALUE))
        assertEquals("", Note.pitchToName(Int.MAX_VALUE))
    }

    @Test
    fun `midiToOctave returns the scientific octave number`() {
        assertEquals(0, Note.midiToOctave(21))  // A0
        assertEquals(4, Note.midiToOctave(60))  // C4
        assertEquals(8, Note.midiToOctave(119)) // B8
    }
}