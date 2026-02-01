package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.annotation.SuppressLint
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
import com.google.gson.Gson
import timber.log.Timber
import java.util.Locale

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
 * @param onChordAnalyzed A callback function that is invoked with the name of the detected chord
 *                        (e.g., "Cmaj7") or `null` if no chord could be identified or if no notes
 *                        were provided.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PianoStaff(
    modifier: Modifier = Modifier,
    notesJson: String,
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

    // Анализ аккордов теперь отделен и запускается только при изменении нот.
    LaunchedEffect(notesJson, isPageLoaded) {
        if (isPageLoaded) {
            webView.handleChordAnalysis(notesJson, onChordAnalyzed)
        }
    }

    // Вычисляем параметры для отображения, которые зависят только от размера.
    // Это позволяет не пересчитывать их при каждом изменении нот.
    val visualConfig = remember(viewSize) {
        if (viewSize == IntSize.Zero) {
            null
        } else {
            object {
                val orientation = if (viewSize.width > viewSize.height) "landscape" else "portrait"
                val width = (viewSize.width / context.resources.displayMetrics.density).toInt()
                val height = (viewSize.height / context.resources.displayMetrics.density).toInt()
            }
        }
    }

    // Обновляем отображение при изменении нот или конфигурации отображения.
    LaunchedEffect(notesJson, visualConfig, isPageLoaded) {
        if (!isPageLoaded || visualConfig == null) return@LaunchedEffect

        val drawScript = "drawGrandStaff(JSON.parse('$notesJson').treble, JSON.parse('$notesJson').bass, '${visualConfig.orientation}', ${visualConfig.width}, ${visualConfig.height});"
        webView.evaluateJavascript(drawScript, null)
    }
}

private fun WebView.handleChordAnalysis(notesJson: String, onChordAnalyzed: (String?) -> Unit) {
    Timber.tag("ChordAnalysis").d("Input notesJson: %s", notesJson)

    val notesForAnalysis = try {
        val notesMap = Gson().fromJson(notesJson, Map::class.java) as Map<String, List<Map<String, Any>>>
        val allNotes = (notesMap["treble"].orEmpty() + notesMap["bass"].orEmpty())
        allNotes.map { it["keys"] as List<String> }.flatten().distinct()
            .map { noteName ->
                val parts = noteName.split("/")
                if (parts.size == 2) {
                    parts[0].replaceFirstChar { it.titlecase(Locale.ROOT) } + parts[1]
                } else {
                    noteName
                }
            }
            // --- ФИНАЛЬНОЕ ИСПРАВЛЕНИЕ: Сортируем ноты перед отправкой в Tonal.js ---
            .sorted()
    } catch (e: Exception) {
        Timber.tag("ChordAnalysis").e(e, "Failed to parse notesJson")
        emptyList<String>()
    }
    Timber.tag("ChordAnalysis").d("Parsed and SORTED notes for Tonal.js: %s", notesForAnalysis)

    if (notesForAnalysis.isNotEmpty()) {
        val notesJsArray = notesForAnalysis.joinToString(prefix = "[", postfix = "]") { "'$it'" }
        val analysisScript = "analyzeNotesWithTonal($notesJsArray)"
        Timber.tag("ChordAnalysis").d("Executing JS: %s", analysisScript)

        evaluateJavascript(analysisScript) { result ->
            Timber.tag("ChordAnalysis").d("JS raw result: %s", result)
            val cleanedResult = result?.removeSurrounding("\"")

            if (cleanedResult.isNullOrEmpty() || cleanedResult.equals("null", ignoreCase = true)) {
                onChordAnalyzed(null)
            } else {
                onChordAnalyzed(cleanedResult)
            }
        }
    } else {
        onChordAnalyzed(null)
    }
}
