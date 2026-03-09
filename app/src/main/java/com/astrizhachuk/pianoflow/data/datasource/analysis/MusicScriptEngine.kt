package com.astrizhachuk.pianoflow.data.datasource.analysis

import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Executes JavaScript for music analysis tasks using a hidden [WebView].
 *
 * This engine provides a simplified interface for running JavaScript code by abstracting away the
 * asynchronous nature of [WebView]. It maintains a queue for scripts, executing them only after
 * the designated web page has fully loaded. This allows callers to use a synchronous-style API
 * without managing the complexities of page load events.
 *
 * By encapsulating the [WebView] within this data-layer class, the application benefits from:
 * - Decoupling of the presentation layer from the Android-specific `WebView` implementation.
 * - Enabling data-layer components (e.g., repositories) to execute JavaScript without
 *   depending on UI lifecycle or callbacks.
 * - A clear separation of concerns, isolating JavaScript-based business logic from the UI.
 *
 * @param webView The [WebView] instance to be used for script execution. This class takes
 *   ownership of configuring its [WebViewClient].
 * @param pageUrl The URL of the page to load into the [WebView]. Scripts will not be
 *   executed until this page has finished loading.
 */
class MusicScriptEngine(
    private val webView: WebView,
    pageUrl: String
) {
    private var isInitialized = false
    private val pendingScripts = ConcurrentLinkedQueue<Pair<String, (String?) -> Unit>>()

    init {
        Timber.d("Initializing MusicScriptEngine for URL: %s", pageUrl)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Timber.d("WebView initialization page loaded: %s", url)
                isInitialized = true
                processPendingScripts()
            }
        }
        webView.loadUrl(pageUrl)
    }

    /**
     * Executes JavaScript code and returns the result via callback.
     *
     * If WebView hasn't finished loading, the script will be queued and executed
     * as soon as the page finishes loading.
     *
     * @param script The JavaScript code to execute
     * @param onResult Callback with the result string
     */
    fun execute(
        script: String,
        onResult: (String?) -> Unit
    ) {
        Timber.d("Execute called: %s, isInitialized: %s", script, isInitialized)

        if (!isInitialized) {
            // Queue the script for later execution
            Timber.d("WebView not ready, queuing script")
            pendingScripts.offer(Pair(script, onResult))
            return
        }

        executeScript(script, onResult)
    }

    /**
     * Processes all pending scripts that were queued before initialization.
     */
    private fun processPendingScripts() {
        Timber.d("Processing %d pending scripts", pendingScripts.size)
        while (true) {
            val scriptPair = pendingScripts.poll() ?: break
            val script = scriptPair.first
            val callback = scriptPair.second
            executeScript(script, callback)
        }
    }

    /**
     * Executes a single script on the WebView.
     */
    private fun executeScript(script: String, onResult: (String?) -> Unit) {
        try {
            Timber.d("Executing JS: %s", script)
            webView.evaluateJavascript(script) { jsResult ->
                Timber.d("JS result: %s", jsResult)
                onResult(jsResult)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute JavaScript")
            onResult(null)
        }
    }
}
