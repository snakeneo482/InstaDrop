package com.instadrop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.instadrop.app.ui.AppRoot
import com.instadrop.app.ui.MainViewModel
import com.instadrop.app.ui.theme.InstaDropTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.settings.themeMode.collectAsState()
            val dynamicColor by viewModel.settings.dynamicColor.collectAsState()
            InstaDropTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AppRoot(viewModel)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Routes an incoming share / link intent into the ViewModel. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    viewModel.onSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
                }
            }
            Intent.ACTION_VIEW -> {
                viewModel.onSharedText(intent.dataString)
            }
        }
    }
}
