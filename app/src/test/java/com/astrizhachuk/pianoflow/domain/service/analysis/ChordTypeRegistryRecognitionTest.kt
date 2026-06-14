package com.astrizhachuk.pianoflow.domain.service.analysis

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Exhaustive recognition check: every entry in [ChordTypeRegistry] (all 106), played in
 * root position from C, must be recognized by [ChordAnalyzer] and render as the canonical
 * name for its chroma.
 *
 * This is the fast, JVM-level counterpart to `ChordRecognitionUITest`: the UI test verifies the
 * end-to-end wiring (MIDI bytes → analyzer → Compose) on a handful of representative chords,
 * while this test asserts that the recognition logic itself covers the whole registry — with no
 * device and sub-millisecond per case.
 *
 * "Canonical" = the first symbol registered for a chroma. When several chord types share one
 * chroma (enharmonic/functional collisions, e.g. `M7b6` vs `maj7#5`), [ChordAnalyzer] returns the
 * first registered one, so collision-second entries deterministically resolve to the first name.
 */
@RunWith(Parameterized::class)
class ChordTypeRegistryRecognitionTest(
    private val symbol: String,
    private val chroma: Int
) {

    @Test
    fun `chord type is recognized from C root position`() {
        val notes = notesFromChroma(chroma)
        assertEquals(expectedDisplay(chroma), ChordAnalyzer().analyze(notes))
    }

    companion object {
        private val SHARP = arrayOf(
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        )

        /**
         * Builds the root-position voicing from C for a chroma bitmask. Bit 0 (root C) is always
         * set and is the lowest note, so C is the bass — which is the order the analyzer expects
         * (it treats the first element as the bass).
         */
        private fun notesFromChroma(chroma: Int): List<String> =
            (0 until 12).filter { (chroma shr it) and 1 == 1 }.map { "${SHARP[it]}4" }

        /**
         * The name the analyzer is expected to produce. The analyzer strips a trailing `M` (only
         * the exact `M` symbol) and, for a chroma shared by several types, returns the first one
         * registered.
         */
        private fun expectedDisplay(chroma: Int): String {
            val symbol = ChordTypeRegistry.byChroma.getValue(chroma).first().symbol
            return "C" + symbol.takeIf { it != "M" }.orEmpty()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0} (chroma={1})")
        fun chordTypes(): Collection<Array<Any>> =
            ChordTypeRegistry.all.map { arrayOf(it.symbol, it.chroma) }
    }
}
