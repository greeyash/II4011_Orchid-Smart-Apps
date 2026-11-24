package com.orchidapps.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.BuildConfig
import com.orchidapps.exploration.ExplorationSettings
import com.orchidapps.location.DemoLocation
import com.orchidapps.map.OfflineMapState
import com.orchidapps.map.OfflineMaps
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.PermissionStatus
import com.orchidapps.ui.rememberLocationPermissionRequest
import com.orchidapps.ui.theme.OrchidColors

/** Tab Pengaturan: Auto-Play, status izin lokasi, suara, dan info data. */
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val settings = remember { ExplorationSettings.get(context) }
    val autoPlay by settings.autoPlay.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val requestPermission = rememberLocationPermissionRequest(vm)

    Column(
        Modifier
            .fillMaxSize()
            .background(OrchidColors.Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium)

        PaperCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Audio Otomatis (Auto-Play)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Narasi langsung diputar setelah nama lokasi disebutkan saat Anda tiba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrchidColors.Muted,
                    )
                }
                Switch(
                    checked = autoPlay,
                    onCheckedChange = settings::setAutoPlay,
                    colors = SwitchDefaults.colors(checkedTrackColor = OrchidColors.Forest),
                )
            }
        }

        SectionCard("Izin lokasi") {
            Text(
                when (location.permission) {
                    PermissionStatus.GRANTED -> if (location.precise) "Diizinkan. Posisi Anda tampil di peta dan sesi jelajah bisa dimulai."
                    else "Hanya lokasi perkiraan. Pemicu lokasi dan navigasi butuh lokasi akurat."
                    PermissionStatus.NOT_REQUESTED -> "Belum diminta."
                    else -> "Ditolak. Anda tetap bisa memilih tempat secara manual."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (location.permission != PermissionStatus.GRANTED || !location.precise) {
                PrimaryButton("Izinkan Lokasi", onClick = requestPermission, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionCard("Suara narasi") {
            Text(
                "Narasi dibacakan oleh suara HP (text-to-speech) bila rekaman tim belum tersedia. " +
                    "Pastikan suara Bahasa Indonesia sudah terpasang.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SecondaryButton(
                "Buka Pengaturan Suara HP",
                onClick = {
                    runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OfflineMapCard(vm)

        if (BuildConfig.DEBUG) DemoLocationCard()

        SectionCard("Tentang data") {
            val count = (dataset as? DatasetState.Loaded)?.dataset?.pois?.size ?: 0
            Text(
                "$count titik wisata, foto, dan audio tersimpan di perangkat dan bisa dibuka tanpa internet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Versi ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = OrchidColors.Muted)
        }
    }
}

/**
 * Mode Demo Lokasi (hanya di build debug): posisi palsu di Orchid Forest untuk uji pemicu lokasi
 * dan rekaman video demo tanpa harus berada di lokasi. Posisi digerakkan dengan joystick di peta.
 */
@Composable
private fun DemoLocationCard() {
    val demo by DemoLocation.enabled.collectAsStateWithLifecycle()

    PaperCard(Modifier.fillMaxWidth(), color = OrchidColors.BlushLight) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mode Demo Lokasi", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Posisi palsu di Orchid Forest untuk uji pemicu lokasi tanpa ke lokasi. " +
                            "GPS asli tidak dipakai selama mode ini aktif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrchidColors.Muted,
                    )
                }
                Switch(
                    checked = demo,
                    onCheckedChange = DemoLocation::setEnabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = OrchidColors.Forest),
                )
            }
            if (demo) {
                Text(
                    "Cara pakai: buka Peta, tekan Mulai Jelajah, lalu geser joystick di sisi kiri peta " +
                        "untuk berjalan ke titik wisata mana pun.",
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryButton("Kembali ke Pintu Masuk", onClick = DemoLocation::reset, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Status peta offline (PB15, A01) dan tombol unduh ulang. */
@Composable
private fun OfflineMapCard(vm: MainViewModel) {
    val context = LocalContext.current
    val state by OfflineMaps.state.collectAsStateWithLifecycle()
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val pois = (dataset as? DatasetState.Loaded)?.dataset?.pois.orEmpty()
    SectionCard("Peta offline") {
        Text(
            when (val s = state) {
                OfflineMapState.Ready -> "Peta kawasan sudah tersimpan. Peta, notifikasi, detail, audio, pencarian, dan navigasi tetap berjalan dalam mode pesawat."
                is OfflineMapState.Downloading -> "Mengunduh peta kawasan… ${(s.progress * 100).toInt()}%"
                OfflineMapState.NotDownloaded -> "Peta kawasan belum tersimpan. Sambungkan ke internet sekali (mis. di loket) agar peta bisa dipakai tanpa sinyal."
                is OfflineMapState.Failed -> "Gagal mengunduh peta: ${s.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state is OfflineMapState.Downloading) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (state as OfflineMapState.Downloading).progress },
                modifier = Modifier.fillMaxWidth(),
                color = OrchidColors.Forest,
            )
        } else if (state !is OfflineMapState.Ready) {
            SecondaryButton(
                "Unduh Peta Offline",
                onClick = { OfflineMaps.ensure(context, pois, force = true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
