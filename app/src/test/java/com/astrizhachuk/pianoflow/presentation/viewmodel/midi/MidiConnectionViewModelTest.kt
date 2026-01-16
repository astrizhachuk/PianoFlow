package com.astrizhachuk.pianoflow.presentation.viewmodel.midi

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.usecase.midi.ShowConnectionNotificationUseCase
import com.astrizhachuk.pianoflow.domain.usecase.midi.TrackMidiConnectionUseCase
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MidiConnectionViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var trackMidiConnectionUseCase: TrackMidiConnectionUseCase

    @RelaxedMockK
    private lateinit var showConnectionNotificationUseCase: ShowConnectionNotificationUseCase

    @RelaxedMockK
    private lateinit var userNotifier: UserNotifier

    @Test
    fun `when use case emits new connection state then view model state is updated`() = runTest {
        // Arrange
        val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        assertEquals(ConnectionState.NoDevice, viewModel.connectionState.value)
        val mockDevice = mockk<MidiDevice>()
        val connectedState = ConnectionState.Connected(mockDevice)

        // Act
        connectionStateFlow.value = connectedState

        // Assert
        assertEquals(connectedState, viewModel.connectionState.value)
    }

    @Test
    fun `when connection state changes then show notification use case is called`() = runTest {
        // Arrange
        val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val disconnectedState = ConnectionState.Disconnected

        // Act
        connectionStateFlow.value = disconnectedState

        // Assert
        coVerify { showConnectionNotificationUseCase(disconnectedState) }
    }

    @Test
    fun `when notification use case returns a message then user notifier sends it`() = runTest {
        // Arrange
        val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
        val testMessage = UserMessage("Test Message")
        coEvery { showConnectionNotificationUseCase(any()) } returns testMessage
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )

        // Act
        connectionStateFlow.value = ConnectionState.Disconnected

        // Assert
        coVerify { userNotifier.sendMessage(testMessage) }
    }
}
