package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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
 *   chord card on the right half.
 * - In **portrait** orientation, it displays the [PianoStaff] at the top and the chord
 *   card below it.
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
 * The stateless content of the Piano Staff screen that renders the musical staff and chord card.
 *
 * This composable handles the responsive layout logic:
 * - In **portrait** mode, the [PianoStaff] occupies the upper portion and the [ChordCard]
 *   is anchored at the bottom.
 * - In **landscape** mode (tablet-optimized), the [PianoStaff] takes 2/3 of the width
 *   on the left and the [ChordCard] takes the remaining 1/3 on the right.
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
    val isDarkScheme = isSystemInDarkTheme()
    val horizontalPadding = dimensionResource(R.dimen.padding_medium)
    val verticalPadding = dimensionResource(R.dimen.padding_medium)

    if (isPortrait) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PianoStaff(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                notesJson = notesJson,
                isPortrait = true,
                isDarkScheme = isDarkScheme
            )
            ChordCard(
                chordName = chordName,
                fillHeight = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = verticalPadding)
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PianoStaff(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                notesJson = notesJson,
                isPortrait = false,
                isDarkScheme = isDarkScheme
            )
            ChordCard(
                chordName = chordName,
                fillHeight = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = verticalPadding)
            )
        }
    }
}

@Composable
private fun ChordCard(
    chordName: String?,
    fillHeight: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_card_default)
        )
    ) {
        val contentModifier = if (fillHeight) {
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        } else {
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium))
        }

        Box(
            modifier = contentModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.chord_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_chord_label)))
                Text(
                    text = chordName ?: stringResource(R.string.chord_placeholder),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

@Preview
@Composable
fun PianoStaffContentPortraitNoChordPreview() {
    AppTheme(darkTheme = false) {
        PianoStaffContent(
            chordName = null,
            notesJson = "{\"treble\":[], \"bass\":[]}",
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

@Preview(device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun PianoStaffContentLandscapeDarkPreview() {
    AppTheme(darkTheme = true) {
        PianoStaffContent(
            chordName = "Dm7♭5",
            notesJson = "{\"treble\":[{\"keys\":[\"d/4\", \"f/4\", \"ab/4\", \"c/5\"], \"duration\":\"w\"}], \"bass\":[]}",
            modifier = Modifier.fillMaxSize()
        )
    }
}
