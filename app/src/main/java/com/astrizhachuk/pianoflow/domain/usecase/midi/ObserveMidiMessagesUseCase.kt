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
 * Use case for observing incoming MIDI messages.
 *
 * It receives a stream of individual notes from the [MidiRepository] and groups them into lists (chords).
 * The grouping is based on temporal proximity: notes that arrive within a short
 * period of time are considered part of the same chord.
 *
 * Each new emission from this use case represents a complete set of notes (a chord)
 * that should be displayed on the screen, replacing the previous one.
 */
class ObserveMidiMessagesUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    /**
     * @return A [Flow] that emits lists of notes ([List]<[Note]>).
     */
    operator fun invoke(): Flow<List<Note>> = channelFlow {
        trace("ObserveMidiMessagesUseCase.invoke") {
            Timber.i("invoke: Starting to observe MIDI messages.")
            val notesBuffer = mutableListOf<Note>()
            var flushJob: Job? = null

            midiRepository.observeNotes().collect { note ->
                Timber.d("collect: Received note: $note")
                // If the previous job is null or has already completed,
                // it means a new musical event has started.
                if (flushJob?.isCompleted ?: true) {
                    Timber.d("collect: New musical event started, clearing buffer.")
                    notesBuffer.clear()
                }

                // Cancel any pending send to extend the time window for the chord.
                flushJob?.cancel()
                Timber.d("collect: Canceled previous flush job.")

                notesBuffer.add(note)

                // Start a new delayed task to send the grouped notes.
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
         * Time window in milliseconds for grouping notes into a chord.
         * If notes arrive within this time, they are considered part of the same chord.
         * A value of 50 ms is a compromise between responsiveness and accuracy
         * in detecting chords that are not played perfectly simultaneously (arpeggiato).
         */
        const val CHORD_WINDOW_MS = 50L
    }
}
