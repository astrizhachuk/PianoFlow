package com.astrizhachuk.pianoflow.domain.usecase.midi

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

class ShowConnectionNotificationUseCaseTest {

    private val context: Context = mockk()
    private val useCase = ShowConnectionNotificationUseCase(context)

    private val disconnectedMessage = "MIDI device disconnected"
    private val connectedMessage = "MIDI device connected: %s"

    @Test
    fun `invoke with Connected state returns connected message from resources`() {
        // Arrange
        val deviceName = "My MIDI Keyboard"
        val device = MidiDevice(id = 1, name = deviceName, product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)
        every { context.getString(R.string.midi_device_connected, deviceName) } returns connectedMessage.format(deviceName)

        // Act
        val result = useCase(state)

        // Assert
        assertEquals(connectedMessage.format(deviceName), result.text)
    }

    @Test
    fun `invoke with Connected state and empty device name returns message with empty name`() {
        // Arrange
        val deviceName = ""
        val device = MidiDevice(id = 1, name = deviceName, product = "Test Product", manufacturer = "Test Manufacturer")
        val state = ConnectionState.Connected(device)
        every { context.getString(R.string.midi_device_connected, deviceName) } returns connectedMessage.format(deviceName)

        // Act
        val result = useCase(state)

        // Assert
        assertEquals(connectedMessage.format(deviceName), result.text)
    }

    @Test
    fun `invoke with Disconnected state returns disconnected message from resources`() {
        // Arrange
        val state = ConnectionState.Disconnected
        every { context.getString(R.string.midi_device_disconnected) } returns disconnectedMessage

        // Act
        val result = useCase(state)

        // Assert
        assertEquals(disconnectedMessage, result.text)
    }

    @Test
    fun `invoke with NoDevice state returns disconnected message from resources`() {
        // Arrange
        val state = ConnectionState.NoDevice
        every { context.getString(R.string.midi_device_disconnected) } returns disconnectedMessage

        // Act
        val result = useCase(state)

        // Assert
        assertEquals(disconnectedMessage, result.text)
    }

    @Test
    fun `invoke with Error state and message returns error message`() {
        // Arrange
        val errorMessage = "Something went wrong"
        val state = ConnectionState.Error(errorMessage)

        // Act
        val result = useCase(state)

        // Assert
        assertEquals(errorMessage, result.text)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, Build.VERSION_CODES.TIRAMISU])
class ShowConnectionNotificationUseCaseIntegrationTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val shadowNotificationManager: ShadowNotificationManager = shadowOf(notificationManager)

    @Test
    fun `use case sends message that results in a correct system notification`() = runTest {
        // Arrange
        val deviceName = "My MIDI Keyboard"
        val expectedTitle = "MIDI Connection"
        val expectedMessage = context.getString(R.string.midi_device_connected, deviceName)
        val notification = NotificationCompat.Builder(context, "midi_channel")
            .setContentTitle(expectedTitle)
            .setContentText(expectedMessage)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        // Act
        notificationManager.notify(1, notification)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        val postedNotification = shadowNotificationManager.getNotification(1)
        assertNotNull("Notification should have been posted but was null", postedNotification)

        val shadowNotification = shadowOf(postedNotification)
        assertEquals(expectedTitle, shadowNotification.contentTitle)
        assertEquals(expectedMessage, shadowNotification.contentText)
        assertEquals(R.drawable.ic_launcher_foreground, postedNotification.icon)
    }
}