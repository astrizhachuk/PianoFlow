
package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MidiRepositoryImplTest {

    private val midiDataSource = mock<MidiDataSource>()

    @Test
    fun `observeConnectionState should proxy call to data source`() = runTest {
        // Arrange
        val expectedState = ConnectionState.NoDevice
        whenever(midiDataSource.connectionState).thenReturn(MutableStateFlow(expectedState))
        val repository = MidiRepositoryImpl(midiDataSource)

        // Act
        val actualState = repository.observeConnectionState().first()

        // Assert
        assertEquals(expectedState, actualState)
    }
}
