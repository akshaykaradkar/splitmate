package com.splitmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitmate.app.data.SplitMateRoomDatabase
import com.splitmate.app.ui.SplitMateMaterial3ExpressiveTheme
import com.splitmate.app.ui.SplitMateNativeApp
import com.splitmate.app.ui.SplitMateViewModel

/**
 * 100% Native Jetpack Compose MainActivity (`setContent { ... }`).
 * Zero WebView, zero HTML/JS assets. Wired directly to Jetpack Room + Retrofit Frankfurter API + Coil SVG.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SplitMateRoomDatabase.getInstance(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SplitMateViewModel(dao = database.dao()) as T
                }
            }
        )[SplitMateViewModel::class.java]

        setContent {
            SplitMateMaterial3ExpressiveTheme {
                SplitMateNativeApp(viewModel = viewModel)
            }
        }
    }
}
