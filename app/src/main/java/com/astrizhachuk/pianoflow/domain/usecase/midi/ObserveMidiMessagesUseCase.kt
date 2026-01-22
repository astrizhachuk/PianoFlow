package com.astrizhachuk.pianoflow.domain.usecase.midi

import androidx.tracing.trace
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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
        trace("ObserveMidiMessagesUseCase.invoke") {
            Timber.i("invoke: Starting to observe MIDI messages.")
            val notesBuffer = mutableListOf<Note>()
            var flushJob: Job? = null

            midiRepository.observeNotes().collect { note ->
                Timber.d("collect: Received note: $note")
                // Если предыдущая задача отсутствует (null) или уже завершена,
                // это означает начало нового музыкального события.
                if (flushJob?.isCompleted ?: true) {
                    Timber.d("collect: New musical event started, clearing buffer.")
                    notesBuffer.clear()
                }

                // Отменяем любую ожидающую отправку, чтобы расширить временное окно для аккорда.
                flushJob?.cancel()
                Timber.d("collect: Canceled previous flush job.")

                notesBuffer.add(note)

                // Запускаем новую отложенную задачу по отправке сгруппированных нот.
                flushJob = launch {
                    delay(CHORD_WINDOW_MS)
                    Timber.d("flushJob: Timer elapsed. Sending ${notesBuffer.size} notes.")
                    send(notesBuffer.toList())
                }
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
