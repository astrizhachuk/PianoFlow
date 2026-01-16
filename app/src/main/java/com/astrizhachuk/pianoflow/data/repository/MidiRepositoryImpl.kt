package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MidiRepositoryImpl @Inject constructor(
    private val midiDataSource: MidiDataSource
) : MidiRepository {
    override fun observeConnectionState(): Flow<ConnectionState> {
        return midiDataSource.connectionState
    }
}
