package com.instadrop.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.instadrop.app.ui.screens.DoneScreen
import com.instadrop.app.ui.screens.DownloadingScreen
import com.instadrop.app.ui.screens.ErrorScreen
import com.instadrop.app.ui.screens.HomeScreen
import com.instadrop.app.ui.screens.PreviewScreen
import com.instadrop.app.ui.screens.ResolvingScreen
import com.instadrop.app.ui.screens.SettingsScreen

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val context = LocalContext.current

    // On Android < 10, saving to the gallery needs WRITE_EXTERNAL_STORAGE.
    // We request it lazily, right before the first download that needs it.
    // Backed by state so it survives recomposition while the system dialog is up.
    var pendingDownload by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingDownload?.invoke()
        pendingDownload = null
    }

    fun withStoragePermission(action: () -> Unit) {
        val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        if (!needsLegacyPermission ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingDownload = action
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // Ask for notification permission once on Android 13+, so download progress
    // and "complete" notifications can show. Declined is fine — they just no-op.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored: notifications are best-effort */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { current ->
                when (current) {
                    is Screen.Home -> HomeScreen(
                        downloads = downloads,
                        onSubmitUrl = viewModel::resolve,
                        onRemove = viewModel::removeFromHistory,
                        onClearAll = viewModel::clearHistory,
                        onOpenSettings = viewModel::openSettings,
                    )

                    is Screen.Resolving -> ResolvingScreen(url = current.url)

                    is Screen.Preview -> PreviewScreen(
                        media = current.media,
                        onDownload = { item ->
                            withStoragePermission { viewModel.download(current.media, item) }
                        },
                        onDownloadAll = {
                            withStoragePermission { viewModel.downloadAll(current.media) }
                        },
                        onBack = viewModel::goHome,
                    )

                    is Screen.Downloading -> DownloadingScreen(
                        media = current.media,
                        percent = current.percent,
                        bytesPerSecond = current.bytesPerSecond,
                        itemIndex = current.itemIndex,
                        itemCount = current.itemCount,
                        onCancel = viewModel::cancelDownload,
                    )

                    is Screen.Done -> DoneScreen(
                        media = current.media,
                        savedCount = current.savedCount,
                        onDone = viewModel::goHome,
                    )

                    is Screen.Error -> ErrorScreen(
                        message = current.message,
                        canRetry = current.url != null,
                        onRetry = { current.url?.let(viewModel::resolve) },
                        onHome = viewModel::goHome,
                    )

                    is Screen.Settings -> SettingsScreen(
                        settings = viewModel.settings,
                        onBack = viewModel::goHome,
                    )
                }
            }
        }
    }
}
