package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel

/**
 * A composable screen that displays a piano staff and the name of the analyzed chord.
 *
 * This screen adapts its layout based on the device's orientation.
 *
 * - In **landscape** orientation, it displays the [PianoStaff] on the left half and the
 *   chord name on the right half.
 * - In **portrait** orientation, it displays the chord name at the top and the [PianoStaff]
 *   below it.
 *
 * The state, including the notes to display (`notesJson`) and the resulting chord name (`chordName`),
 * is collected from the [PianoStaffViewModel] and updated via callbacks.
 *
 * @param modifier The modifier to be applied to the screen's root layout.
 * @param viewModel The [PianoStaffViewModel] instance for state management,
 *                  provided by Hilt.
 */
@Composable
fun PianoStaffScreen(
    modifier: Modifier = Modifier,
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val chordName: @Composable () -> Unit = {
        uiState.chordName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }

    val pianoStaff: @Composable (Modifier) -> Unit = { staffModifier ->
        PianoStaff(
            modifier = staffModifier,
            notesJson = uiState.notesJson,
            isPortrait = isPortrait,
            onChordAnalyzed = { viewModel.updateChordName(it) }
        )
    }

    if (isPortrait) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            chordName()
            pianoStaff(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pianoStaff(Modifier.fillMaxWidth(0.5f))
            chordName()
        }
    }
}
