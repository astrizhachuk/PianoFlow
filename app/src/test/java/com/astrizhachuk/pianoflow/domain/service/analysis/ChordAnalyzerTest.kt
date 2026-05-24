package com.astrizhachuk.pianoflow.domain.service.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChordAnalyzerTest {

    private lateinit var analyzer: ChordAnalyzer

    @Before
    fun setup() {
        analyzer = ChordAnalyzer()
    }

    // ── Empty / invalid inputs ────────────────────────────────────────────

    @Test
    fun `empty list returns null`() {
        assertNull(analyzer.analyze(emptyList()))
    }

    @Test
    fun `fully invalid list returns null`() {
        assertNull(analyzer.analyze(listOf("Z4", "H3", "")))
    }

    @Test
    fun `single invalid note returns null`() {
        assertNull(analyzer.analyze(listOf("Z4")))
    }

    // ── Single-note simplification ────────────────────────────────────────

    @Test
    fun `single natural note returns itself`() {
        assertEquals("C4", analyzer.analyze(listOf("C4")))
    }

    @Test
    fun `single E sharp simplifies to F same octave`() {
        assertEquals("F4", analyzer.analyze(listOf("E#4")))
    }

    @Test
    fun `single B sharp simplifies to C next octave`() {
        assertEquals("C5", analyzer.analyze(listOf("B#4")))
    }

    @Test
    fun `single Cb simplifies to B lower octave`() {
        assertEquals("B3", analyzer.analyze(listOf("Cb4")))
    }

    @Test
    fun `single Cx double sharp simplifies to D`() {
        assertEquals("D4", analyzer.analyze(listOf("Cx4")))
    }

    @Test
    fun `single Ebb double flat simplifies to D`() {
        assertEquals("D4", analyzer.analyze(listOf("Ebb4")))
    }

    @Test
    fun `single Ab flat stays as Ab`() {
        assertEquals("Ab4", analyzer.analyze(listOf("Ab4")))
    }

    @Test
    fun `single G sharp stays as G sharp`() {
        assertEquals("G#4", analyzer.analyze(listOf("G#4")))
    }

    @Test
    fun `single note without octave simplifies without octave`() {
        assertEquals("F", analyzer.analyze(listOf("E#")))
    }

    @Test
    fun `single flat note without octave simplifies without octave`() {
        assertEquals("Ab", analyzer.analyze(listOf("Ab")))
    }

    @Test
    fun `single note with out of range octave simplifies without octave`() {
        // C10 is midi 132, which is null in Pitch. 
        // B-2 is midi -1, which is null in Pitch.
        // G#9 is midi 128, which is null in Pitch.
        assertEquals("C", analyzer.analyze(listOf("C10")))
        assertEquals("B", analyzer.analyze(listOf("B-2")))
        assertEquals("G#", analyzer.analyze(listOf("G#9")))
    }

    @Test
    fun `single note at MIDI boundaries simplifies with octave`() {
        // C-1 is midi 0.
        // G9 is midi 127.
        assertEquals("C-1", analyzer.analyze(listOf("C-1")))
        assertEquals("G9", analyzer.analyze(listOf("G9")))
    }

    @Test
    fun `single double sharp note without octave simplifies without octave`() {
        assertEquals("D", analyzer.analyze(listOf("Cx")))
    }

    @Test
    fun `partial list with one valid and one invalid returns simplification of valid`() {
        // "Z4" invalid, "C4" valid → single pitch → simplify "C4" → "C4"
        assertEquals("C4", analyzer.analyze(listOf("C4", "Z4")))
    }

    // ── Chord detection: basic triads ─────────────────────────────────────

    @Test
    fun `C major triad returns C (trailing M stripped)`() {
        assertEquals("C", analyzer.analyze(listOf("C4", "E4", "G4")))
    }

    @Test
    fun `A minor triad`() {
        assertEquals("Am", analyzer.analyze(listOf("A4", "C5", "E5")))
    }

    @Test
    fun `G major triad`() {
        assertEquals("G", analyzer.analyze(listOf("G4", "B4", "D5")))
    }

    @Test
    fun `F minor triad`() {
        assertEquals("Fm", analyzer.analyze(listOf("F4", "Ab4", "C5")))
    }

    @Test
    fun `B diminished triad`() {
        assertEquals("Bdim", analyzer.analyze(listOf("B3", "D4", "F4")))
    }

    @Test
    fun `C augmented triad`() {
        assertEquals("Caug", analyzer.analyze(listOf("C4", "E4", "G#4")))
    }

    // ── Chord detection: seventh chords ──────────────────────────────────

    @Test
    fun `G dominant seventh`() {
        assertEquals("G7", analyzer.analyze(listOf("G3", "B3", "D4", "F4")))
    }

    @Test
    fun `C major seventh`() {
        assertEquals("Cmaj7", analyzer.analyze(listOf("C4", "E4", "G4", "B4")))
    }

    @Test
    fun `A minor seventh`() {
        assertEquals("Am7", analyzer.analyze(listOf("A3", "C4", "E4", "G4")))
    }

    // ── Chord detection: sus chords ───────────────────────────────────────

    @Test
    fun `C sus4`() {
        assertEquals("Csus4", analyzer.analyze(listOf("C4", "F4", "G4")))
    }

    @Test
    fun `C sus2`() {
        assertEquals("Csus2", analyzer.analyze(listOf("C4", "D4", "G4")))
    }

    // ── Chord detection: inversions ───────────────────────────────────────

    @Test
    fun `E first in list becomes bass for algorithm`() {
        // analyze(["E4","G4","C5"]) with E first → E root position, weight 1.0 → "Em#5"
        assertEquals("Em#5", analyzer.analyze(listOf("E4", "G4", "C5")))
    }

    @Test
    fun `sorted C-bass input produces C major`() {
        // Repository always sorts before calling; sorted ["C5","E4","G4"] → C bass → "C"
        assertEquals("C", analyzer.analyze(listOf("C5", "E4", "G4")))
    }

    // ── Chord detection: enharmonic input preservation ────────────────────

    @Test
    fun `Db major triad uses flat notation in output`() {
        assertEquals("Db", analyzer.analyze(listOf("Db4", "F4", "Ab4")))
    }

    @Test
    fun `C sharp major uses sharp notation in output`() {
        assertEquals("C#", analyzer.analyze(listOf("C#4", "E#4", "G#4")))
    }

    // ── Chord detection: no match ─────────────────────────────────────────

    @Test
    fun `cluster chord with no matching type returns null`() {
        assertNull(analyzer.analyze(listOf("C4", "D4", "E4")))
    }

    @Test
    fun `two notes with no matching chord type returns null`() {
        assertNull(analyzer.analyze(listOf("C4", "F#4")))
    }

    // ── Chord detection: duplicates ───────────────────────────────────────

    @Test
    fun `duplicate same-name notes are deduplicated by chroma`() {
        // C4 twice + E4 = 2 unique pitch classes; no chord type → null
        assertNull(analyzer.analyze(listOf("C4", "C4", "E4")))
    }

    @Test
    fun `C sharp and Db same chroma deduplicates to one pitch`() {
        // Both are chroma 1; only one pitch class → not enough → null
        assertNull(analyzer.analyze(listOf("C#4", "Db4")))
    }

    // ── Formatting: trailing M stripping ─────────────────────────────────

    @Test
    fun `root-position C major CM becomes C`() {
        assertEquals("C", analyzer.analyze(listOf("C4", "E4", "G4")))
    }

    @Test
    fun `Db major DbM becomes Db`() {
        assertEquals("Db", analyzer.analyze(listOf("Db4", "F4", "Ab4")))
    }

    // ── Chord detection: slash chords ─────────────────────────────────────

    @Test
    fun `C major with G bass returns C over G`() {
        // G4, C5, E5. Bass G (chroma 7). 
        // Triad C-E-G found at root C (chroma 0).
        assertEquals("C/G", analyzer.analyze(listOf("G4", "C5", "E5")))
    }

    @Test
    fun `A minor 7 with G bass returns Am7 over G`() {
        // G3, A3, C4, E4. Bass G (chroma 7).
        // Am7 (A-C-E-G) found at root A (chroma 9).
        assertEquals("Am7/G", analyzer.analyze(listOf("G3", "A3", "C4", "E4")))
    }

    @Test
    fun `D minor with A bass returns Dm over A`() {
        // Sorted input from repository: ["A3", "D4", "F4"]. Bass A (chroma 9).
        // No chord type matches with A as root; Dm (D-F-A) found at root D (chroma 2).
        assertEquals("Dm/A", analyzer.analyze(listOf("A3", "D4", "F4")))
    }
}
