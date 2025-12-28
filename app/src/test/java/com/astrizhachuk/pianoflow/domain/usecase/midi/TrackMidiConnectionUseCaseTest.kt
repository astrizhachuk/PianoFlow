package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit-тесты для TrackMidiConnectionUseCase.
 */
class TrackMidiConnectionUseCaseTest {
    
    private lateinit var midiRepository: MidiRepository
    private lateinit var useCase: TrackMidiConnectionUseCase
    
    @Before
    fun setUp() {
        midiRepository = mock()
        useCase = TrackMidiConnectionUseCase(midiRepository)
    }
    
    @Test
    fun `invoke should return flow of connection states`() = runTest {
        // Given
        val device = MidiDevice(id = 1, name = "Test Device", isInput = true)
        val expectedStates = listOf(
            ConnectionState.Disconnected,
            ConnectionState.Connecting,
            ConnectionState.Connected(device)
        )
        whenever(midiRepository.observeConnectionState())
            .thenReturn(flow {
                expectedStates.forEach { emit(it) }
            })
        
        // When
        val result = useCase().toList()
        
        // Then
        assertEquals(expectedStates, result)
    }
    
    @Test
    fun `initialize should connect to first device when disconnected`() = runTest {
        // Given
        val devices = listOf(
            MidiDevice(id = 1, name = "Device 1", isInput = true),
            MidiDevice(id = 2, name = "Device 2", isInput = true)
        )
        whenever(midiRepository.getCurrentConnectionState())
            .thenReturn(ConnectionState.Disconnected)
        whenever(midiRepository.getAvailableDevices())
            .thenReturn(devices)
        whenever(midiRepository.connectToDevice(1))
            .thenReturn(kotlin.Result.success(devices.first()))
        
        // When
        useCase.initialize()
        
        // Then
        verify(midiRepository).getCurrentConnectionState()
        verify(midiRepository).getAvailableDevices()
        verify(midiRepository).connectToDevice(1)
    }
    
    @Test
    fun `initialize should not connect when already connected`() = runTest {
        // Given
        val connectedDevice = MidiDevice(id = 1, name = "Device", isInput = true)
        whenever(midiRepository.getCurrentConnectionState())
            .thenReturn(ConnectionState.Connected(connectedDevice))
        
        // When
        useCase.initialize()
        
        // Then
        verify(midiRepository).getCurrentConnectionState()
        verify(midiRepository, org.mockito.kotlin.never()).getAvailableDevices()
        verify(midiRepository, org.mockito.kotlin.never()).connectToDevice(org.mockito.kotlin.any())
    }
    
    @Test
    fun `initialize should not connect when no devices available`() = runTest {
        // Given
        whenever(midiRepository.getCurrentConnectionState())
            .thenReturn(ConnectionState.Disconnected)
        whenever(midiRepository.getAvailableDevices())
            .thenReturn(emptyList())
        
        // When
        useCase.initialize()
        
        // Then
        verify(midiRepository).getCurrentConnectionState()
        verify(midiRepository).getAvailableDevices()
        verify(midiRepository, org.mockito.kotlin.never()).connectToDevice(org.mockito.kotlin.any())
    }
}

