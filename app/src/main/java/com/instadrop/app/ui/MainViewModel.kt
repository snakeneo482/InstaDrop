package com.instadrop.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instadrop.app.data.ServiceLocator
import com.instadrop.app.data.download.SavedMedia
import com.instadrop.app.domain.InstagramUrl
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Resolving(val url: String) : Screen
    data class Preview(val media: InstaMedia) : Screen
    data class Downloading(
        val media: InstaMedia,
        val percent: Int,
        val bytesPerSecond: Long,
        val itemIndex: Int = 1,
        val itemCount: Int = 1,
    ) : Screen
    data class Done(val media: InstaMedia, val savedCount: Int) : Screen
    data class Error(val message: String, val url: String?) : Screen
}

class MainViewModel : ViewModel() {

    private val resolver = ServiceLocator.resolver
    private val downloader = ServiceLocator.downloader
    private val history = ServiceLocator.history
    private val notifier = ServiceLocator.notifier

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val downloads = history.entries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
    )

    private var downloadJob: Job? = null
    private var lastSavedUri: Uri = Uri.EMPTY

    fun onSharedText(text: String?) {
        val url = InstagramUrl.extract(text) ?: return
        resolve(url)
    }

    fun resolve(url: String) {
        val clean = InstagramUrl.extract(url)
        if (clean == null) {
            _screen.value = Screen.Error("That doesn't look like an Instagram link.", url)
            return
        }
        _screen.value = Screen.Resolving(clean)
        viewModelScope.launch {
            runCatching { resolver.resolve(clean) }
                .onSuccess { _screen.value = Screen.Preview(it) }
                .onFailure {
                    _screen.value = Screen.Error(it.message ?: "Couldn't fetch that media.", clean)
                }
        }
    }

    /** Download a single item (the default), e.g. a Reel or one carousel slide. */
    fun download(media: InstaMedia, item: MediaItem = media.primary) =
        runDownload(media, listOf(item))

    /** Download every asset in a carousel, sequentially. */
    fun downloadAll(media: InstaMedia) = runDownload(media, media.items)

    private fun runDownload(media: InstaMedia, items: List<MediaItem>) {
        downloadJob?.cancel()
        _screen.value = Screen.Downloading(media, 0, 0, itemIndex = 1, itemCount = items.size)
        downloadJob = viewModelScope.launch {
            runCatching {
                var saved = 0
                items.forEachIndexed { index, item ->
                    notifier.showProgress(NOTIF_ID, "Downloading ${media.kind.label}", 0)
                    val result = downloader.download(item, displayName = suggestName(media, index)) { percent, bps ->
                        _screen.value = Screen.Downloading(
                            media, percent.coerceAtLeast(0), bps,
                            itemIndex = index + 1, itemCount = items.size,
                        )
                        notifier.showProgress(NOTIF_ID, "Downloading ${media.kind.label}", percent)
                    }
                    persist(result, item)
                    saved++
                }
                saved
            }.onSuccess { saved ->
                val label = if (saved == 1) "1 file saved to gallery" else "$saved files saved to gallery"
                notifier.showComplete(NOTIF_ID, label, lastSavedUri, media.primary.type)
                _screen.value = Screen.Done(media, saved)
            }.onFailure { e ->
                if (e is CancellationException) throw e // let cancellation propagate
                notifier.cancel(NOTIF_ID)
                _screen.value = Screen.Error(e.message ?: "Download failed.", media.sourceUrl)
            }
        }
    }

    private suspend fun persist(result: SavedMedia, item: MediaItem) {
        history.add(
            displayName = result.displayName,
            uri = result.uri,
            type = item.type,
            thumbnailUrl = item.thumbnailUrl,
            sizeBytes = result.sizeBytes,
        )
        lastSavedUri = result.uri
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        notifier.cancel(NOTIF_ID)
        val current = _screen.value
        if (current is Screen.Downloading) _screen.value = Screen.Preview(current.media)
    }

    fun goHome() {
        downloadJob?.cancel()
        _screen.value = Screen.Home
    }

    private fun suggestName(media: InstaMedia, index: Int): String {
        val handle = media.author?.takeIf { it.isNotBlank() } ?: media.kind.label.lowercase()
        val suffix = if (media.items.size > 1) "_${index + 1}" else ""
        return "${handle}_${media.kind.name.lowercase()}_${System.currentTimeMillis()}$suffix"
    }

    private companion object {
        const val NOTIF_ID = 1001
    }
}
