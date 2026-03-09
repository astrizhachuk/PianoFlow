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
 * Observes incoming MIDI note messages and groups them into chords based on temporal proximity.
 *
 * This use case subscribes to the stream of individual MIDI [Note] events from the
 * [MidiRepository]. It collects notes that arrive within a short time window ([CHORD_WINDOW_MS])
 * and emits them as a single list, representing a chord.
 *
 * The logic is designed to handle both single notes and chords played with slight timing
 * variations (arpeggiation). Each time a note arrives, a timer is reset. If more notes arrive
 * before the timer expires, they are added to the current chord. When the timer finally
 * expires, the collected list of notes is emitted. A new note arriving after a previous
 * emission will start a new chord.
 *
 * Each emission from the resulting Flow represents a complete musical event (a single note or a chord)
 * that should be displayed, replacing any previously displayed notes.
 *
 * @see MidiRepository.observeNotes
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
