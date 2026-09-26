package com.splitmate.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.data.SplitMateRoomDatabase
import com.splitmate.app.ui.SplitMateMaterial3ExpressiveTheme
import com.splitmate.app.ui.SplitMateViewModel

/**
 * 100% Native Jetpack Compose MainActivity (`setContent { ... }`).
 * Zero WebView, zero HTML/JS assets. Wired directly to Jetpack Room + Retrofit Frankfurter API + Coil SVG.
 */
class MainActivity : ComponentActivity() {

    private lateinit var splitMateViewModel: SplitMateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SplitMateRoomDatabase.getInstance(applicationContext)
        splitMateViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SplitMateViewModel(dao = database.dao()) as T
                }
            }
        )[SplitMateViewModel::class.java]

        if (savedInstanceState == null) {
            handleIncomingSyncIntent(intent)
        }

        setContent {
            val uiState by splitMateViewModel.uiState.collectAsStateWithLifecycle()
            SplitMateMaterial3ExpressiveTheme(darkTheme = uiState.isDarkTheme) {
                SplitMateApp(viewModel = splitMateViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingSyncIntent(intent)
    }

    private fun handleIncomingSyncIntent(incomingIntent: Intent?) {
        if (incomingIntent == null || !::splitMateViewModel.isInitialized) return
        if (incomingIntent.getBooleanExtra("com.splitmate.SYNC_CONSUMED", false)) return
        when (incomingIntent.action) {
            Intent.ACTION_VIEW -> {
                val dataUri = incomingIntent.data ?: return
                val isTripSync = dataUri.scheme.equals("splitmate", ignoreCase = true) && dataUri.host.equals("trip-sync", ignoreCase = true)
                val isJoinApp = dataUri.scheme.equals("splitmate", ignoreCase = true) && dataUri.host.equals("join", ignoreCase = true)
                val isJoinWeb = dataUri.scheme.equals("https", ignoreCase = true) && dataUri.host.equals("akshaykaradkar.github.io", ignoreCase = true) && dataUri.path?.startsWith("/splitmate/join") == true
                
                if (isTripSync) {
                    val rawPayload = dataUri.getQueryParameter("payload")?.takeIf { it.isNotBlank() }
                        ?: dataUri.toString()
                    if (SplitMateViewModel.extractSyncTokenFromRawInput(rawPayload) != null) {
                        incomingIntent.putExtra("com.splitmate.SYNC_CONSUMED", true)
                        splitMateViewModel.importAndMergeGroupSyncPayload(
                            rawPayloadOrMessage = rawPayload,
                            openGroupAfterMerge = true
                        )
                    }
                } else if (isJoinApp || isJoinWeb) {
                    val shortKey = dataUri.getQueryParameter("g")
                    if (!shortKey.isNullOrBlank()) {
                        incomingIntent.putExtra("com.splitmate.SYNC_CONSUMED", true)
                        splitMateViewModel.resolveAndMergeShortInviteKey(shortKey)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (incomingIntent.type?.startsWith("text/plain", ignoreCase = true) == true) {
                    val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    
                    // Support pasting full short links directly via ACTION_SEND
                    val gParam = extractShortKeyFromUrl(sharedText)
                    if (gParam != null) {
                        incomingIntent.putExtra("com.splitmate.SYNC_CONSUMED", true)
                        splitMateViewModel.resolveAndMergeShortInviteKey(gParam)
                    } else if (SplitMateViewModel.extractSyncTokenFromRawInput(sharedText) != null) {
                        incomingIntent.putExtra("com.splitmate.SYNC_CONSUMED", true)
                        splitMateViewModel.importAndMergeGroupSyncPayload(
                            rawPayloadOrMessage = sharedText,
                            openGroupAfterMerge = true
                        )
                    }
                }
            }
        }
    }
    
    private fun extractShortKeyFromUrl(text: String): String? {
        val urlRegex = Regex("""https://akshaykaradkar\.github\.io/splitmate/join\?g=([A-Za-z0-9_-]+)""")
        return urlRegex.find(text)?.groupValues?.getOrNull(1)
    }
}
