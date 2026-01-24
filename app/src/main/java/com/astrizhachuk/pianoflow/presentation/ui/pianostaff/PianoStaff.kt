
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
    val orientation = when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        else -> "portrait"
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            webChromeClient = WebChromeClient()
            loadUrl("file:///android_asset/vexflow.html")
        }
    }

    // Этот эффект будет перезапускаться каждый раз, когда изменятся notesJson или orientation.
    LaunchedEffect(notesJson, orientation) {
        val script = """
            try {
                const data = JSON.parse('$notesJson');
                drawGrandStaff(data.treble, data.bass, '$orientation');
            } catch (e) {
                console.error('Error executing script: ', e);
            }
        """
        // Выполняем скрипт и в колбэке принудительно перерисовываем WebView
        webView.evaluateJavascript(script) {
            webView.invalidate()
        }
    }

    AndroidView({ webView }, modifier = modifier)
}
