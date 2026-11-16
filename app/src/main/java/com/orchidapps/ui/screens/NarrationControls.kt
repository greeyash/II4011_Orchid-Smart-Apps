package com.orchidapps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.audio.NarrationItem
import com.orchidapps.audio.NarrationPlayer
import com.orchidapps.audio.NarrationStatus
import com.orchidapps.ui.theme.OrchidColors
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun rememberNarrationPlayer(): NarrationPlayer {
    val context = LocalContext.current
    return remember { NarrationPlayer.get(context) }
}

/**
 * Kartu narasi seperti desain hi-fi: tombol bulat putar/jeda, judul, progress bar, dan waktu.
 * Mendukung putar, jeda, lanjut, dan ulang (F03 AC2).
 */
@Composable
fun NarrationCard(item: NarrationItem, modifier: Modifier = Modifier) {
    val player = rememberNarrationPlayer()
    val state by player.state.collectAsStateWithLifecycle()
    val isThis = state.item?.id == item.id
    val status = if (isThis) state.status else NarrationStatus.IDLE

    NarrationSurface(
        title = "Narasi: ${item.title}",
        status = status,
        message = if (isThis) state.message else null,
        onPlayPause = {
            when (status) {
                NarrationStatus.PLAYING -> player.pause()
                NarrationStatus.PAUSED, NarrationStatus.ENDED -> player.resume()
                NarrationStatus.PREPARING -> Unit
                NarrationStatus.IDLE, NarrationStatus.ERROR -> player.play(item)
            }
        },
        onReplay = if (isThis && status != NarrationStatus.ERROR) player::replay else null,
        onClose = null,
        progress = { if (isThis) player.progress() else 0L to 0L },
        modifier = modifier,
    )
}

/** Kontrol audio ringkas yang tampil di semua layar selama narasi aktif (UI04). */
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {
    val player = rememberNarrationPlayer()
    val state by player.state.collectAsStateWithLifecycle()
    val item = state.item ?: return
    if (!state.isActive) return

    NarrationSurface(
        title = item.title,
        status = state.status,
        message = state.message,
        onPlayPause = player::togglePlayPause,
        onReplay = player::replay,
        onClose = player::stop,
        progress = player::progress,
        modifier = modifier,
        elevated = true,
    )
}

@Composable
private fun NarrationSurface(
    title: String,
    status: NarrationStatus,
    message: String?,
    onPlayPause: () -> Unit,
    onReplay: (() -> Unit)?,
    onClose: (() -> Unit)?,
    progress: () -> Pair<Long, Long>,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
) {
    var position by remember { mutableStateOf(0L to 0L) }
    LaunchedEffect(status) {
        position = progress()
        while (status == NarrationStatus.PLAYING) {
            delay(500)
            position = progress()
        }
    }

    PaperCard(modifier = modifier.fillMaxWidth(), elevated = elevated) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrchidColors.Forest),
                contentAlignment = Alignment.Center,
            ) {
                if (status == NarrationStatus.PREPARING) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = OrchidColors.Paper, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (status == NarrationStatus.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (status == NarrationStatus.PLAYING) "Jeda" else "Putar",
                            tint = OrchidColors.Paper,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val (pos, dur) = position
                LinearProgressIndicator(
                    progress = { if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = OrchidColors.Forest,
                    trackColor = OrchidColors.Cream,
                    drawStopIndicator = {},
                )
                Text(
                    when {
                        status == NarrationStatus.ERROR -> message ?: "Audio tidak tersedia."
                        status == NarrationStatus.PREPARING -> "Menyiapkan suara…"
                        status == NarrationStatus.IDLE -> "Ketuk untuk mendengarkan"
                        else -> "${formatTime(pos)} / ${formatTime(dur)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == NarrationStatus.ERROR) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            onReplay?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Replay, contentDescription = "Ulang") }
            }
            onClose?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Close, contentDescription = "Tutup audio") }
            }
        }
        if (status == NarrationStatus.ERROR && onClose == null) {
            TextButton(onClick = onPlayPause, modifier = Modifier.padding(start = 8.dp)) { Text("Coba lagi") }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
}
