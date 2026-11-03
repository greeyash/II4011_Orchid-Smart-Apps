package com.orchidapps.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
