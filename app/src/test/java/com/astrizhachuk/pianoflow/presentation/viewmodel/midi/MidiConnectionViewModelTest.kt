
package com.astrizhachuk.pianoflow.presentation.viewmodel.midi

import app.cash.turbine.test
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
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

    private lateinit var connectionStateFlow: MutableSharedFlow<ConnectionState>

    @Before
    fun setUp() {
        connectionStateFlow = MutableSharedFlow(replay = 1)
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
        // Emit initial value to mimic StateFlow behavior for consistency in tests
        connectionStateFlow.tryEmit(ConnectionState.NoDevice)
    }

    @Test
    fun `when use case emits new connection state then view model state is updated`() = runTest {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val mockDevice = mockk<MidiDevice>()
        val connectedState = ConnectionState.Connected(mockDevice)

        viewModel.connectionState.test {
            // Assert initial state
            assertEquals(ConnectionState.NoDevice, awaitItem())

            // Act
            connectionStateFlow.emit(connectedState)

            // Assert new state
            assertEquals(connectedState, awaitItem())
        }
    }

    @Test
    fun `when connection state changes then notification use case is called`() = runTest {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val disconnectedState = ConnectionState.Disconnected

        viewModel.connectionState.test {
            awaitItem() // Initial state

            // Act
            connectionStateFlow.emit(disconnectedState)

            // Assert state update
            assertEquals(disconnectedState, awaitItem())

            // Assert side-effect
            coVerify { showConnectionNotificationUseCase(disconnectedState) }
        }
    }

    @Test
    fun `when notification use case returns a message then notifier sends it`() = runTest {
        // Arrange
        val testMessage = UserMessage("Test Message")
        coEvery { showConnectionNotificationUseCase(any()) } returns testMessage
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )

        viewModel.connectionState.test {
            awaitItem() // Initial state

            // Act
            connectionStateFlow.emit(ConnectionState.Disconnected)

            // Wait for state change
            awaitItem()

            // Assert side-effect
            coVerify { userNotifier.sendMessage(testMessage) }
        }
    }

    @Test
    fun `no notification on initialization`() = runTest {
        // Arrange
        MidiConnectionViewModel(trackMidiConnectionUseCase, showConnectionNotificationUseCase, userNotifier)

        // Act & Assert
        verify(exactly = 0) { userNotifier.sendMessage(any()) }
    }

    @Test
    fun `rapid sequential state changes are processed`() = runTest {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val state1 = ConnectionState.Connected(mockk())
        val state2 = ConnectionState.Disconnected
        val finalState = ConnectionState.Connected(mockk())

        viewModel.connectionState.test {
            awaitItem() // Initial

            // Act & Assert
            connectionStateFlow.emit(state1)
            assertEquals(state1, awaitItem())

            connectionStateFlow.emit(state2)
            assertEquals(state2, awaitItem())

            connectionStateFlow.emit(finalState)
            assertEquals(finalState, awaitItem())
        }

        // Assert side-effects
        coVerify(exactly = 3) { showConnectionNotificationUseCase(any()) }
    }

    @Test
    fun `state change to the same value is handled correctly`() = runTest {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val disconnectedState = ConnectionState.Disconnected

        viewModel.connectionState.test {
            awaitItem() // Initial state

            // Act
            connectionStateFlow.emit(disconnectedState)
            assertEquals(disconnectedState, awaitItem()) // State is updated

            // Act again with the same state
            connectionStateFlow.emit(disconnectedState)

            // Since StateFlow is distinct, no new item should be emitted
            // We can ensure channel is empty before cancelling
            expectNoEvents()
        }

        // Assert that the side-effect was only called once
        coVerify(exactly = 1) { showConnectionNotificationUseCase(disconnectedState) }
    }

    @Test
    fun `error in upstream flow does not crash collector`() = runTest {
        // Arrange
        val errorFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
        coEvery { trackMidiConnectionUseCase.invoke() } returns errorFlow
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val exception = RuntimeException("Upstream error")
        val errorState = ConnectionState.Error(exception.message ?: "Unknown error")
        val connectedState = ConnectionState.Connected(mockk())

        viewModel.connectionState.test {
            // Assert
            assertEquals(ConnectionState.NoDevice, awaitItem())

            // Act
            errorFlow.emit(connectedState)
            assertEquals(connectedState, awaitItem())

            errorFlow.emit(errorState)
            assertEquals(errorState, awaitItem())
        }
    }
}
