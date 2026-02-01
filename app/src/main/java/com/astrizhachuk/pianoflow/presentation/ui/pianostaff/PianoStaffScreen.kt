package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel

/**
 * A composable screen that displays a piano staff and the name of the currently played chord.
 *
 * This screen is composed of two main parts:
 * 1. A `PianoStaff` composable that visually represents the musical notes.
 * 2. A `Text` composable that displays the name of the chord analyzed from the notes on the staff.
 *
 * It uses a custom `Layout` to position the `PianoStaff` on the left half of the screen
 * and the chord name on the right half, separated by a small spacer.
 * The state, including the notes to display (`notesJson`) and the resulting chord name (`chordName`),
 * is managed by the [PianoStaffViewModel].
 *
 * @param modifier The modifier to be applied to the layout. Defaults to [Modifier].
 * @param viewModel The [PianoStaffViewModel] instance used to manage the screen's state.
 *                  It is provided by Hilt via [hiltViewModel].
 */
@Composable
fun PianoStaffScreen(
    modifier: Modifier = Modifier,
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            PianoStaff(
                notesJson = uiState.notesJson,
                onChordAnalyzed = { viewModel.updateChordName(it) }
            )

            uiState.chordName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    ) { measurables, constraints ->
        val spacerWidth = 16.dp.toPx().toInt()

        val staffWidth = (constraints.maxWidth * 0.5f).toInt()
        val chordNameWidth = constraints.maxWidth - staffWidth - spacerWidth

        val staffPlaceable = measurables[0].measure(
            constraints.copy(
                minWidth = staffWidth,
                maxWidth = staffWidth,
                minHeight = constraints.maxHeight,
                maxHeight = constraints.maxHeight
            )
        )

        val chordNamePlaceable = measurables.getOrNull(1)?.measure(
            constraints.copy(
                minWidth = chordNameWidth,
                maxWidth = chordNameWidth
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            staffPlaceable.placeRelative(x = 0, y = 0)

            chordNamePlaceable?.let {
                val chordNameY = (constraints.maxHeight - it.height) / 2
                it.placeRelative(x = staffWidth + spacerWidth, y = chordNameY)
            }
        }
    }
}
