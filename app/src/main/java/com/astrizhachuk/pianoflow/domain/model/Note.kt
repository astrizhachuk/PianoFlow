package com.astrizhachuk.pianoflow.domain.model

/**
 * Доменная модель, представляющая одну MIDI-ноту.
 *
 * @param pitch MIDI-номер ноты (от 0 до 127).
 */
data class Note(
    val pitch: Int
)
