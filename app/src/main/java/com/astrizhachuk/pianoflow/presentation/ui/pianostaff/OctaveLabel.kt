package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.annotation.StringRes
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.Note

/**
 * Maps a MIDI pitch to the string resource ID of its traditional octave name.
 *
 * Covers octaves 0 to 8 (the physical piano range, from Sub-contra to the five-lined octave).
 * Returns null for any pitch outside this range to indicate that no label should be displayed.
 *
 * @param midi The MIDI pitch value (typically 0..127).
 * @return The string resource ID of the octave name, or null if the octave is unnamed.
 */
@StringRes
internal fun octaveLabelResOrNull(midi: Int): Int? =
    when (Note.midiToOctave(midi)) {
        0 -> R.string.octave_sub_contra
        1 -> R.string.octave_contra
        2 -> R.string.octave_great
        3 -> R.string.octave_small
        4 -> R.string.octave_one_lined
        5 -> R.string.octave_two_lined
        6 -> R.string.octave_three_lined
        7 -> R.string.octave_four_lined
        8 -> R.string.octave_five_lined
        else -> null
    }
