package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel

/**
 * Компонента-экран, которая отображает нотный стан (скрипичный и басовый ключи) с музыкальными нотами.
 *
 * Этот экран отслеживает состояние UI из [PianoStaffViewModel], чтобы получить ноты для отображения.
 * Он центрирует компоненту [PianoStaff], которая отвечает за отрисовку самого нотного стана и нот.
 *
 * @param modifier Модификатор, применяемый к корневому контейнеру.
 * @param viewModel Экземпляр [PianoStaffViewModel], обычно предоставляемый через Hilt, который
 * поставляет состояние UI, включая JSON-представления нот для скрипичного и басового ключей.
 */
@Composable
fun PianoStaffScreen(
    modifier: Modifier = Modifier,
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PianoStaff(
            trebleNotesJson = uiState.trebleNotesJson,
            bassNotesJson = uiState.bassNotesJson
        )
    }
}
