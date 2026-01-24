
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tracing.trace

/**
 * Composable-компонент, который отвечает за отрисовку музыкального стана.
 *
 * Этот компонент использует [AndroidView] для встраивания [WebView]. В `WebView` загружается
 * локальный HTML-файл (`vexflow.html`), который использует библиотеку VexFlow для визуализации
 * музыкальных нот. Ноты передаются в `WebView` в виде JSON-строки.
 *
 * @param notesJson JSON-строка, содержащая ноты для отрисовки. Она должна иметь
 *                  структуру `{ "treble": [...], "bass": [...] }`.
 * @param modifier Модификатор, который будет применен к `AndroidView`.
 */
@Composable
fun PianoStaff(
    notesJson: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            trace("PianoStaff:WebView:factory") {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    webChromeClient = WebChromeClient()
                    loadUrl("file:///android_asset/vexflow.html")
                }
            }
        },
        update = { webView ->
            trace("PianoStaff:WebView:update") {
                val script = """
                    try {
                        const data = JSON.parse('$notesJson');
                        drawGrandStaff(data.treble, data.bass);
                    } catch (e) {
                        console.error('Error executing script: ', e);
                    }
                """
                webView.evaluateJavascript(script, null)
            }
        },
        modifier = modifier
    )
}

