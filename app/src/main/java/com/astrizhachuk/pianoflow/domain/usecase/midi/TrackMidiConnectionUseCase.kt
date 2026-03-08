package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import javax.inject.Inject

/**
 * A use case that provides a data stream of the MIDI device connection state.
 *
 * This use case acts as an intermediary between the data layer, represented by [MidiRepository],
 * and the UI layer. It allows observers, such as a ViewModel, to react to changes
 * in the MIDI device's connection status in a clean and isolated manner.
 *
 * @property midiRepository The repository responsible for managing MIDI data and connection state.
 */
class TrackMidiConnectionUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    operator fun invoke() = midiRepository.observeConnectionState()
}

