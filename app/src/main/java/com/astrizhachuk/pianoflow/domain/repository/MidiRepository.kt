package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing MIDI data.
 *
 * This interface defines the contract for interacting with MIDI devices.
 * It abstracts the data source and provides a clean API for observing the state
 * of the MIDI connection and incoming note events.
 */
interface MidiRepository {

    /**
     * Observes the current connection state of the MIDI device.
     *
     * This function returns a [Flow] that emits [ConnectionState] updates whenever
     * the MIDI device's connection status changes (e.g., connecting, connected, disconnected).
     *
     * @return A [Flow] of [ConnectionState] representing the MIDI connection status.
     */
    fun observeConnectionState(): Flow<ConnectionState>


    /**
     * Observes incoming MIDI note events.
     *
     * This function returns a [Flow] that emits a [Note] object each time
     * a note-on or note-off event is received from a connected MIDI device.
     * The flow will only be active when there is an active MIDI connection.
     *
     * @return A [Flow] of [Note] objects representing incoming MIDI note events.
     */
    fun observeNotes(): Flow<Note>
}
