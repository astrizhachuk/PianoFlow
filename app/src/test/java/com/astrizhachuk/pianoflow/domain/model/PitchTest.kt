package com.astrizhachuk.pianoflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PitchTest {

    // ── Parsing: letters ──────────────────────────────────────────────────

    @Test
    fun `parse C4 natural note`() {
        val p = Pitch.parse("C4")!!
        assertEquals(NoteLetter.C, p.letter)
        assertEquals(0, p.alter)
        assertEquals(4, p.octave)
        assertEquals(60, p.midi)
        assertEquals(0, p.chroma)
    }

    @Test
    fun `parse lowercase c4`() {
        val p = Pitch.parse("c4")!!
        assertEquals(NoteLetter.C, p.letter)
        assertEquals(0, p.chroma)
    }

    @Test
    fun `parse A4`() {
        val p = Pitch.parse("A4")!!
        assertEquals(NoteLetter.A, p.letter)
        assertEquals(9, p.chroma)
        assertEquals(69, p.midi)  // (4+1)*12 + 9 = 69
    }

    // ── Parsing: alterations ──────────────────────────────────────────────

    @Test
    fun `parse C sharp`() {
        val p = Pitch.parse("C#4")!!
        assertEquals(1, p.alter)
        assertEquals(1, p.chroma)
        assertEquals(61, p.midi)  // 60+1
    }

    @Test
    fun `parse D double sharp Dx`() {
        val p = Pitch.parse("Dx4")!!
        assertEquals(2, p.alter)
        assertEquals(4, p.chroma)  // D letterChroma=2, alter=2, chroma=(2+2)%12=4
        assertEquals(64, p.midi)   // (4+1)*12 + 2 + 2 = 64
    }

    @Test
    fun `parse B sharp cross-octave midi 72 chroma 0`() {
        val p = Pitch.parse("B#4")!!
        assertEquals(NoteLetter.B, p.letter)
        assertEquals(1, p.alter)
        assertEquals(4, p.octave)
        assertEquals(72, p.midi)   // (4+1)*12 + 11 + 1 = 72
        assertEquals(0, p.chroma)  // (11+1+12) mod 12 = 0
    }

    @Test
    fun `parse Cb flat cross-octave midi 59 chroma 11`() {
        val p = Pitch.parse("Cb4")!!
        assertEquals(-1, p.alter)
        assertEquals(59, p.midi)   // (4+1)*12 + 0 - 1 = 59
        assertEquals(11, p.chroma) // (0-1+12) mod 12 = 11
    }

    @Test
    fun `parse Cx double sharp midi 62 chroma 2`() {
        val p = Pitch.parse("Cx4")!!
        assertEquals(2, p.alter)
        assertEquals(62, p.midi)   // (4+1)*12 + 0 + 2 = 62
        assertEquals(2, p.chroma)
    }

    @Test
    fun `parse Ebb double flat midi 62 chroma 2`() {
        val p = Pitch.parse("Ebb4")!!
        assertEquals(-2, p.alter)
        assertEquals(62, p.midi)   // (4+1)*12 + 4 - 2 = 62
        assertEquals(2, p.chroma)
    }

    @Test
    fun `parse Fb flat midi 64 chroma 4`() {
        val p = Pitch.parse("Fb4")!!
        assertEquals(-1, p.alter)
        assertEquals(64, p.midi)   // (4+1)*12 + 5 - 1 = 64
        assertEquals(4, p.chroma)  // (5-1+12) mod 12 = 4
    }

    @Test
    fun `parse E sharp midi 65 chroma 5`() {
        val p = Pitch.parse("E#4")!!
        assertEquals(1, p.alter)
        assertEquals(65, p.midi)   // (4+1)*12 + 4 + 1 = 65
        assertEquals(5, p.chroma)  // (4+1) mod 12 = 5
    }

    // ── Parsing: octave variants ──────────────────────────────────────────

    @Test
    fun `parse C negative octave C-1 midi 0`() {
        val p = Pitch.parse("C-1")!!
        assertEquals(-1, p.octave)
        assertEquals(0, p.midi)    // (-1+1)*12 + 0 + 0 = 0
    }

    @Test
    fun `parse C0 midi 12`() {
        val p = Pitch.parse("C0")!!
        assertEquals(0, p.octave)
        assertEquals(12, p.midi)   // (0+1)*12 + 0 + 0 = 12
        assertEquals(0, p.chroma)
    }

    @Test
    fun `parse B-1 midi 11`() {
        val p = Pitch.parse("B-1")!!
        assertEquals(NoteLetter.B, p.letter)
        assertEquals(-1, p.octave)
        assertEquals(11, p.midi)   // (-1+1)*12 + 11 + 0 = 11
        assertEquals(11, p.chroma)
    }

    @Test
    fun `parse C without octave has null octave and midi`() {
        val p = Pitch.parse("C")!!
        assertNull(p.octave)
        assertNull(p.midi)
        assertEquals(0, p.chroma)
    }

    @Test
    fun `parse F sharp without octave`() {
        val p = Pitch.parse("F#")!!
        assertNull(p.octave)
        assertNull(p.midi)
        assertEquals(6, p.chroma)  // (5+1) mod 12 = 6
    }

    // ── Parsing: invalid inputs ───────────────────────────────────────────

    @Test
    fun `parse empty string returns null`() {
        assertNull(Pitch.parse(""))
    }

    @Test
    fun `parse unknown letter H returns null`() {
        assertNull(Pitch.parse("H4"))
    }

    @Test
    fun `parse mixed sharp and flat C sharp b returns null`() {
        assertNull(Pitch.parse("C#b4"))
    }

    @Test
    fun `parse double x is invalid`() {
        assertNull(Pitch.parse("Cxx4"))
    }

    @Test
    fun `parse x mixed with sharp is invalid`() {
        assertNull(Pitch.parse("Cx#4"))
    }

    @Test
    fun `parse purely numeric input returns null`() {
        assertNull(Pitch.parse("4"))
    }

    @Test
    fun `parse octave overflow returns null`() {
        // Regex (-?\d+)? admits arbitrary digit length; parse must not throw NumberFormatException.
        assertNull(Pitch.parse("C99999999999"))
        assertNull(Pitch.parse("C-99999999999"))
    }

    @Test
    fun `parse triple sharp is invalid`() {
        // Regex (#{1,2}|b{1,2}|x) admits at most two sharps; longer runs must be rejected.
        assertNull(Pitch.parse("C###4"))
    }

    @Test
    fun `parse triple flat is invalid`() {
        assertNull(Pitch.parse("Cbbb4"))
    }

    // ── MIDI boundary ─────────────────────────────────────────────────────

    @Test
    fun `parse G9 midi 127 boundary`() {
        // (9+1)*12 + 7 = 127
        val p = Pitch.parse("G9")!!
        assertEquals(127, p.midi)
    }

    @Test
    fun `parse G sharp 9 midi out of range returns Pitch with null midi`() {
        // (9+1)*12 + 7 + 1 = 128 → out of 0..127 → midi = null
        val p = Pitch.parse("G#9")!!
        assertNotNull(p)            // Pitch is still returned
        assertEquals(9, p.octave)  // octave is present
        assertNull(p.midi)          // but midi is null (out of range)
        assertEquals(8, p.chroma)  // G#=chroma 8
    }

    @Test
    fun `parse Cb-1 midi -1 out of range returns Pitch with null midi`() {
        // (-1+1)*12 + 0 - 1 = -1 → out of 0..127 → midi = null
        val p = Pitch.parse("Cb-1")!!
        assertNotNull(p)
        assertEquals(-1, p.octave)
        assertNull(p.midi)
        assertEquals(11, p.chroma)
    }

    @Test
    fun `parse very large octave does not overflow and yields null midi`() {
        // (200_000_000 + 1) * 12 = 2_400_000_012, exceeds Int.MAX_VALUE.
        // Without Long arithmetic this would wrap and could spuriously fall into 0..127.
        val p = Pitch.parse("C200000000")!!
        assertEquals(200_000_000, p.octave)
        assertNull(p.midi)
        assertEquals(0, p.chroma)
    }

    @Test
    fun `parse very large negative octave does not overflow and yields null midi`() {
        val p = Pitch.parse("C-200000000")!!
        assertEquals(-200_000_000, p.octave)
        assertNull(p.midi)
        assertEquals(0, p.chroma)
    }

    // ── Direct constructor: invariant enforcement ─────────────────────────

    @Test
    fun `constructor rejects alter below -2`() {
        assertThrows(IllegalArgumentException::class.java) {
            Pitch(NoteLetter.C, alter = -3, octave = 4)
        }
    }

    @Test
    fun `constructor rejects alter above +2`() {
        assertThrows(IllegalArgumentException::class.java) {
            Pitch(NoteLetter.C, alter = 3, octave = 4)
        }
    }

    @Test
    fun `constructor accepts alter at boundary -2`() {
        val p = Pitch(NoteLetter.E, alter = -2, octave = 4)
        assertEquals(2, p.chroma)   // (4 - 2) mod 12 = 2
        assertEquals(62, p.midi)    // (4+1)*12 + 4 - 2 = 62
    }

    @Test
    fun `constructor accepts alter at boundary +2`() {
        val p = Pitch(NoteLetter.C, alter = 2, octave = 4)
        assertEquals(2, p.chroma)   // (0 + 2) mod 12 = 2
        assertEquals(62, p.midi)    // (4+1)*12 + 0 + 2 = 62
    }
}
