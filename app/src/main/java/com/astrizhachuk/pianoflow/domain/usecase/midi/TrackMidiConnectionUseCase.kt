package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import javax.inject.Inject

class TrackMidiConnectionUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    operator fun invoke() = midiRepository.observeConnectionState()
}

