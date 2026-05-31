package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiMessageParser
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Фейковая реализация MidiRepository для управления состоянием подключения в UI-тестах.
 * Начальное состояние - NoDevice, что соответствует первому запуску приложения.
 */
class FakeMidiRepository : MidiRepository {

    private val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
    private val notesFlow = MutableSharedFlow<Note>(extraBufferCapacity = 64)
    private val parser = MidiMessageParser()

    /**
     * Метод для симуляции различных состояний подключения из тестов.
     */
    fun emitState(state: ConnectionState) {
        connectionStateFlow.value = state
    }

    /**
     * Метод для симуляции MIDI-нот из тестов.
     */
    suspend fun emitNote(note: Note) {
        notesFlow.emit(note)
    }

    fun sendRawMidi(data: ByteArray) {
        parser.parse(data)?.let { notesFlow.tryEmit(it) }
    }

    override fun observeConnectionState(): Flow<ConnectionState> {
        return connectionStateFlow
    }

    override fun observeNotes(): Flow<Note> {
        return notesFlow
    }
}
