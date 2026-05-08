package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.astrizhachuk.pianoflow.data.datasource.analysis.MusicScriptEngine
import timber.log.Timber

/**
 * A Composable that displays a piano staff with musical notes.
 *
 * This component utilizes an embedded `WebView` to render a musical staff via the VexFlow
 * JavaScript library. It loads a local HTML file (`vexflow.html` from assets) and then
 * executes a JavaScript function (`drawGrandStaff`) to draw the specified notes.
 *
 * The staff automatically redraws whenever the notes, orientation, or component size change,
 * ensuring it adapts to different screen configurations. This composable is designed for
 * display purposes only and does not contain any music theory or note analysis logic.
 *
 * @param notesJson A JSON string representing the notes to be displayed. The string should
 *   contain `treble` and `bass` keys, each with an array of notes for the respective clef.
 * @param isPortrait A boolean indicating the orientation. `true` for portrait mode, `false` for
 *   landscape. This affects how the staff is rendered.
 * @param isDarkScheme A boolean indicating the active color scheme. `true` for dark scheme, `false`
 *   for light. This controls the rendering colors of the staff and notes.
 * @param needsScale A boolean indicating whether the staff should use a reduced scale. `true` on
 *   compact screens (phones) to fit the grand staff within a smaller landscape viewport.
 * @param modifier The modifier to be applied to the `PianoStaff` container.
 */
@Composable
fun PianoStaff(
    modifier: Modifier = Modifier,
    notesJson: String,
    isPortrait: Boolean,
    isDarkScheme: Boolean,
    needsScale: Boolean = false
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Piano Staff Preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val context = LocalContext.current
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            setBackgroundColor(0)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Timber.tag("WebView").d(consoleMessage.message())
                    return true
                }
            }
        }
    }

    val executor = remember {
        MusicScriptEngine(
            webView = webView,
            pageUrl = "file:///android_asset/vexflow.html"
        )
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.onSizeChanged { viewSize = it }
    )

    // Update display when notes, orientation, size, theme, or scale flag changes.
    LaunchedEffect(notesJson, isPortrait, viewSize, isDarkScheme, needsScale) {
        if (viewSize == IntSize.Zero) return@LaunchedEffect

        val drawScript = "drawGrandStaff(JSON.parse('$notesJson').treble, JSON.parse('$notesJson').bass, $isPortrait, $isDarkScheme, $needsScale);"
        executor.execute(drawScript) { /* No result needed */ }
    }
}
