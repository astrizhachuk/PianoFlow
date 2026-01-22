package com.astrizhachuk.pianoflow.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.data.di.DataModule
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.repository.FakeMidiRepository
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import com.astrizhachuk.pianoflow.presentation.ui.main.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UninstallModules(DataModule::class)
@HiltAndroidTest
class MidiConnectionUITest {

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
    }

    @Test
    fun connectionSuccess_showsSuccessMessage() {
        // Given
        val device = MidiDevice(1, "Virtual MIDI", "PianoFlow", "Google")

        // When: Simulate a successful connection
        fakeMidiRepository.emitState(ConnectionState.Connected(device))

        // Then: Check that the UI has updated and shows the connected device name
        val expectedText = context.getString(R.string.midi_device_connected, device.name)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun connectionLost_showsDisconnectedMessage() {
        // Given: The device was connected
        val device = MidiDevice(1, "Virtual MIDI", "PianoFlow", "Google")
        fakeMidiRepository.emitState(ConnectionState.Connected(device))

        // When: Simulate connection loss
        fakeMidiRepository.emitState(ConnectionState.Disconnected)

        // Then: Check that the UI displays a disconnection message
        val expectedText = context.getString(R.string.midi_device_disconnected)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun connectionError_showsErrorMessage() {
        // When: Simulate a connection error
        val errorMessage = context.getString(R.string.midi_error_connection_failed)
        fakeMidiRepository.emitState(ConnectionState.Error(errorMessage))

        // Then: Check that an error message is displayed on the screen
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }
}
