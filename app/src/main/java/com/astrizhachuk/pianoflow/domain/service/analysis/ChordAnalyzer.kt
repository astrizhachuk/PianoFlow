package com.astrizhachuk.pianoflow.domain.service.analysis

import com.astrizhachuk.pianoflow.domain.model.Pitch
import javax.inject.Inject

/**
 * Domain service for native chord and single-note analysis. Pure Kotlin, platform-independent.
 *
 * Synchronous and main-safe. Stateless — all analysis is performed inside [analyze].
 */
class ChordAnalyzer @Inject constructor() {

    /**
     * Analyzes a list of note names to identify a chord or a simplified single note.
     *
     * The analysis follows these rules:
     * - Returns `null` if the list is empty or contains no valid note names.
     * - For a single valid note: returns the enharmonically simplified name (e.g., "E#4" becomes "F4").
     * - For two or more valid notes: attempts to identify a chord.
     * - If multiple chord interpretations exist, prioritizes the one where the first note in the
     *   input list is the bass note.
     * - Returns `null` if the combination of notes does not match any known [ChordTypeRegistry] pattern.
     *
     * @param noteNames A list of note strings (e.g., "C4", "Eb", "G#3").
     * @return The identified chord name (e.g., "Cm7", "G/B"), a simplified note name, or `null`.
     */
    fun analyze(noteNames: List<String>): String? {
        val parsed = noteNames.mapNotNull { name ->
            Pitch.parse(name)?.let { pitch -> name to pitch }
        }
        return when {
            parsed.isEmpty() -> null
            parsed.size == 1 -> simplify(parsed[0].second)
            else -> detectChord(parsed)
        }
    }

    private fun simplify(pitch: Pitch): String {
        val scale = if (pitch.alter > 0) SHARP_SCALE else FLAT_SCALE
        val noteName = scale[pitch.chroma]
        val midi = pitch.midi ?: return noteName
        val octave = (midi / 12) - 1
        return "$noteName$octave"
    }

    private fun detectChord(parsed: List<Pair<String, Pitch>>): String? {
        // Step 2: chroma → pitch-class name (first occurrence wins per chroma)
        val chromaToName = mutableMapOf<Int, String>()
        for ((name, pitch) in parsed) {
            chromaToName.putIfAbsent(pitch.chroma, stripOctave(name))
        }

        // Step 3: 12-bit bitmask from unique chromas only
        val bitmask = chromaToName.keys.fold(0) { acc, c -> acc or (1 shl c) }
        val bassChroma = parsed[0].second.chroma

        // Steps 4–6: try all 12 rotations
        val results = mutableListOf<Pair<Double, String>>()
        for (u in 0..11) {
            val rotated = rotate12(bitmask, u)
            if (rotated and 1 == 0) continue                       // root not present at this rotation
            val types = ChordTypeRegistry.byChroma[rotated] ?: continue
            val rootName = chromaToName[u] ?: continue             // root must be an input note

            for (type in types) {
                if (u == bassChroma) {
                    results += 1.0 to "$rootName${type.symbol}"
                } else {
                    val bassName = chromaToName[bassChroma]!!
                    results += 0.5 to "$rootName${type.symbol}/$bassName"
                }
            }
        }

        if (results.isEmpty()) return null

        // Step 7: pick first result with highest weight (preserves rotation/insertion order)
        val maxWeight = results.maxOf { it.first }
        val best = results.first { it.first == maxWeight }.second

        // Step 8: strip trailing M for root-position major chord compatibility
        return if (best.endsWith("M")) best.dropLast(1) else best
    }

    private fun rotate12(mask: Int, u: Int): Int {
        if (u == 0) return mask
        return ((mask ushr u) or (mask shl (12 - u))) and 0xFFF
    }

    private fun stripOctave(name: String): String =
        STRIP_OCTAVE_RE.replace(name, "")

    companion object {
        private val STRIP_OCTAVE_RE = Regex("""-?\d+$""")

        private val SHARP_SCALE = arrayOf(
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        )
        private val FLAT_SCALE = arrayOf(
            "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
        )
    }
}
