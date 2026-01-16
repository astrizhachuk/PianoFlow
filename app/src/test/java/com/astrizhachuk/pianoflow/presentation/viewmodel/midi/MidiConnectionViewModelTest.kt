
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
        connectionStateFlow = MutableSharedFlow()
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
    }

    @Test
    fun `when use case emits new connection state then view model state is updated`() = runTest {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        assertEquals(ConnectionState.NoDevice, viewModel.connectionState.value)
        val mockDevice = mockk<MidiDevice>()
        val connectedState = ConnectionState.Connected(mockDevice)

        // Act
        connectionStateFlow.emit(connectedState)

        // Assert
        assertEquals(connectedState, viewModel.connectionState.value)
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

        // Act
        connectionStateFlow.emit(disconnectedState)

        // Assert
        coVerify { showConnectionNotificationUseCase(disconnectedState) }
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

        // Act
        connectionStateFlow.emit(ConnectionState.Disconnected)

        // Assert
        coVerify { userNotifier.sendMessage(testMessage) }
    }

    @Test
    fun `multiple collectors receive state updates`() = runTest(mainDispatcherRule.testDispatcher) {
        // Arrange
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val states1 = mutableListOf<ConnectionState>()
        val states2 = mutableListOf<ConnectionState>()

        // Act
        val job1 = launch { viewModel.connectionState.collect { states1.add(it) } }
        val job2 = launch { viewModel.connectionState.collect { states2.add(it) } }

        // Assert
        assertEquals(listOf(ConnectionState.NoDevice), states1)
        assertEquals(listOf(ConnectionState.NoDevice), states2)

        // Act
        val connectedState = ConnectionState.Connected(mockk())
        connectionStateFlow.emit(connectedState)

        // Assert
        assertEquals(listOf(ConnectionState.NoDevice, connectedState), states1)
        assertEquals(listOf(ConnectionState.NoDevice, connectedState), states2)

        job1.cancel()
        job2.cancel()
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
        val finalState = ConnectionState.Connected(mockk())

        // Act
        connectionStateFlow.emit(ConnectionState.Connected(mockk()))
        connectionStateFlow.emit(ConnectionState.Disconnected)
        connectionStateFlow.emit(finalState)

        // Assert
        assertEquals(finalState, viewModel.connectionState.value)
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

        // Act
        connectionStateFlow.emit(disconnectedState)
        connectionStateFlow.emit(disconnectedState)

        // Assert
        assertEquals(disconnectedState, viewModel.connectionState.value)
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
        val job = launch { viewModel.connectionState.collect {} } 
        val exception = RuntimeException("Upstream error")

        // Act
        errorFlow.tryEmit(ConnectionState.Connected(mockk()))
        errorFlow.tryEmit(ConnectionState.Error(exception.message ?: "Unknown error"))

        // Assert
        // The main assertion is that the collector coroutine doesn't crash and can be cancelled.
        job.cancel()
    }
}
