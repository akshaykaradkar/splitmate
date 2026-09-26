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
                if (dataUri.scheme.equals("splitmate", ignoreCase = true) &&
                    dataUri.host.equals("trip-sync", ignoreCase = true)
                ) {
                    val rawPayload = dataUri.getQueryParameter("payload")?.takeIf { it.isNotBlank() }
                        ?: dataUri.toString()
                    val claimParam = dataUri.getQueryParameter("claim")?.takeIf { it.isNotBlank() }
                    if (SplitMateViewModel.extractSyncTokenFromRawInput(rawPayload) != null) {
                        incomingIntent.putExtra("com.splitmate.SYNC_CONSUMED", true)
                        splitMateViewModel.importAndMergeGroupSyncPayload(
                            rawPayloadOrMessage = rawPayload,
                            claimedMemberIdOverride = claimParam,
                            openGroupAfterMerge = true
                        )
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (incomingIntent.type?.startsWith("text/plain", ignoreCase = true) == true) {
                    val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    if (SplitMateViewModel.extractSyncTokenFromRawInput(sharedText) != null) {
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
}
