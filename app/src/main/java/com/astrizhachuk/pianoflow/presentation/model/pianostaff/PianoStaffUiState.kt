package com.astrizhachuk.pianoflow.presentation.model.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note

/**
 * Представляет состояние UI для экрана с нотным станом.
 *
 * @param notes Список нот, которые должны быть отображены на нотном стане.
 */
data class PianoStaffUiState(
    val notes: List<Note> = emptyList()
)