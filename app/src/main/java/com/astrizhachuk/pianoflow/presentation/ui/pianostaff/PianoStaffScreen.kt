
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel

/**
 * Composable-экран, который отображает музыкальный нотный стан.
 *
 * Этот экран является точкой входа для отображения [PianoStaff]. Он подключается к [PianoStaffViewModel]
 * для получения актуального состояния UI (`notesJson`) и передает его в `PianoStaff` для отрисовки.
 *
 * @param modifier Модификатор, применяемый к корневому контейнеру Box.
 * @param viewModel ViewModel, предоставляемая через Hilt, которая управляет состоянием экрана.
 */
@Composable
fun PianoStaffScreen(
    modifier: Modifier = Modifier,
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PianoStaff(notesJson = uiState.notesJson)
    }
}
