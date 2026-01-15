package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import javax.inject.Inject

/**
 * Use case, который предоставляет поток данных о состоянии подключения MIDI-устройства.
 *
 * Этот класс служит мостом между слоем данных ([MidiRepository]) и слоем представления (ViewModel),
 * предоставляя простой и изолированный способ для наблюдения за состоянием подключения.
 */
class TrackMidiConnectionUseCase @Inject constructor(
    private val midiRepository: MidiRepository
) {
    operator fun invoke() = midiRepository.observeConnectionState()
}

