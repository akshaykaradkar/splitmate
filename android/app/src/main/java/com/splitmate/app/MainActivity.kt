package com.splitmate.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var dbHelper: SplitMateDatabaseHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = SplitMateDatabaseHelper(this)
        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.textZoom = 100

        webView.setBackgroundColor(0xFFFAF6F0.toInt())
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(SplitMateBridge(), "SplitMateNative")

        webView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.handleAndroidBack && window.handleAndroidBack()") { res ->
                    if (res != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            finish()
                        }
                    }
                }
            }
        })
    }

    inner class SplitMateBridge {

        @JavascriptInterface
        fun triggerHaptic(type: String) {
            runOnUiThread {
                val flag = when (type) {
                    "confirm" -> HapticFeedbackConstants.CONFIRM
                    "clock_tick" -> HapticFeedbackConstants.CLOCK_TICK
                    else -> HapticFeedbackConstants.VIRTUAL_KEY
                }
                webView.performHapticFeedback(flag)
            }
        }

        @JavascriptInterface
        fun savePrivateDeviceState(stateJson: String): Boolean {
            val prefs = getSharedPreferences("splitmate_private_vault", Context.MODE_PRIVATE)
            prefs.edit().putString("app_state_v2", stateJson).apply()
            return true
        }

        @JavascriptInterface
        fun loadPrivateDeviceState(): String {
            val prefs = getSharedPreferences("splitmate_private_vault", Context.MODE_PRIVATE)
            return prefs.getString("app_state_v2", "") ?: ""
        }

        @JavascriptInterface
        fun shareLedgerExport(title: String, content: String) {
            runOnUiThread {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, title)
                startActivity(shareIntent)
            }
        }

        @JavascriptInterface
        fun saveUserProfile(alias: String, emoji: String, currency: String, isGuest: Boolean): String {
            dbHelper.updateActiveUserProfile(alias, emoji, currency, isGuest)
            return dbHelper.computeAuditHash()
        }

        @JavascriptInterface
        fun logQuickExpense(title: String, category: String, groupId: String, payerId: String, totalCents: Long): String {
            return dbHelper.insertQuickExpense(title, category, groupId, payerId, totalCents).toString()
        }

        @JavascriptInterface
        fun markDebtSettled(debtorName: String, creditorName: String, amountCents: Long, method: String): String {
            return dbHelper.recordSettlement(debtorName, creditorName, amountCents, method)
        }

        @JavascriptInterface
        fun calculateEqualSplit(totalCents: Long): String {
            val splits = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents,
                listOf("u_alex" to "Alex", "u_sam" to "Sam", "u_priya" to "Priya")
            )
            val arr = JSONArray()
            for (s in splits) {
                val o = JSONObject()
                o.put("memberId", s.memberId)
                o.put("displayName", s.displayName)
                o.put("finalCents", s.finalCents)
                o.put("plusOneCent", s.plusOneCent)
                arr.put(o)
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun getVaultAuditSnapshot(): String {
            return dbHelper.exportVaultSnapshotJson()
        }
    }
}
