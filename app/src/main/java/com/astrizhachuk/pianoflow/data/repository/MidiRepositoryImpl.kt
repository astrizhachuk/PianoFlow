package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Concrete implementation of the [MidiRepository] interface.
 *
 * This class serves as the single source of truth for MIDI data, delegating calls
 * to the underlying [MidiDataSource]. It abstracts the data source implementation
 * from the domain layer, providing a clean API for observing the state of the MIDI connection
 * and incoming note events.
 *
 * @param midiDataSource The data source responsible for handling raw MIDI communication.
 */
class MidiRepositoryImpl @Inject constructor(
    private val midiDataSource: MidiDataSource
) : MidiRepository {
    override fun observeConnectionState(): Flow<ConnectionState> {
        return midiDataSource.connectionState
    }

    override fun observeNotes(): Flow<Note> {
        return midiDataSource.notes
    }
}
