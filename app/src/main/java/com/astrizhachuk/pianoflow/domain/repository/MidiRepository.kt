package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления MIDI-данными.
 *
 * Этот интерфейс определяет контракт для взаимодействия с MIDI-устройствами.
 * Он абстрагирует источник данных и предоставляет чистый API для наблюдения за состоянием
 * MIDI-соединения и входящими событиями нот.
 */
interface MidiRepository {
    /**
     * Наблюдает за текущим состоянием подключения MIDI-устройства.
     *
     * Эта функция возвращает [Flow], который выдает обновления [ConnectionState] всякий раз,
     * когда изменяется статус подключения MIDI-устройства (например, подключение, подключено, отключено).
     *
     * @return [Flow] из [ConnectionState], представляющий статус MIDI-соединения.
     */
    fun observeConnectionState(): Flow<ConnectionState>

    /**
     * Наблюдает за входящими событиями MIDI-нот.
     *
     * @return [Flow], который выдает объект [Note] каждый раз, когда от подключенного
     * MIDI-устройства получается событие включения/выключения ноты.
     */
    fun observeNotes(): Flow<Note>
}
