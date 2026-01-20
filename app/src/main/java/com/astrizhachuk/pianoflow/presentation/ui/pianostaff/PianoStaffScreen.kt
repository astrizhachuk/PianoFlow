package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.domain.model.Note
import com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState
import com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel

/**
 * Экран, отображающий нотный стан и сыгранные ноты.
 *
 * @param viewModel ViewModel, предоставляющая состояние экрана.
 */
@Composable
fun PianoStaffScreen(
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PianoStaffContent(uiState = uiState)
}

/**
 * Основной контент для экрана с нотным станом.
 *
 * @param uiState Текущее состояние UI.
 */
@Composable
private fun PianoStaffContent(
    uiState: PianoStaffUiState
) {
    Box(
        modifier = Modifier.Companion.fillMaxSize(),
        contentAlignment = Alignment.Companion.Center
    ) {
        if (uiState.notes.isEmpty()) {
            Text(text = "Начните играть на MIDI-клавиатуре...")
        } else {
            // Простое отображение MIDI-номеров нот для прототипа.
            // В будущем здесь будет полноценный компонент нотного стана.
            val notesText = uiState.notes.joinToString(separator = ", ") { it.pitch.toString() }
            Text(text = "Сыграны ноты: $notesText")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PianoStaffScreenPreview_Empty() {
    MaterialTheme {
        PianoStaffContent(uiState = PianoStaffUiState(notes = emptyList()))
    }
}

@Preview(showBackground = true)
@Composable
private fun PianoStaffScreenPreview_WithNotes() {
    MaterialTheme {
        PianoStaffContent(uiState = PianoStaffUiState(notes = listOf(Note(60), Note(64), Note(67))))
    }
}