package com.astrizhachuk.pianoflow.data.repository

import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Конкретная реализация интерфейса [MidiRepository].
 *
 * Этот класс служит единым источником истины для MIDI-данных, делегируя вызовы
 * к базовому [MidiDataSource]. Он абстрагирует реализацию источника данных
 * от доменного слоя, предоставляя чистый API для наблюдения за состоянием MIDI-соединения
 * и входящими событиями нот.
 *
 * @param midiDataSource Источник данных, отвечающий за обработку необработанной MIDI-связи.
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
