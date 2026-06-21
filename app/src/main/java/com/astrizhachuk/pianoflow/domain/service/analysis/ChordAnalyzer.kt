package com.astrizhachuk.pianoflow.domain.service.analysis

import com.astrizhachuk.pianoflow.domain.model.Note
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
            Pitch.parse(name)?.let { name to it }
        }
        return when (parsed.size) {
            0 -> null
            1 -> simplify(parsed[0].second)
            else -> detectChord(parsed)
        }
    }

    private fun simplify(pitch: Pitch): String {
        val scale = if (pitch.alter > 0) SHARP_SCALE else FLAT_SCALE
        val noteName = scale[pitch.chroma]
        val midi = pitch.midi ?: return noteName
        val octave = Note.midiToOctave(midi)
        return "$noteName$octave"
    }

    private fun detectChord(parsed: List<Pair<String, Pitch>>): String? {
        // Map each unique chroma to its first occurrence's name without octave.
        // This preserves the user's notation (e.g., "Cb" vs "B").
        val chromaToName = parsed.distinctBy { it.second.chroma }
            .associate { it.second.chroma to stripOctave(it.first) }

        // 12-bit bitmask from unique chromas.
        val bitmask = chromaToName.keys.fold(0) { acc, c -> acc or (1 shl c) }
        val bassChroma = parsed[0].second.chroma
        val bassName = chromaToName.getValue(bassChroma)

        // Find all possible chord interpretations across all potential roots.
        // We prioritize the root matching the bass note by assigning a higher weight.
        return chromaToName.asSequence()
            .flatMap { (u, rootName) ->
                val rotated = rotate12(bitmask, u)
                val types = ChordTypeRegistry.byChroma[rotated] ?: return@flatMap emptySequence()

                types.asSequence().map { type ->
                    val isBass = u == bassChroma
                    val symbol = type.symbol.takeIf { it != "M" } ?: ""
                    val fullName = if (isBass) "$rootName$symbol" else "$rootName$symbol/$bassName"
                    val weight = if (isBass) 1.0 else 0.5
                    weight to fullName
                }
            }
            .maxByOrNull { it.first }
            ?.second
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
