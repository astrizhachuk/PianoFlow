package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.presentation.ui.theme.AppTheme
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

    PianoStaffContent(
        chordName = uiState.chordName,
        notesJson = uiState.notesJson,
        modifier = modifier
    )
}

/**
 * The stateless content of the Piano Staff screen that renders the musical staff and chord name.
 *
 * This composable handles the responsive layout logic:
 * - In **portrait** mode, it stacks the chord name vertically above the [PianoStaff].
 * - In **landscape** mode, it places the [PianoStaff] on the left and the chord name on the right.
 *
 * @param chordName The name of the analyzed chord to display, or null if no chord is identified.
 * @param notesJson A JSON string representation of the musical notes to be rendered on the staff.
 * @param modifier The [Modifier] to be applied to the layout container.
 */
@Composable
fun PianoStaffContent(
    chordName: String?,
    notesJson: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isDarkTheme = isSystemInDarkTheme()

    val chordNameText: @Composable () -> Unit = {
        chordName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }

    val pianoStaff: @Composable (Modifier) -> Unit = { staffModifier ->
        PianoStaff(
            modifier = staffModifier,
            notesJson = notesJson,
            isPortrait = isPortrait,
            isDarkTheme = isDarkTheme
        )
    }

    if (isPortrait) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            chordNameText()
            pianoStaff(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pianoStaff(Modifier.fillMaxWidth(0.5f))
            chordNameText()
        }
    }
}

@Preview
@Composable
fun PianoStaffContentPortraitPreview() {
    AppTheme(darkTheme = false) {
        PianoStaffContent(
            chordName = "C Major",
            notesJson = "{\"treble\":[{\"keys\":[\"c/4\", \"e/4\", \"g/4\"], \"duration\":\"w\"}], \"bass\":[{\"keys\":[\"c/3\"], \"duration\":\"w\"}]}",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun PianoStaffContentLandscapePreview() {
    AppTheme(darkTheme = true) {
        PianoStaffContent(
            chordName = "C Major",
            notesJson = "{\"treble\":[{\"keys\":[\"c/4\", \"e/4\", \"g/4\"], \"duration\":\"w\"}], \"bass\":[{\"keys\":[\"c/3\"], \"duration\":\"w\"}]}",
            modifier = Modifier.fillMaxSize()
        )
    }
}