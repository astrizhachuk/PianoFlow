
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.annotation.SuppressLint
import android.view.ViewGroup
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
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PianoStaff(
    notesJson: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var isPageLoaded by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            webChromeClient = WebChromeClient()
        }
    }

    LaunchedEffect(notesJson, viewSize, isPageLoaded) {
        if (!isPageLoaded || viewSize == IntSize.Zero) return@LaunchedEffect

        val orientation = if (viewSize.width > viewSize.height) "landscape" else "portrait"
        val script = """
            try {
                const svg = document.querySelector('svg');
                if (svg && svg.parentElement) {
                    svg.parentElement.innerHTML = '';
                }
                const data = JSON.parse('$notesJson');
                drawGrandStaff(data.treble, data.bass, '$orientation');
            } catch (e) {
                console.error('Error executing script: ', e);
            }
        """
        webView.evaluateJavascript(script, null)
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
}
