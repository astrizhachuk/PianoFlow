
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
 * A composable that renders a musical staff.
 *
 * This component utilizes an [AndroidView] to embed a [WebView]. The `WebView`
 * loads a local HTML file (`vexflow.html`) which leverages the VexFlow.js library
 * to visualize musical notes. The notes to be rendered are passed into the `WebView`
 * as a JSON string.
 *
 * Communication with the JavaScript in the `WebView` is handled via `evaluateJavascript`.
 * The composable also tracks its own size to adjust the staff's orientation
 * (portrait or landscape) for optimal rendering.
 *
 * @param notesJson A JSON string containing the notes to be drawn. It is expected
 *                  to have a structure like `{ "treble": [...], "bass": [...] }`.
 * @param modifier The modifier to be applied to the underlying [AndroidView].
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
}
