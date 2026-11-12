package com.orchidapps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.audio.NarrationItem
import com.orchidapps.audio.NarrationPlayer
import com.orchidapps.audio.NarrationStatus

@Composable
fun rememberNarrationPlayer(): NarrationPlayer {
    val context = LocalContext.current
    return remember { NarrationPlayer.get(context) }
}

/** Tombol putar/jeda/lanjut/ulang untuk satu narasi (F03 AC2). */
@Composable
fun NarrationControls(item: NarrationItem, modifier: Modifier = Modifier, playLabel: String = "Dengarkan") {
    val player = rememberNarrationPlayer()
    val state by player.state.collectAsStateWithLifecycle()
    val isThis = state.item?.id == item.id
    val status = if (isThis) state.status else NarrationStatus.IDLE

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            NarrationStatus.IDLE -> Button(onClick = { player.play(item) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  $playLabel")
            }
            NarrationStatus.PREPARING -> Text("Menyiapkan suara…")
            NarrationStatus.PLAYING, NarrationStatus.PAUSED, NarrationStatus.ENDED -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (status == NarrationStatus.PLAYING) {
                    Button(onClick = player::pause, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Pause, contentDescription = null)
                        Text("  Jeda")
                    }
                } else {
                    Button(onClick = player::resume, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(if (status == NarrationStatus.ENDED) "  Putar lagi" else "  Lanjutkan")
                    }
                }
                OutlinedButton(onClick = player::replay, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Text("  Ulang")
                }
            }
            NarrationStatus.ERROR -> {
                Text(state.message ?: "Audio tidak tersedia.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { player.play(item) }) { Text("Coba lagi") }
            }
        }
        if (status == NarrationStatus.IDLE && item.recordingAsset != null) {
            Text(
                "Bila rekaman tim belum ada, narasi dibacakan oleh suara HP.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Kontrol audio ringkas yang tampil di semua layar selama narasi aktif (UI04). */
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {
    val player = rememberNarrationPlayer()
    val state by player.state.collectAsStateWithLifecycle()
    val item = state.item ?: return
    if (!state.isActive) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (state.status) {
                        NarrationStatus.PREPARING -> "Menyiapkan suara…"
                        NarrationStatus.PLAYING -> "Narasi diputar"
                        NarrationStatus.PAUSED -> "Dijeda"
                        NarrationStatus.ENDED -> "Selesai"
                        NarrationStatus.ERROR -> state.message ?: "Audio tidak tersedia"
                        NarrationStatus.IDLE -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.status == NarrationStatus.PLAYING) {
                IconButton(onClick = player::pause) { Icon(Icons.Filled.Pause, contentDescription = "Jeda") }
            } else if (state.status != NarrationStatus.PREPARING && state.status != NarrationStatus.ERROR) {
                IconButton(onClick = player::resume) { Icon(Icons.Filled.PlayArrow, contentDescription = "Lanjutkan") }
            }
            IconButton(onClick = player::replay) { Icon(Icons.Filled.Replay, contentDescription = "Ulang") }
            IconButton(onClick = player::stop) { Icon(Icons.Filled.Close, contentDescription = "Tutup audio") }
        }
    }
}
