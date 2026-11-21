package com.orchidapps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.orchidapps.recommendation.Recommendation
import com.orchidapps.recommendation.Recommender
import com.orchidapps.ui.theme.OrchidColors
import kotlinx.coroutines.launch

/** "Lanjut ke Mana?" (L10-A, PB13): 1-3 POI berikutnya yang belum dikunjungi. */
@Composable
fun RecommendationCard(
    recommendations: List<Recommendation>,
    onNavigate: (Recommendation) -> Unit,
    onOpen: (Recommendation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = rememberNarrationPlayer()
    val scope = rememberCoroutineScope()
    PaperCard(modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lanjut ke Mana?", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tempat terdekat yang belum Anda kunjungi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrchidColors.Muted,
                    )
                }
                // F07: rekomendasi bisa didengar lewat audio.
                IconButton(onClick = { scope.launch { runCatching { player.speech.speak(Recommender.spoken(recommendations)) } } }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Dengarkan rekomendasi")
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Tutup") }
            }
            RecommendationList(recommendations, onNavigate, onOpen)
        }
    }
}

@Composable
fun RecommendationList(
    recommendations: List<Recommendation>,
    onNavigate: (Recommendation) -> Unit,
    onOpen: (Recommendation) -> Unit,
) {
    if (recommendations.isEmpty()) {
        Text(
            "Selamat, seluruh titik wisata sudah Anda jelajahi!",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    recommendations.forEachIndexed { index, rec ->
        if (index > 0) HorizontalDivider(color = OrchidColors.Cream)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PoiImage(rec.poi, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), showFlowers = false)
            Column(Modifier.weight(1f)) {
                Text(rec.poi.name, style = MaterialTheme.typography.titleSmall)
                Text(rec.reason, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${formatDistance(rec.distanceMeters)} · ±${rec.walkingMinutes} menit jalan kaki",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrchidColors.Muted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                "Navigasi",
                onClick = { onNavigate(rec) },
                icon = Icons.Filled.Directions,
                modifier = Modifier.weight(1f).height(44.dp),
            )
            SecondaryButton("Lihat Detail", onClick = { onOpen(rec) }, modifier = Modifier.weight(1f).height(44.dp))
        }
    }
}
