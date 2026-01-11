
package com.astrizhachuk.pianoflow.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import com.astrizhachuk.pianoflow.presentation.ui.main.MainActivity
import com.astrizhachuk.pianoflow.presentation.di.NotificationModule
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@UninstallModules(NotificationModule::class)
@HiltAndroidTest
class MidiConnectionUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var userNotifier: UserNotifier

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun whenMessageIsSent_itIsDisplayedOnScreen() {
        val testMessage = "This is a test message"

        activityRule.scenario.onActivity {
            (userNotifier as com.astrizhachuk.pianoflow.presentation.service.FakeUserNotifier).sendMessage(UserMessage(testMessage))
        }

        onView(withText(testMessage))
            .check(matches(isDisplayed()))
    }
}
