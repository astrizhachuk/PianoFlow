package com.astrizhachuk.pianoflow.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.astrizhachuk.pianoflow.data.di.DataModule
import com.astrizhachuk.pianoflow.data.repository.ChordAnalysisRepositoryImpl
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.repository.FakeMidiRepository
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import com.astrizhachuk.pianoflow.domain.service.analysis.ChordAnalyzer
import com.astrizhachuk.pianoflow.presentation.ui.main.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.inject.Singleton

@RunWith(Parameterized::class)
@UninstallModules(DataModule::class)
@HiltAndroidTest
class ChordRecognitionUITest(
    private val chordName: String,
    private val expectedDisplay: String,
    private val midiPitches: IntArray
) {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val fakeMidiRepository = FakeMidiRepository()

    @BindValue
    val midiRepository: MidiRepository = fakeMidiRepository

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext<Context>()

        fakeMidiRepository.emitState(
            ConnectionState.Connected(
                MidiDevice(1, "Virtual MIDI", "TestPiano", "TestManufacturer")
            )
        )
    }

    @Test
    fun chord_showsCorrectName() {
        // When: Play the chord notes
        midiPitches.forEach { pitch ->
            fakeMidiRepository.sendRawMidi(noteOnBytes(pitch))
        }

        // Then: Wait for chord window (50ms) + processing and verify UI
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(expectedDisplay).assertIsDisplayed()
    }

    private fun noteOnBytes(pitch: Int, velocity: Int = 100, channel: Int = 0): ByteArray {
        return byteArrayOf(
            (0x90 or channel).toByte(),
            pitch.toByte(),
            velocity.toByte()
        )
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDataModule {

        @Provides
        @Singleton
        fun provideChordAnalysisRepository(): ChordAnalysisRepository {
            return ChordAnalysisRepositoryImpl(ChordAnalyzer())
        }
    }

    companion object {

        // MIDI pitches: C4=60, C#=61, D=62, D#=63, E=64, F=65, F#=66, G=67, G#=68, A=69, A#=70, B=71

        @JvmStatic
        @Parameterized.Parameters(name = "{0} → {1}")
        fun chords(): Collection<Array<Any>> = listOf(
            arrayOf("Major triad", "C", intArrayOf(60, 64, 67)),
            arrayOf("Minor triad", "Cm", intArrayOf(60, 63, 67)),
            arrayOf("Diminished triad", "Cdim", intArrayOf(60, 63, 66)),
            arrayOf("Augmented triad", "Caug", intArrayOf(60, 64, 68)),
            arrayOf("sus2", "Csus2", intArrayOf(60, 62, 67)),
            arrayOf("sus4", "Csus4", intArrayOf(60, 65, 67)),
            arrayOf("Dominant 7", "C7", intArrayOf(60, 64, 67, 70)),
            arrayOf("Major 7", "Cmaj7", intArrayOf(60, 64, 67, 71)),
            arrayOf("Minor 7", "Cm7", intArrayOf(60, 63, 67, 70)),
            arrayOf("Half-diminished", "Cm7b5", intArrayOf(60, 63, 66, 70)),
            arrayOf("Unknown chord", "?", intArrayOf(60, 61))
        )
    }
}
