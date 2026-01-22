package com.astrizhachuk.pianoflow.domain.model

/**
 * Domain model representing a single MIDI note.
 *
 * @param pitch MIDI note number (from 0 to 127).
 */
data class Note(
    val pitch: Int
)
