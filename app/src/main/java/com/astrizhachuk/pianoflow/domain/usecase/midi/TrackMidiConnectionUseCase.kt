package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import javax.inject.Inject

/**
 * Use case that provides a data stream of the MIDI device connection state.
 *
 * This class serves as a bridge between the data layer ([MidiRepository]) and the presentation layer (ViewModel),
 * providing a simple and isolated way to observe the connection state.
 */
class TrackMidiConnectionUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    operator fun invoke() = midiRepository.observeConnectionState()
}

