package com.instadrop.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.instadrop.app.domain.model.InstaMedia

@Composable
fun ResolvingScreen(url: String) {
    Centered {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("Receiving Instagram link…", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DownloadingScreen(
    media: InstaMedia,
    percent: Int,
    bytesPerSecond: Long,
    itemIndex: Int,
    itemCount: Int,
    onCancel: () -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = if (percent >= 0) percent / 100f else 0f,
        label = "progress",
    )
    Centered {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (percent >= 0) animated else 0f },
                modifier = Modifier.size(120.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (percent >= 0) "$percent%" else "…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(24.dp))
        val title = if (itemCount > 1) {
            "Downloading item $itemIndex of $itemCount…"
        } else {
            "Downloading ${media.kind.label.lowercase()}…"
        }
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            formatSpeed(bytesPerSecond),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun DoneScreen(media: InstaMedia, savedCount: Int, onDone: () -> Unit) {
    Centered {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        val heading = if (savedCount > 1) "$savedCount files saved" else "Download complete"
        Text(heading, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Saved to your gallery · InstaDrop folder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Done", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onHome: () -> Unit,
) {
    Centered {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canRetry) OutlinedButton(onClick = onRetry) { Text("Retry") }
            Button(onClick = onHome) { Text("Home") }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun formatSpeed(bytesPerSecond: Long): String = when {
    bytesPerSecond <= 0 -> ""
    bytesPerSecond < 1024 * 1024 -> "%.0f KB/s".format(bytesPerSecond / 1024.0)
    else -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
}
