package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable-функция, которая отображает фортепианный нотный стан с музыкальными нотами с помощью WebView.
 *
 * Эта функция отрисовывает полный нотный стан (скрипичный и басовый ключи), загружая HTML-файл
 * из папки assets, который использует библиотеку VexFlow для рендеринга музыкальной нотации.
 * Ноты для отображения передаются в виде JSON-строк и рисуются на нотном стане
 * с помощью вызова JavaScript-функции внутри WebView.
 *
 * @param trebleNotesJson JSON-строка, представляющая ноты для отрисовки на скрипичном ключе.
 *                        Формат должен быть совместим с библиотекой VexFlow.
 * @param bassNotesJson JSON-строка, представляющая ноты для отрисовки на басовом ключе.
 *                      Формат должен быть совместим с библиотекой VexFlow.
 * @param modifier Модификатор, который будет применен к контейнеру WebView.
 */
@Composable
fun PianoStaff(
    trebleNotesJson: String,
    bassNotesJson: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webChromeClient = WebChromeClient()
                loadUrl("file:///android_asset/vexflow.html")
            }
        },
        update = { webView ->
            webView.evaluateJavascript("drawNotes('$trebleNotesJson', '$bassNotesJson')", null)
        },
        modifier = modifier
    )
}
