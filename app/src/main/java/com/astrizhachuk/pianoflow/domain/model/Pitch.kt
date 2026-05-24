package com.astrizhachuk.pianoflow.domain.model

/**
 * The seven natural note letters and their semitone offset from C.
 *
 * Enum names match the canonical note letters, so [valueOf] consumes the parser's
 * uppercased letter character directly.
 */
internal enum class NoteLetter(val chroma: Int) {
    C(0), D(2), E(4), F(5), G(7), A(9), B(11)
}

/**
 * A parsed musical note.
 *
 * Two views of the same note coexist on the same instance:
 * - Spelling: [letter] + [alter] + [octave] reflect how the input string is written.
 * - Sounding: [chroma] (pitch class) and [midi] reflect the actual sounding pitch and are
 *   derived deterministically from the spelling. For example `B#4` keeps `octave = 4` in the
 *   spelling but produces `chroma = 0` and `midi = 72` (sounding as C5).
 *
 * The canonical form stored on the instance is the spelling triple; sounding values are
 * computed properties, so inconsistent instances cannot exist.
 *
 * @param letter Natural note letter — restricted to A..G by the type.
 * @param alter Accidental in `-2..2` — bb (-2), b (-1), natural (0), # (+1), x (+2).
 * @param octave Octave number, or null when the input had no octave.
 */
internal data class Pitch(
    val letter: NoteLetter,
    val alter: Int,
    val octave: Int?
) {
    init {
        require(alter in -2..2) { "alter must be in -2..2, got $alter" }
    }

    /** Pitch class 0..11 (semitones from C, modulo 12). */
    val chroma: Int
        get() = (letter.chroma + alter).mod(SEMITONES_PER_OCTAVE)

    /** MIDI pitch in `0..127` when [octave] is present and the result fits, else null. */
    val midi: Int?
        get() {
            val oct = octave ?: return null
            // Long arithmetic guards against Int overflow for arbitrary octave values
            // admitted by the parser regex.
            val m = (oct.toLong() + 1) * SEMITONES_PER_OCTAVE + letter.chroma + alter
            return if (m in MIDI_RANGE) m.toInt() else null
        }

    companion object {
        private const val SEMITONES_PER_OCTAVE = 12
        private val MIDI_RANGE = 0..127
        private val REGEX = Regex("""^([A-Ga-g])(#{1,2}|b{1,2}|x)?(-?\d+)?$""")

        /**
         * Parses a note name into a [Pitch]. Returns null for invalid input.
         *
         * Accepts:
         * - letter A..G (case-insensitive)
         * - optional accidentals: `#`, `##`, `b`, `bb`, or `x` (≡ `##`)
         * - optional octave (signed integer)
         *
         * Examples: "C4", "B#4", "Cb4", "Cx4", "Ebb4", "F#", "C-1".
         */
        fun parse(name: String): Pitch? {
            val match = REGEX.matchEntire(name) ?: return null
            val letter = NoteLetter.valueOf(match.groupValues[1].uppercase())
            val altStr = match.groupValues[2]
            val octStr = match.groupValues[3]

            val alter = when {
                altStr.isEmpty() -> 0
                altStr == "x" -> 2
                altStr[0] == '#' -> altStr.length
                else -> -altStr.length
            }

            val octave = if (octStr.isEmpty()) null else octStr.toIntOrNull() ?: return null

            return Pitch(letter, alter, octave)
        }
    }
}
