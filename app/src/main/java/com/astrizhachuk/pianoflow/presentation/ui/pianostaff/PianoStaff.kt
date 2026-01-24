
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tracing.trace

/**
 * Composable-функция, которая отображает фортепианный нотный стан с музыкальными нотами с помощью WebView.
 *
 * Эта функция отрисовывает полный нотный стан (скрипичный и басовый ключи), загружая HTML-файл
 * из папки assets, который использует библиотеку VexFlow для рендеринга музыкальной нотации.
 * Ноты для отображения передаются в виде JSON-строк и рисуются на нотном стане
 * с помощью вызова JavaScript-функции внутри WebView.
 *
 * @param notesJson JSON-строка, представляющая ноты для обоих станов (скрипичного и басового).
 * @param modifier Модификатор, который будет применен к контейнеру WebView.
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
                    // Возвращаем загрузку основного, рабочего файла
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

