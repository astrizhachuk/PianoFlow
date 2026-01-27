
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
 * A Composable screen that displays a musical staff.
 *
 * This screen serves as the entry point for displaying the [PianoStaff]. It connects to the
 * [PianoStaffViewModel] to observe the current UI state (`notesJson`) and passes it
 * down to the [PianoStaff] composable for rendering.
 *
 * @param modifier The modifier to be applied to the root Box container.
 * @param viewModel The ViewModel, provided by Hilt, which manages the screen's state.
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
