package com.astrizhachuk.pianoflow.data.datasource.analysis

import android.os.Build
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, Build.VERSION_CODES.TIRAMISU])
class MusicScriptEngineTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var webView: WebView

    private lateinit var webViewClient: WebViewClient

    private lateinit var musicScriptEngine: MusicScriptEngine

    private val pageUrl = "file:///android_asset/vexflow.html"

    @Before
    fun setUp() {
        val webViewClientSlot: CapturingSlot<WebViewClient> = slot()
        every { webView.webViewClient = capture(webViewClientSlot) } just runs
        musicScriptEngine = MusicScriptEngine(webView, pageUrl)
        webViewClient = webViewClientSlot.captured
    }

    @Test
    fun `when initialized then loads correct url`() {
        verify { webView.loadUrl(pageUrl) }
    }

    @Test
    fun `given webview is initialized when execute is called then script is executed`() {
        // given
        val script = "const VF = Vex.Flow; new VF.Note({keys: [\"c/4\"], duration: \"q\" });"
        val expectedResult = "{\"duration\":\"q\",\"keys\":[\"c/4\"]}"
        var actualResult: String? = null
        val onResultCallback = { result: String? -> actualResult = result }
        val evaluateJavascriptCallbackSlot = slot<ValueCallback<String>>()
        every { webView.evaluateJavascript(script, capture(evaluateJavascriptCallbackSlot)) } just runs
        webViewClient.onPageFinished(webView, pageUrl)

        // when
        musicScriptEngine.execute(script, onResultCallback)
        evaluateJavascriptCallbackSlot.captured.onReceiveValue(expectedResult)

        // then
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `given webview is not initialized when execute is called then script is queued`() {
        // given
        val script = "const VF = Vex.Flow; new VF.Note({keys: [\"c/4\"], duration: \"q\" });"
        var wasCalled = false
        val onResultCallback = { _: String? -> wasCalled = true }

        // when
        musicScriptEngine.execute(script, onResultCallback)

        // then
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
        assertFalse(wasCalled)
    }

    @Test
    fun `given script is queued when webview is initialized then script is executed`() {
        // given
        val script = "const VF = Vex.Flow; new VF.Note({keys: [\"c/4\"], duration: \"q\" });"
        val expectedResult = "{\"duration\":\"q\",\"keys\":[\"c/4\"]}"
        var actualResult: String? = null
        val onResultCallback = { result: String? -> actualResult = result }
        val evaluateJavascriptCallbackSlot = slot<ValueCallback<String>>()
        every { webView.evaluateJavascript(script, capture(evaluateJavascriptCallbackSlot)) } just runs
        musicScriptEngine.execute(script, onResultCallback)

        // when
        webViewClient.onPageFinished(webView, pageUrl)
        evaluateJavascriptCallbackSlot.captured.onReceiveValue(expectedResult)

        // then
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `given multiple scripts are queued when webview is initialized then all scripts are executed`() {
        // given
        val script1 = "const VF = Vex.Flow; new VF.Note({keys: [\"c/4\"], duration: \"q\" });"
        val expectedResult1 = "{\"duration\":\"q\",\"keys\":[\"c/4\"]}"
        var actualResult1: String? = null
        val onResultCallback1 = { result: String? -> actualResult1 = result }
        val evaluateJavascriptCallbackSlot1 = slot<ValueCallback<String>>()
        every { webView.evaluateJavascript(script1, capture(evaluateJavascriptCallbackSlot1)) } just runs
        musicScriptEngine.execute(script1, onResultCallback1)

        val script2 = "const VF = Vex.Flow; new VF.Note({keys: [\"d/4\"], duration: \"h\" });"
        val expectedResult2 = "{\"duration\":\"h\",\"keys\":[\"d/4\"]}"
        var actualResult2: String? = null
        val onResultCallback2 = { result: String? -> actualResult2 = result }
        val evaluateJavascriptCallbackSlot2 = slot<ValueCallback<String>>()
        every { webView.evaluateJavascript(script2, capture(evaluateJavascriptCallbackSlot2)) } just runs
        musicScriptEngine.execute(script2, onResultCallback2)

        // when
        webViewClient.onPageFinished(webView, pageUrl)
        evaluateJavascriptCallbackSlot1.captured.onReceiveValue(expectedResult1)
        evaluateJavascriptCallbackSlot2.captured.onReceiveValue(expectedResult2)

        // then
        assertEquals(expectedResult1, actualResult1)
        assertEquals(expectedResult2, actualResult2)
    }

    @Test
    fun `when immediate javascript execution fails then onResult is called with null`() {
        // given
        val script = "invalid script"
        var actualResult: String? = "a non-null initial value"
        val onResultCallback = { result: String? -> actualResult = result }
        val jsException = Exception("JS error")
        every { webView.evaluateJavascript(any(), any()) } throws jsException
        webViewClient.onPageFinished(webView, pageUrl)

        // when
        musicScriptEngine.execute(script, onResultCallback)

        // then
        assertNull(actualResult)
    }

    @Test
    fun `when queued javascript execution fails then onResult is called with null`() {
        // given
        // webview is NOT initialized, so script will be queued
        val script = "invalid script"
        var actualResult: String? = "a non-null initial value"
        val onResultCallback = { result: String? -> actualResult = result }
        val jsException = Exception("JS error from queued script")

        // Call execute. Script is queued because webview is not ready.
        musicScriptEngine.execute(script, onResultCallback)

        // Now, set up the mock for the failure that will happen when the queue is processed
        every { webView.evaluateJavascript(any(), any()) } throws jsException

        // when
        // webview becomes initialized, which triggers execution of the queued script
        webViewClient.onPageFinished(webView, pageUrl)

        // then
        // check that the callback was called with null as a result of the failure
        assertNull(actualResult)
    }
}
