package com.astrizhachuk.pianoflow.presentation.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

private const val TABLET_MIN_WIDTH_DP = 600

/**
 * Holds information about the current window configuration.
 *
 * @property isLandscape True if the device is in landscape orientation.
 * @property isPhone True if the device is considered a phone (smallest screen width < 600dp).
 */
data class WindowInfo(
    val isLandscape: Boolean,
    val isPhone: Boolean
)

/**
 * Remembers and returns [WindowInfo] based on the current configuration.
 */
@Composable
fun rememberWindowInfo(): WindowInfo {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        WindowInfo(
            isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            isPhone = configuration.smallestScreenWidthDp < TABLET_MIN_WIDTH_DP
        )
    }
}
