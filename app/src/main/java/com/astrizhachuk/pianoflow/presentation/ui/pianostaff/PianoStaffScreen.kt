package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PianoStaff(
            notes = uiState.notes,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}