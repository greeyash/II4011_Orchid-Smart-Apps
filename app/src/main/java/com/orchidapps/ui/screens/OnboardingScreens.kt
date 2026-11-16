package com.orchidapps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.PermissionStatus
import com.orchidapps.ui.illustration.SceneIllustration
import com.orchidapps.ui.illustration.SceneSubject
import com.orchidapps.ui.rememberLocationPermissionRequest
import com.orchidapps.ui.theme.OrchidColors

/** S00: layar sambutan. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(OrchidColors.Cream),
    ) {
        SceneIllustration(SceneSubject.GREENHOUSE, Modifier.fillMaxWidth().weight(1.25f))
        Column(
            Modifier
                .weight(1f)
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Pill("Orchid Forest Cikole")
            Text("Orchid Apps", style = MaterialTheme.typography.displaySmall, color = OrchidColors.ForestDark)
            Text(
                "Pemandu wisata Orchid Forest Cikole. Jelajahi, dengarkan, dan temukan tempat menarik di sekitar Anda.",
                style = MaterialTheme.typography.bodyLarge,
                color = OrchidColors.Muted,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Mulai", onClick = onStart, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** L01: penjelasan izin lokasi (UI01); pengunjung bisa memberi izin atau lanjut manual (F01 AC4). */
@Composable
fun PermissionScreen(vm: MainViewModel, onDone: () -> Unit) {
    val location by vm.location.collectAsStateWithLifecycle()
    val request = rememberLocationPermissionRequest(vm) { onDone() }
    val denied = location.permission == PermissionStatus.DENIED ||
        location.permission == PermissionStatus.PERMANENTLY_DENIED

    Column(
        Modifier
            .fillMaxSize()
            .background(OrchidColors.Cream)
            .verticalScroll(rememberScrollState()),
    ) {
        SceneIllustration(SceneSubject.TREES, Modifier.fillMaxWidth().height(220.dp))
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Izinkan akses lokasi", style = MaterialTheme.typography.headlineMedium, color = OrchidColors.ForestDark)
            Text(
                "Orchid Apps memakai lokasi Anda di kawasan untuk menampilkan posisi Anda, memicu narasi saat " +
                    "mendekati tempat menarik, dan memberi tahu saat Anda tiba di sebuah titik wisata.",
                style = MaterialTheme.typography.bodyMedium,
                color = OrchidColors.Muted,
            )
            BenefitRow(Icons.Filled.Place, "Posisi di peta", "Lihat di mana Anda berada")
            BenefitRow(Icons.Filled.Headphones, "Narasi otomatis", "Aktif juga saat layar mati")
            BenefitRow(Icons.Filled.NotificationsActive, "Pemberitahuan saat tiba", "Getar dan suara di tiap titik wisata")
            if (denied) {
                Text(
                    "Izin lokasi ditolak. Anda tetap bisa memilih tempat secara manual untuk membaca dan mendengar " +
                        "informasinya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                "Lokasi hanya diproses di perangkat ini dan tidak dibagikan.",
                style = MaterialTheme.typography.labelSmall,
                color = OrchidColors.Muted,
            )
            PrimaryButton(
                if (location.permission == PermissionStatus.PERMANENTLY_DENIED) "Buka Pengaturan Izin" else "Izinkan Lokasi",
                onClick = request,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Lanjutkan Secara Manual", fontWeight = FontWeight.SemiBold, color = OrchidColors.ForestDark)
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    PaperCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrchidColors.Blush),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = OrchidColors.ForestDark, modifier = Modifier.size(18.dp)) }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OrchidColors.Muted)
            }
        }
    }
}
