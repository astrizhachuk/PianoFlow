package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MidiRepositoryImplTest {

    private val midiDataSource: MidiDataSource = mockk()

    @Test
    fun `observeConnectionState should proxy call to data source`() = runTest {
        // Arrange
        val expectedState = ConnectionState.NoDevice
        every { midiDataSource.connectionState } returns MutableStateFlow(expectedState)
        val repository = MidiRepositoryImpl(midiDataSource)

        // Act
        val actualState = repository.observeConnectionState().first()

        // Assert
        assertEquals(expectedState, actualState)
    }
}
