package com.astrizhachuk.pianoflow.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
        onView(withText(context.getString(R.string.midi_device_connected, device.name)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun connectionLost_showsDisconnectedMessage() {
        // Given: The device was connected
        val device = MidiDevice(1, "Virtual MIDI", "PianoFlow", "Google")
        fakeMidiRepository.emitState(ConnectionState.Connected(device))

        // When: Simulate connection loss
        fakeMidiRepository.emitState(ConnectionState.Disconnected)

        // Then: Check that the UI displays a disconnection message
        onView(withText(context.getString(R.string.midi_device_disconnected)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun connectionError_showsErrorMessage() {
        // When: Simulate a connection error
        val errorMessage = context.getString(R.string.midi_error_connection_failed)
        fakeMidiRepository.emitState(ConnectionState.Error(errorMessage))

        // Then: Check that an error message is displayed on the screen
        onView(withText(errorMessage))
            .check(matches(isDisplayed()))
    }
}
