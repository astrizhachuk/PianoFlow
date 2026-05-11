package com.astrizhachuk.pianoflow.domain.model

/**
 * Represents a parsed musical note and its properties.
 *
 * @param letter Note letter 'A'..'G'.
 * @param alter Accidental sign: -2 (bb), -1 (b), 0 (natural), +1 (#), +2 (x).
 * @param octave Octave number, or null if the input note name had no octave.
 * @param midi MIDI pitch (0..127) when octave is present and within range, else null.
 * @param chroma Pitch class 0..11 (semitones from C, modulo 12).
 */
internal data class Pitch(
    val letter: Char,
    val alter: Int,
    val octave: Int?,
    val midi: Int?,
    val chroma: Int
) {
    companion object {
        private val LETTER_TO_CHROMA = mapOf(
            'C' to 0, 'D' to 2, 'E' to 4, 'F' to 5,
            'G' to 7, 'A' to 9, 'B' to 11
        )
        private val REGEX = Regex("""^([A-Ga-g])([#b]+|x)?(-?\d+)?$""")

        /**
         * Parses a note name into a [Pitch]. Returns null for invalid input.
         *
         * Accepts: letter (A-G, case-insensitive), optional accidentals ([#b]+ or x), optional octave (-?\d+).
         * Examples: "C4", "B#4", "Cb4", "Cx4", "Ebb4", "F#", "C-1".
         */
        fun parse(name: String): Pitch? {
            val match = REGEX.matchEntire(name) ?: return null
            val letter = match.groupValues[1].uppercase()[0]
            val altStr = match.groupValues[2]
            val octStr = match.groupValues[3]

            val alter = when {
                altStr.isEmpty() -> 0
                altStr == "x" -> 2
                // [#b]+|x alternation in the regex means altStr is either "x" or a run of #/b chars — never mixed.
                // Reject mixed # and b (e.g. "C#b") which the regex allows through [#b]+.
                altStr.contains('#') && altStr.contains('b') -> return null
                else -> altStr.count { it == '#' } - altStr.count { it == 'b' }
            }

            val letterChroma = LETTER_TO_CHROMA[letter]!!
            val chroma = ((letterChroma + alter) % 12 + 12) % 12

            val octave = octStr.takeIf { it.isNotEmpty() }?.toInt()
            val midi = octave?.let { oct ->
                val m = (oct + 1) * 12 + letterChroma + alter
                if (m in 0..127) m else null
            }

            return Pitch(letter, alter, octave, midi, chroma)
        }
    }
}
