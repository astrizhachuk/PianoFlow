package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain model representing a single MIDI note.
 *
 * @param pitch MIDI note number (from 0 to 127).
 * @param name Musical note name with octave (e.g., "C4", "G#3").
 */
data class Note(
    val pitch: Int,
    val name: String
) {
    companion object {
        private const val SEMITONES_PER_OCTAVE = 12
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        /** Scientific octave number for a MIDI pitch (e.g., 60 -> 4). */
        fun midiToOctave(pitch: Int): Int = pitch / SEMITONES_PER_OCTAVE - 1

        /**
         * Converts a MIDI pitch to a standard note name (e.g., 60 -> "C4").
         * Returns an empty string if the pitch is out of range.
         */
        fun pitchToName(pitch: Int): String {
            if (pitch !in 0..127) return ""
            val octave = midiToOctave(pitch)
            val noteName = NOTE_NAMES[pitch % SEMITONES_PER_OCTAVE]
            return "$noteName$octave"
        }
    }
}
