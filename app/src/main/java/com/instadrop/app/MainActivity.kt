package com.instadrop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.instadrop.app.ui.AppRoot
import com.instadrop.app.ui.MainViewModel
import com.instadrop.app.ui.theme.InstaDropTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InstaDropTheme {
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
