package com.astrizhachuk.pianoflow.presentation.model.pianostaff

/**
 * Представляет состояние UI для экрана нотного стана.
 *
 * @param notesJson JSON-строка, содержащая ноты для обоих станов (скрипичного и басового).
 */
data class PianoStaffUiState(
    val notesJson: String = "{\"treble\":[], \"bass\":[]}"
)
