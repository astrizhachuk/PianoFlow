package com.astrizhachuk.pianoflow.data.datasource.analysis

import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A script engine for music-related tasks, powered by a hidden WebView.
 *
 * This class abstracts the complexities of interacting with a [WebView], providing a straightforward
 * interface for executing JavaScript code. It manages a queue of pending scripts, ensuring that
 * they are executed only after the WebView has finished loading its initial page. This design
 * allows for a synchronous-style API for callers while handling the asynchronous nature of
 * WebView page loads internally.
 *
 * Encapsulating the [WebView] within this data-layer class offers several advantages:
 * - It decouples the presentation layer from the Android-specific [WebView] implementation.
 * - It enables repositories or other data sources to execute JavaScript without relying on UI
 *   callbacks or lifecycle events.
 * - It promotes a clean separation of concerns, keeping business logic (e.g., data processing
 *   via JS) distinct from UI rendering.
 *
 * @param webView The [WebView] instance to be used for executing JavaScript. This class will
 *   configure its [WebViewClient].
 * @param pageUrl The URL of the local or remote page that needs to be loaded into the [WebView]
 *   before scripts can be executed.
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
                // Process any pending scripts
                processPendingScripts()
            }
        }
        // Load the specified HTML page
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
