package com.orchidapps.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.exploration.ExplorationSettings
import com.orchidapps.location.Geo
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.LocationUiState
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.PermissionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenMap: () -> Unit,
    onOpenPoiList: () -> Unit,
    onOpenPoi: (String) -> Unit,
) {
    val context = LocalContext.current
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val locationState by vm.location.collectAsStateWithLifecycle()
    val explorationActive by vm.explorationActive.collectAsStateWithLifecycle()
    val visited by vm.visitedPoiIds.collectAsStateWithLifecycle()
    val settings = remember { ExplorationSettings.get(context) }
    val autoPlay by settings.autoPlay.collectAsStateWithLifecycle()

    // Izin notifikasi (Android 13+) diminta saat "Mulai Jelajah"; sesi tetap jalan bila ditolak
    // karena getar dan suara tidak membutuhkan izin ini.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.startExploration() }
    val startExploration = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startExploration()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        // Setelah ditolak, rationale=false berarti pengguna memilih "Jangan tanya lagi".
        val activity = context.findActivity()
        val permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            )
        vm.onPermissionResult(granted, permanentlyDenied)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Orchid Apps") }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Pemandu wisata Orchid Forest Cikole",
                style = MaterialTheme.typography.bodyLarge,
            )

            DatasetCard(datasetState, onRetry = vm::loadDataset)

            when (locationState.permission) {
                PermissionStatus.NOT_REQUESTED -> PermissionRequestCard(
                    onRequest = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                    onManual = onOpenPoiList,
                )
                PermissionStatus.DENIED,
                PermissionStatus.PERMANENTLY_DENIED -> PermissionDeniedCard(
                    permanentlyDenied = locationState.permission == PermissionStatus.PERMANENTLY_DENIED,
                    onRetry = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                        )
                    },
                    onManual = onOpenPoiList,
                )
                PermissionStatus.GRANTED -> CurrentLocationCard(
                    state = locationState,
                    datasetState = datasetState,
                    onEnableLocation = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    onOpenPoi = onOpenPoi,
                )
            }

            ExplorationCard(
                active = explorationActive,
                visitedCount = visited.size,
                canStart = locationState.permission == PermissionStatus.GRANTED,
                onStart = startExploration,
                onEnd = vm::endExploration,
                autoPlay = autoPlay,
                onAutoPlayChange = settings::setAutoPlay,
            )

            Button(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Text("  Lihat Peta")
            }
            OutlinedButton(onClick = onOpenPoiList, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Text("  Lihat Semua POI")
            }
        }
    }
}

/** Mulai/Akhiri Jelajah (PB08). */
@Composable
private fun ExplorationCard(
    active: Boolean,
    visitedCount: Int,
    canStart: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
) {
    SectionCard(
        title = if (active) "Jelajah aktif" else "Jelajah",
        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (active) {
            Text(
                "Posisi Anda terus dipantau, juga saat layar mati. Ponsel bergetar dua kali dan " +
                    "menyebutkan nama lokasi saat Anda tiba di titik wisata."
            )
            Text("$visitedCount titik wisata sudah dikunjungi.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text("  Akhiri Jelajah")
            }
        } else {
            Text(
                "Tekan Mulai Jelajah, lalu simpan ponsel di saku. Aplikasi memberi tahu lewat getar " +
                    "dan suara saat Anda tiba di titik wisata."
            )
            Button(onClick = onStart, enabled = canStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Explore, contentDescription = null)
                Text("  Mulai Jelajah")
            }
            if (!canStart) {
                Text(
                    "Butuh izin lokasi. Tanpa izin, Anda tetap bisa memilih POI secara manual.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto-Play narasi", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Narasi langsung diputar setelah tiba di titik wisata.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = autoPlay, onCheckedChange = onAutoPlayChange)
        }
    }
}

@Composable
private fun DatasetCard(state: DatasetState, onRetry: () -> Unit) {
    SectionCard(title = "Data POI") {
        when (state) {
            DatasetState.Loading -> Text("Memuat data POI…")
            is DatasetState.Loaded -> Text(
                "${state.dataset.pois.size} POI tersimpan di perangkat (bisa dipakai tanpa internet): " +
                    state.dataset.pois.joinToString { it.name }
            )
            is DatasetState.Error -> {
                Text("Data POI gagal dibaca: ${state.message}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequest: () -> Unit, onManual: () -> Unit) {
    SectionCard(title = "Izinkan akses lokasi") {
        Text(
            "Orchid Apps memakai lokasi Anda untuk menampilkan posisi Anda di kawasan " +
                "dan memberi tahu saat Anda tiba di sebuah titik wisata. " +
                "Lokasi hanya diproses di perangkat ini."
        )
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.MyLocation, contentDescription = null)
            Text("  Izinkan Akses Lokasi")
        }
        TextButton(onClick = onManual) { Text("Lewati, pilih POI secara manual") }
    }
}

@Composable
private fun PermissionDeniedCard(
    permanentlyDenied: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onManual: () -> Unit,
) {
    SectionCard(
        title = "Izin lokasi ditolak",
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            "Tanpa izin lokasi, aplikasi tidak dapat menampilkan posisi Anda atau memberi " +
                "notifikasi saat tiba di titik wisata. Anda tetap bisa memilih POI secara manual " +
                "untuk membaca informasinya."
        )
        if (permanentlyDenied) {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Pengaturan Aplikasi")
            }
        } else {
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Minta Izin Lagi")
            }
        }
        Button(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
            Text("Pilih POI Manual")
        }
    }
}

@Composable
private fun CurrentLocationCard(
    state: LocationUiState,
    datasetState: DatasetState,
    onEnableLocation: () -> Unit,
    onOpenPoi: (String) -> Unit,
) {
    SectionCard(title = "Lokasi Anda") {
        if (!state.locationEnabled) {
            Text("Layanan lokasi (GPS) di perangkat sedang mati.")
            OutlinedButton(onClick = onEnableLocation) { Text("Nyalakan Lokasi") }
            return@SectionCard
        }
        if (state.showWeakGps) WeakGpsBadge()

        val loc = state.location
        if (loc == null) {
            Text(
                if (state.noFixTimeout) "Sinyal GPS belum didapat. Coba pindah ke area yang lebih terbuka."
                else "Mencari posisi Anda…"
            )
        } else {
            Text("Lat: ${formatCoord(loc.lat)}\nLng: ${formatCoord(loc.lng)}")
            Text(
                "Akurasi: " + (loc.accuracyMeters?.let { "±${it.toInt()} m" } ?: "tidak diketahui") +
                    " · diperbarui " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(loc.timeMillis)),
                style = MaterialTheme.typography.bodySmall,
            )

            val pois = (datasetState as? DatasetState.Loaded)?.dataset?.pois.orEmpty()
            Geo.nearestPoi(loc.lat, loc.lng, pois)?.let { (poi, distance) ->
                val inside = distance <= poi.radiusMeters
                Text(
                    if (inside) "Anda berada di area ${poi.name}"
                    else "POI terdekat: ${poi.name} (${formatDistance(distance)}, " +
                        "±${Geo.walkingMinutes(distance)} menit jalan kaki)",
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = { onOpenPoi(poi.id) }) { Text("Buka ${poi.name}") }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
