package com.astrizhachuk.pianoflow.domain.model

/**
 * One chord type from the registry.
 *
 * @param chroma 12-bit pitch-class bitmask; bit i is set iff the interval of i semitones from the root is present. Bit 0 is always set (root present).
 * @param symbol Primary chord symbol appended to the root note name (e.g. "M", "m", "7").
 */
internal data class ChordType(
    val chroma: Int,
    val symbol: String
)
