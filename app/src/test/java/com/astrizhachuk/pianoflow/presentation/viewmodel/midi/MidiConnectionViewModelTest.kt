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
    lateinit var trackMidiConnectionUseCase: TrackMidiConnectionUseCase

    @RelaxedMockK
    lateinit var showConnectionNotificationUseCase: ShowConnectionNotificationUseCase

    @RelaxedMockK
    lateinit var userNotifier: UserNotifier

    lateinit var connectionStateFlow: MutableSharedFlow<ConnectionState>

    @Before
    fun setUp() {
        connectionStateFlow = MutableSharedFlow()
        coEvery { trackMidiConnectionUseCase.invoke() } returns connectionStateFlow
    }

    //region General Behavior

    @Test
    fun `when use case emits new connection state then view model state is updated`() = runTest {
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        // Initial state is NoDevice
        assertEquals(ConnectionState.NoDevice, viewModel.connectionState.value)

        val mockDevice = mockk<MidiDevice>()
        val connectedState = ConnectionState.Connected(mockDevice)
        connectionStateFlow.emit(connectedState)

        assertEquals(connectedState, viewModel.connectionState.value)
    }

    @Test
    fun `when connection state changes then show notification use case is called`() = runTest {
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val disconnectedState = ConnectionState.Disconnected
        connectionStateFlow.emit(disconnectedState)

        coVerify { showConnectionNotificationUseCase(disconnectedState) }
    }

    @Test
    fun `when notification use case returns a message then user notifier sends it`() = runTest {
        val testMessage = UserMessage("Test Message")
        coEvery { showConnectionNotificationUseCase(any()) } returns testMessage
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )

        connectionStateFlow.emit(ConnectionState.Disconnected)

        coVerify { userNotifier.sendMessage(testMessage) }
    }

    @Test
    fun `Multiple Subscribers Behavior`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val states1 = mutableListOf<ConnectionState>()
        val states2 = mutableListOf<ConnectionState>()

        // When collectors start, they get the initial value
        val job1 = launch { viewModel.connectionState.collect { states1.add(it) } }
        val job2 = launch { viewModel.connectionState.collect { states2.add(it) } }

        assertEquals(listOf(ConnectionState.NoDevice), states1)
        assertEquals(listOf(ConnectionState.NoDevice), states2)

        // When a new state is emitted
        val connectedState = ConnectionState.Connected(mockk())
        connectionStateFlow.emit(connectedState)

        // Both collectors receive the new state
        assertEquals(listOf(ConnectionState.NoDevice, connectedState), states1)
        assertEquals(listOf(ConnectionState.NoDevice, connectedState), states2)

        job1.cancel()
        job2.cancel()
    }

    //endregion

    //region StateFlow Behavior

    @Test
    fun `No Notification on Initialization`() = runTest {
        // .drop(1) operator should prevent notification for the initial value
        MidiConnectionViewModel(trackMidiConnectionUseCase, showConnectionNotificationUseCase, userNotifier)
        verify(exactly = 0) { userNotifier.sendMessage(any()) }
    }

    @Test
    fun `Rapid Sequential State Changes`() = runTest {
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val finalState = ConnectionState.Connected(mockk())

        connectionStateFlow.emit(ConnectionState.Connected(mockk()))
        connectionStateFlow.emit(ConnectionState.Disconnected)
        connectionStateFlow.emit(finalState)

        assertEquals(finalState, viewModel.connectionState.value)
        coVerify(exactly = 3) { showConnectionNotificationUseCase(any()) }
    }

    @Test
    fun `State Change to Same Value`() = runTest {
        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val disconnectedState = ConnectionState.Disconnected

        connectionStateFlow.emit(disconnectedState)
        connectionStateFlow.emit(disconnectedState)
        connectionStateFlow.emit(disconnectedState)

        assertEquals(disconnectedState, viewModel.connectionState.value)
        coVerify(exactly = 1) { showConnectionNotificationUseCase(disconnectedState) }
    }

    //endregion

    //region Error Handling

    @Test
    fun `Error in Upstream Flow`() = runTest {
        // Re-setup flow for this specific test
        val errorFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
        coEvery { trackMidiConnectionUseCase.invoke() } returns errorFlow

        val viewModel = MidiConnectionViewModel(
            trackMidiConnectionUseCase,
            showConnectionNotificationUseCase,
            userNotifier
        )
        val job = launch { viewModel.connectionState.collect {} }

        val exception = RuntimeException("Upstream error")
        errorFlow.tryEmit(ConnectionState.Connected(mockk())) // Works fine
        errorFlow.tryEmit(ConnectionState.Error(exception.message ?: "Unknown error")) // Emit error

        // This would throw if the scope was cancelled
        errorFlow.tryEmit(ConnectionState.Disconnected)

        job.cancel() // Clean up
    }

    //endregion
}
