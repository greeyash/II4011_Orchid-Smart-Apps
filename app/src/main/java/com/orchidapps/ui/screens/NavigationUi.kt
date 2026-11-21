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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.orchidapps.navigation.NavigationState
import com.orchidapps.navigation.Turn
import com.orchidapps.ui.theme.OrchidColors
import kotlinx.coroutines.delay

/** Kartu arahan di atas peta (L07-A): ikon belokan dan teks "Belok kanan, lalu lurus 120 m". */
@Composable
fun NavigationTopCard(state: NavigationState, modifier: Modifier = Modifier) {
    // UI18: tampilkan "Menghitung ulang rute…" beberapa detik setelah rute dihitung ulang.
    var showReroute by remember { mutableStateOf(false) }
    LaunchedEffect(state.reroutedAtMillis) {
        if (state.reroutedAtMillis != null) {
            showReroute = true
            delay(3_000)
            showReroute = false
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = OrchidColors.Forest, contentColor = OrchidColors.Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrchidColors.Blush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(state.turn.icon(), contentDescription = null, tint = OrchidColors.ForestDark)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (showReroute) "MENGHITUNG ULANG RUTE…" else "BERIKUTNYA",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrchidColors.Blush,
                )
                Text(state.instruction, style = MaterialTheme.typography.titleLarge, color = OrchidColors.Paper)
            }
        }
    }
}

/** Kartu bawah selama navigasi: tujuan, sisa jarak, ETA, dan tombol Hentikan Navigasi. */
@Composable
fun NavigationBottomCard(state: NavigationState, startDistance: Double, onStop: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MENUJU", style = MaterialTheme.typography.labelSmall, color = OrchidColors.Muted)
            Text(state.target.name, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(state.target.category.label)
                Text(
                    "Sisa ${formatDistance(state.remainingMeters)} · ±${state.etaMinutes} menit",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (startDistance > 0) (1 - state.remainingMeters / startDistance).toFloat().coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = OrchidColors.Forest,
                trackColor = OrchidColors.Cream,
                drawStopIndicator = {},
            )
            Text(
                "Rute berupa garis lurus ke tujuan; ikuti jalan setapak yang tersedia.",
                style = MaterialTheme.typography.labelSmall,
                color = OrchidColors.Muted,
            )
            SecondaryButton("Hentikan Navigasi", onClick = onStop, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** UI19: konfirmasi tiba di tujuan navigasi. Rekomendasi berikutnya ditampilkan di bawahnya. */
@Composable
fun NavigationArrivedCard(
    state: NavigationState,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    PaperCard(modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(OrchidColors.Forest),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Flag, contentDescription = null, tint = OrchidColors.Paper) }
                Column {
                    Text("Anda telah tiba", style = MaterialTheme.typography.labelSmall, color = OrchidColors.Muted)
                    Text(state.target.name, style = MaterialTheme.typography.titleLarge)
                }
            }
            extra()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Lihat Detail", onClick = onOpen, modifier = Modifier.weight(1f))
                SecondaryButton("Selesai", onClick = onDone)
            }
        }
    }
}

private fun Turn.icon(): ImageVector = when (this) {
    Turn.STRAIGHT -> Icons.Filled.Straight
    Turn.SLIGHT_LEFT -> Icons.Filled.TurnSlightLeft
    Turn.SLIGHT_RIGHT -> Icons.Filled.TurnSlightRight
    Turn.LEFT -> Icons.Filled.TurnLeft
    Turn.RIGHT -> Icons.Filled.TurnRight
    Turn.U_TURN -> Icons.Filled.UTurnLeft
    Turn.UNKNOWN -> Icons.AutoMirrored.Filled.ArrowForward
}
