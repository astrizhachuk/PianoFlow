package com.astrizhachuk.pianoflow.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.presentation.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class RealUserNotifierIntegrationTest {

    // Правила Hilt и Activity остаются такими же.
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
    fun whenRealNotifierSendsMessage_SnackbarIsDisplayedOnScreen() {
        val testMessage = "Это реальное сообщение от UserNotifier"

        activityRule.scenario.onActivity {
            userNotifier.sendMessage(UserMessage(testMessage))
        }

        onView(withText(testMessage))
            .check(matches(isDisplayed()))
    }
}