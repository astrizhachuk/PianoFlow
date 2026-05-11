package com.astrizhachuk.pianoflow.domain.service.analysis

import com.astrizhachuk.pianoflow.domain.model.ChordType

/**
 * A registry of known musical chord types mapped to their pitch-class chroma patterns.
 *
 * This registry uses a bitmask (chroma) to represent the intervals present in a chord,
 * where each bit corresponds to a semitone relative to the root (bit 0 = root, bit 1 = m2, etc.).
 *
 * It provides a comprehensive list of common and complex chords across various families,
 * including Major, Minor, Augmented, Quartal, and Altered chords. Since different musical
 * contexts can use different names for the same set of notes (enharmonic or functional equivalents),
 * the registry handles collisions by mapping a single chroma to multiple [ChordType] definitions.
 *
 * @property all A complete list of all registered [ChordType] instances.
 * @property byChroma A lazy-initialized map for efficient lookup of chord names by their integer bitmask.
 */
internal object ChordTypeRegistry {

    // Convert spec chroma string to Int: spec writes chroma-0 at leftmost position,
    // standard binary has it at bit-0 (LSB), so we reverse before parsing.
    private fun c(s: String): Int = s.reversed().toInt(2)

    val all: List<ChordType> = listOf(
        // ── Unknown / quartal ────────────────────────────────────────────────
        ChordType(c("100000010000"), "5"),
        ChordType(c("100001001001"), "M7#5sus4"),
        ChordType(c("100001001010"), "7#5sus4"),
        ChordType(c("100001010000"), "sus4"),
        ChordType(c("100001010001"), "M7sus4"),
        ChordType(c("100001010010"), "7sus4"),
        // ── Major family ─────────────────────────────────────────────────────
        ChordType(c("100010000010"), "7no5"),
        ChordType(c("100010001000"), "aug"),
        ChordType(c("100010001001"), "M7b6"),
        ChordType(c("100010001001"), "maj7#5"),   // collision: same chroma as M7b6
        ChordType(c("100010001010"), "7#5"),
        ChordType(c("100010001010"), "7b13"),      // collision: same chroma as 7#5
        ChordType(c("100010010000"), "M"),
        ChordType(c("100010010001"), "maj7"),
        ChordType(c("100010010010"), "7"),
        ChordType(c("100010010100"), "6"),
        ChordType(c("100010010110"), "7add6"),
        ChordType(c("100010011010"), "7b6"),
        ChordType(c("100010100000"), "Mb5"),
        ChordType(c("100010100001"), "M7b5"),
        ChordType(c("100010100010"), "7b5"),
        ChordType(c("100010110001"), "maj#4"),
        ChordType(c("100010110010"), "7#11"),
        ChordType(c("100010110100"), "M6#11"),
        ChordType(c("100010111010"), "7#11b13"),
        // ── Minor / augmented ────────────────────────────────────────────────
        ChordType(c("100100001000"), "m#5"),
        ChordType(c("100100001001"), "mb6M7"),
        ChordType(c("100100001010"), "m7#5"),
        ChordType(c("100100010000"), "m"),
        ChordType(c("100100010001"), "m/ma7"),
        ChordType(c("100100010010"), "m7"),
        ChordType(c("100100010100"), "m6"),
        ChordType(c("100100011001"), "mMaj7b6"),
        ChordType(c("100100100000"), "dim"),
        ChordType(c("100100100001"), "oM7"),
        ChordType(c("100100100010"), "m7b5"),
        ChordType(c("100100100100"), "dim7"),
        ChordType(c("100100100101"), "o7M7"),
        // ── Quartal / minor add ──────────────────────────────────────────────
        ChordType(c("100101000010"), "4"),
        ChordType(c("100101010000"), "madd4"),
        ChordType(c("100101010010"), "m7add11"),
        // ── Augmented + sharp 9 ─────────────────────────────────────────────
        ChordType(c("100110001000"), "+add#9"),
        ChordType(c("100110001010"), "7#5#9"),
        ChordType(c("100110010010"), "7#9"),
        ChordType(c("100110010110"), "13#9"),
        ChordType(c("100110011010"), "7#9b13"),
        ChordType(c("100110110001"), "maj7#9#11"),
        ChordType(c("100110110010"), "7#9#11"),
        ChordType(c("100110110110"), "13#9#11"),
        ChordType(c("100110111010"), "7#9#11b13"),
        // ── sus2 / 9th family ────────────────────────────────────────────────
        ChordType(c("101000010000"), "sus2"),
        ChordType(c("101001001001"), "M9#5sus4"),
        ChordType(c("101001010000"), "sus24"),
        ChordType(c("101001010001"), "M9sus4"),
        ChordType(c("101001010010"), "11"),
        ChordType(c("101001010010"), "9sus4"),        // collision: same chroma as 11
        ChordType(c("101001010110"), "13sus4"),
        // ── Major 9th / 13th family ──────────────────────────────────────────
        ChordType(c("101010000010"), "9no5"),
        ChordType(c("101010000110"), "13no5"),
        ChordType(c("101010001000"), "M#5add9"),
        ChordType(c("101010001001"), "maj9#5"),
        ChordType(c("101010001010"), "9#5"),
        ChordType(c("101010001010"), "9b13"),         // collision: same chroma as 9#5
        ChordType(c("101010010000"), "Madd9"),
        ChordType(c("101010010001"), "maj9"),
        ChordType(c("101010010010"), "9"),
        ChordType(c("101010010100"), "6add9"),
        ChordType(c("101010010101"), "maj13"),
        ChordType(c("101010010101"), "M7add13"),      // collision: same chroma as maj13
        ChordType(c("101010010110"), "13"),
        ChordType(c("101010100001"), "M9b5"),
        ChordType(c("101010100010"), "9b5"),
        ChordType(c("101010100110"), "13b5"),
        ChordType(c("101010101010"), "9#5#11"),
        ChordType(c("101010110001"), "maj9#11"),
        ChordType(c("101010110010"), "9#11"),
        ChordType(c("101010110100"), "69#11"),
        ChordType(c("101010110101"), "M13#11"),
        ChordType(c("101010110110"), "13#11"),
        ChordType(c("101010111010"), "9#11b13"),
        // ── Minor 9th / 13th family ──────────────────────────────────────────
        ChordType(c("101100001010"), "m9#5"),
        ChordType(c("101100010000"), "madd9"),
        ChordType(c("101100010001"), "mM9"),
        ChordType(c("101100010010"), "m9"),
        ChordType(c("101100010100"), "m69"),
        ChordType(c("101100010110"), "m13"),
        ChordType(c("101100011001"), "mMaj9b6"),
        ChordType(c("101100100010"), "m9b5"),
        ChordType(c("101101001010"), "m11A"),
        ChordType(c("101101010010"), "m11"),
        // ── Phrygian / flat-9 family ─────────────────────────────────────────
        ChordType(c("110001010010"), "b9sus"),
        ChordType(c("110001010010"), "11b9"),         // collision: same chroma as b9sus
        ChordType(c("110001011010"), "7sus4b9b13"),
        ChordType(c("110010000010"), "alt7"),
        ChordType(c("110010001010"), "7#5b9"),
        ChordType(c("110010010000"), "Maddb9"),
        ChordType(c("110010010001"), "M7b9"),
        ChordType(c("110010010010"), "7b9"),
        ChordType(c("110010010110"), "13b9"),
        ChordType(c("110010011010"), "7b9b13"),
        ChordType(c("110010101010"), "7#5b9#11"),
        ChordType(c("110010110010"), "7b9#11"),
        ChordType(c("110010110110"), "13b9#11"),
        ChordType(c("110010111010"), "7b9b13#11"),
        ChordType(c("110100001000"), "mb6b9"),
        ChordType(c("110110010010"), "7b9#9")
    )

    val byChroma: Map<Int, List<ChordType>> by lazy {
        all.groupBy { it.chroma }
    }
}
