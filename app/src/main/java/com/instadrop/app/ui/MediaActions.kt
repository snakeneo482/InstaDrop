package com.instadrop.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.instadrop.app.domain.model.MediaType

/** User-facing actions on a saved item: open in gallery, share, copy caption. */
object MediaActions {

    fun open(context: Context, uri: Uri, type: MediaType) {
        val mime = if (type == MediaType.VIDEO) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { toast(context, "No app to open this media.") }
    }

    fun share(context: Context, uri: Uri, type: MediaType) {
        val mime = if (type == MediaType.VIDEO) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun copyCaption(context: Context, caption: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("caption", caption))
        toast(context, "Caption copied")
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
