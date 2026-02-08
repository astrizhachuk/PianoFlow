package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.JsonSyntaxException
import timber.log.Timber

/**
 * A Composable that displays a piano staff with notes using a WebView and the VexFlow library.
 * It also analyzes the displayed notes to identify the chord they form using the Tonal.js library.
 *
 * This function renders an HTML file (`vexflow.html`) from the assets, which contains the necessary
 * VexFlow and Tonal.js setup. It then uses JavaScript evaluation to draw the notes on the staff
 * and to perform chord analysis.
 *
 * The notes are passed in as a JSON string. The function parses this JSON, sends the notes
 * to the WebView to be drawn, and simultaneously sends them for analysis. The result of the
 * chord analysis is reported back via the `onChordAnalyzed` callback.
 *
 * The view is dynamically resized and redrawn based on the available space and orientation changes.
 *
 * @param notesJson A JSON string representing the notes to be displayed. The JSON should have
 *                  `"treble"` and `"bass"` keys, each with a list of note objects.
 *                  Example: `{"treble":[{"keys":["c/4"],"duration":"q"}],"bass":[]}`.
 * @param modifier The modifier to be applied to the WebView container.
 * @param isPortrait A boolean flag indicating the orientation of the staff.
 * @param onChordAnalyzed A callback function that is invoked with the name of the detected chord
 *                        (e.g., "Cmaj7") or `null` if no chord could be identified or if no notes
 *                        were provided.
 */
@Composable
fun PianoStaff(
    modifier: Modifier = Modifier,
    notesJson: String,
    isPortrait: Boolean,
    onChordAnalyzed: (String?) -> Unit
) {
    val context = LocalContext.current
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var isPageLoaded by remember { mutableStateOf(false) }

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

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageLoaded = true
                    }
                }
                loadUrl("file:///android_asset/vexflow.html")
            }
        },
        modifier = modifier.onSizeChanged { viewSize = it }
    )

    LaunchedEffect(notesJson, isPageLoaded) {
        if (isPageLoaded) {
            try {
                Timber.tag("ChordAnalysis").d("Parsing notes: %s", notesJson)
                val notes = parseNotesForAnalysis(notesJson)
                webView.evaluateChordAnalysis(notes, onChordAnalyzed)
            } catch (e: JsonSyntaxException) {
                Timber.tag("ChordAnalysis").e(e, "Failed to parse notesJson for chord analysis")
            }
        }
    }

    // Обновляем отображение при изменении нот, ориентации или размера.
    LaunchedEffect(notesJson, isPortrait, isPageLoaded, viewSize) {
        if (!isPageLoaded || viewSize == IntSize.Zero) return@LaunchedEffect

        // Теперь размеры не передаются, JS их определяет сам, а ориентация передается как boolean
        val drawScript = "drawGrandStaff(JSON.parse('$notesJson').treble, JSON.parse('$notesJson').bass, $isPortrait);"
        webView.evaluateJavascript(drawScript, null)
    }
}

private fun WebView.evaluateChordAnalysis(notes: List<String>, onChordAnalyzed: (String?) -> Unit) {
    if (notes.isEmpty()) {
        onChordAnalyzed(null)
        return
    }

    when (notes.size) {
        1 -> {
            val note = notes[0]
            val script = "simplifyNote('$note')"
            Timber.tag("ChordAnalysis").d("Executing JS: %s", script)
            evaluateJavascript(script) { result ->
                val finalResult = result?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "null" }
                Timber.tag("ChordAnalysis").d("JS raw result: %s, final: %s", result, finalResult)
                onChordAnalyzed(finalResult)
            }
        }
        else -> {
            val notesJsArray = notes.joinToString(prefix = "['", separator = "','", postfix = "']")
            val script = "detectChord($notesJsArray)"
            Timber.tag("ChordAnalysis").d("Executing JS: %s", script)
            evaluateJavascript(script) { chordResult ->
                val cleanedChordResult = chordResult?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "null" }

                if (cleanedChordResult != null) {
                    // Tonal.js может вернуть "CM" для до-мажора, исправим это
                    val finalResult = if (cleanedChordResult == "CM") "C" else cleanedChordResult
                    Timber.tag("ChordAnalysis").d("Chord detected: %s, final: %s", cleanedChordResult, finalResult)
                    onChordAnalyzed(finalResult)
                } else {
                    Timber.tag("ChordAnalysis").d("Chord not identified for notes: %s", notes)
                    onChordAnalyzed("Аккорд не определен")
                }
            }
        }
    }
}
