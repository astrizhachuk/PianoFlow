package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.annotation.StringRes
import com.astrizhachuk.pianoflow.R

private const val SEMITONES_PER_OCTAVE = 12

/**
 * Maps a MIDI pitch to the string resource of its traditional octave name.
 *
 * The scientific octave number is `pitch / 12 - 1`. Only octaves 0..8 (A0..C8, the
 * physical piano range) have a named label; any pitch outside that range returns null
 * so the caller can hide the octave line.
 *
 * @param midi MIDI pitch (0..127).
 * @return The `@StringRes` id of the octave name, or null when the octave is unnamed.
 */
@StringRes
internal fun octaveLabelResOrNull(midi: Int): Int? =
    when (midi / SEMITONES_PER_OCTAVE - 1) {
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
