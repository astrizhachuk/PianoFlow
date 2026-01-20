package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use case для наблюдения за входящими MIDI-сообщениями.
 *
 * Он получает поток отдельных нот от [MidiRepository] и группирует их в списки (аккорды).
 * Группировка происходит на основе временной близости: ноты, пришедшие в течение
 * короткого промежутка времени, считаются частью одного аккорда.
 *
 * Каждая новая эмиссия из этого use case представляет собой полный набор нот (аккорд),
 * который должен быть отображен на экране, заменяя предыдущий.
 */
class ObserveMidiMessagesUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    /**
     * @return [Flow], который порождает списки нот ([List]<[Note]>).
     */
    operator fun invoke(): Flow<List<Note>> = channelFlow {
        val notesBuffer = mutableListOf<Note>()
        var flushJob: Job? = null

        midiRepository.observeNotes().collect { note ->
            // Если предыдущая задача по отправке была завершена (или ее не было),
            // значит, это начало нового музыкального события (новой ноты или аккорда).
            // В этом случае мы очищаем буфер перед добавлением новой ноты.
            if (flushJob == null || flushJob?.isCompleted == true) {
                notesBuffer.clear()
            }

            // Отменяем любую ожидающую отправку, чтобы расширить временное окно для аккорда.
            flushJob?.cancel()

            notesBuffer.add(note)

            // Запускаем новую отложенную задачу по отправке сгруппированных нот.
            flushJob = launch {
                delay(CHORD_WINDOW_MS)
                send(notesBuffer.toList())
            }
        }
    }

    private companion object {
        /**
         * Временное окно в миллисекундах для группировки нот в аккорд.
         * Если ноты приходят в течение этого времени, они считаются частью одного аккорда.
         * Значение 50 мс является компромиссом между отзывчивостью и точностью
         * определения аккордов, сыгранных не идеально одновременно (арпеджиато).
         */
        const val CHORD_WINDOW_MS = 50L
    }
}
