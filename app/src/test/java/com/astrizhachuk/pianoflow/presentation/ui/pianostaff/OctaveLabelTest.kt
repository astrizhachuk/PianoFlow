package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OctaveLabelTest {

    @Test
    fun `maps each octave boundary C to its resource`() {
        assertEquals(R.string.octave_sub_contra, octaveLabelResOrNull(21))  // A0
        assertEquals(R.string.octave_contra, octaveLabelResOrNull(24))      // C1
        assertEquals(R.string.octave_great, octaveLabelResOrNull(36))       // C2
        assertEquals(R.string.octave_small, octaveLabelResOrNull(48))       // C3
        assertEquals(R.string.octave_one_lined, octaveLabelResOrNull(60))   // C4
        assertEquals(R.string.octave_two_lined, octaveLabelResOrNull(72))   // C5
        assertEquals(R.string.octave_three_lined, octaveLabelResOrNull(84)) // C6
        assertEquals(R.string.octave_four_lined, octaveLabelResOrNull(96))  // C7
        assertEquals(R.string.octave_five_lined, octaveLabelResOrNull(108)) // C8
    }

    @Test
    fun `B8 still maps to five-lined, C9 and above are null`() {
        assertEquals(R.string.octave_five_lined, octaveLabelResOrNull(119)) // B8
        assertNull(octaveLabelResOrNull(120)) // C9
        assertNull(octaveLabelResOrNull(127)) // top of MIDI range
    }

    @Test
    fun `pitches below A0 octave are null`() {
        assertNull(octaveLabelResOrNull(11)) // octave -1
        assertNull(octaveLabelResOrNull(0))
    }
}
