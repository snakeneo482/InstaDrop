package com.instadrop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import com.instadrop.app.ui.MediaActions

@Composable
fun PreviewScreen(
    media: InstaMedia,
    onDownload: (MediaItem) -> Unit,
    onDownloadAll: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val item = media.primary

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.size(4.dp))
            Text(
                "${media.kind.label} detected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (item.type == MediaType.VIDEO) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.durationSeconds?.let { AssistChip(onClick = {}, label = { Text(formatDuration(it)) }) }
            AssistChip(onClick = {}, label = { Text(if (item.isHd) "HD" else "SD") })
            if (media.isCarousel) {
                AssistChip(onClick = {}, label = { Text("${media.items.size} items") })
            }
        }

        media.author?.let {
            Spacer(Modifier.height(12.dp))
            Text("@$it", fontWeight = FontWeight.SemiBold)
        }

        media.caption?.let { caption ->
            Spacer(Modifier.height(8.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
            TextButton(onClick = { MediaActions.copyCaption(context, caption) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Copy caption")
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onDownload(item) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                if (media.isCarousel) "Download this item" else "Download",
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (media.isCarousel) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDownloadAll,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Download all ${media.items.size}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

internal fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
