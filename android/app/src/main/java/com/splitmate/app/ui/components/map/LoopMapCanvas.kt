package com.splitmate.app.ui.components.map

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * v2.4.0 rc15: the Loop map's model. Built from the theme ([styleJson], [overlayJson]) and the
 * day's route ([dataJson]); all three are JSON strings from [LoopMapStyle].
 */
data class LoopMapModel(
    val styleJson: String,
    val overlayJson: String,
    val dataJson: String,
    /** For the fake canvas and TalkBack: stay + stops in route order. */
    val points: List<LoopMapPoint>
)

data class LoopMapPoint(val id: String, val index: Int, val lat: Double, val lng: Double, val isStay: Boolean)

/** Commands from Compose (toolbar, list taps) to the map. Calls before the page is ready are queued. */
class LoopMapController {
    internal var send: ((String) -> Unit)? = null
    private val pending = ArrayList<String>()
    internal var reducedMotion = false

    internal fun attach(sender: (String) -> Unit) {
        send = sender
        pending.forEach(sender)
        pending.clear()
    }

    internal fun detach() { send = null }

    private fun run(js: String) { send?.invoke(js) ?: pending.add(js) }
    private val animate get() = if (reducedMotion) "false" else "true"

    fun zoomIn() = run("splitmate.zoomBy(1,$animate)")
    fun zoomOut() = run("splitmate.zoomBy(-1,$animate)")
    fun fitLoop() = run("splitmate.fit($animate)")
    fun select(id: String?) = run("splitmate.select(${if (id == null) "null" else JSONObject.quote(id)},$animate)")
}

/** Tests / screenshots swap the WebView for [FakeLoopMapCanvas] (WebView can't render on the JVM). */
val LocalUseFakeLoopMap = staticCompositionLocalOf { false }

/**
 * The map surface: MapLibre GL JS in the system WebView (no native code, so the APK barely grows).
 * The page and scripts are bundled assets served from [LoopMapStyle.ASSET_HOST]; the only network
 * host allowed is OpenFreeMap. JSON data is passed with `JSONObject.quote`, never concatenated raw.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoopMapCanvas(
    model: LoopMapModel,
    controller: LoopMapController,
    reducedMotion: Boolean,
    onStopTap: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current || LocalUseFakeLoopMap.current) {
        FakeLoopMapCanvas(model = model, modifier = modifier)
        return
    }
    controller.reducedMotion = reducedMotion
    val latestTap by rememberUpdatedState(onStopTap)
    val latestError by rememberUpdatedState(onError)
    var pageReady by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val main = remember { Handler(Looper.getMainLooper()) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setGeolocationEnabled(false)
                settings.mediaPlaybackRequiresUserGesture = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url
                        if (url.host == LoopMapStyle.ASSET_HOST) {
                            val path = url.path.orEmpty().removePrefix("/")
                            if (!path.startsWith("map/") || path.contains("..")) return blocked()
                            return runCatching {
                                WebResourceResponse(mimeOf(path), "utf-8", view.context.assets.open(path))
                            }.getOrElse { blocked() }
                        }
                        return if (LoopMapStyle.isAllowedHost(url.host)) null else blocked()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun onPageReady() = main.post { pageReady = true }.let { }
                    @JavascriptInterface fun onReady() = main.post { ready = true }.let { }
                    @JavascriptInterface fun onStopTap(id: String) = main.post { latestTap(id) }.let { }
                    @JavascriptInterface fun onError(message: String) = main.post { latestError(message.take(200)) }.let { }
                }, "SplitMateBridge")
                webView = this
                loadUrl(LoopMapStyle.PAGE_URL)
            }
        }
    )

    val view = webView
    // The page signals onPageReady once its scripts have run; then the map is initialised once
    // and kept in sync with style / data below.
    LaunchedEffect(view, pageReady) {
        if (view == null || !pageReady) return@LaunchedEffect
        val options = JSONObject().put("reducedMotion", reducedMotion).toString()
        view.evaluateJavascript(
            "splitmate.init(${JSONObject.quote(model.styleJson)},${JSONObject.quote(model.overlayJson)}," +
                "${JSONObject.quote(model.dataJson)},${JSONObject.quote(options)})",
            null
        )
    }
    LaunchedEffect(view, ready) {
        if (view != null && ready) controller.attach { js -> view.evaluateJavascript(js, null) }
    }
    var lastStyle by remember { mutableStateOf(model.styleJson) }
    var lastData by remember { mutableStateOf(model.dataJson) }
    LaunchedEffect(model.styleJson, model.overlayJson, ready) {
        if (ready && model.styleJson != lastStyle) {
            view?.evaluateJavascript("splitmate.setStyle(${JSONObject.quote(model.styleJson)},${JSONObject.quote(model.overlayJson)})", null)
            lastStyle = model.styleJson
        }
    }
    LaunchedEffect(model.dataJson, ready) {
        if (ready && model.dataJson != lastData) {
            view?.evaluateJavascript("splitmate.setData(${JSONObject.quote(model.dataJson)})", null)
            lastData = model.dataJson
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.detach()
            view?.apply {
                stopLoading()
                removeJavascriptInterface("SplitMateBridge")
                destroy()
            }
        }
    }
}

private fun blocked() = WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))

private fun mimeOf(path: String) = when {
    path.endsWith(".html") -> "text/html"
    path.endsWith(".js") -> "application/javascript"
    path.endsWith(".css") -> "text/css"
    else -> "application/octet-stream"
}
