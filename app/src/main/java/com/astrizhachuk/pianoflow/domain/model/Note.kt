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
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        /**
         * Converts a MIDI pitch to a standard note name (e.g., 60 -> "C4").
         * Returns an empty string if the pitch is out of range.
         */
        fun pitchToName(pitch: Int): String {
            if (pitch !in 0..127) return ""
            val octave = (pitch / 12) - 1
            val noteName = NOTE_NAMES[pitch % 12]
            return "$noteName$octave"
        }
    }
}
