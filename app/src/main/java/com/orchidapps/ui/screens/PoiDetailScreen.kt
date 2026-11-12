package com.orchidapps.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.audio.AudioGuide
import com.orchidapps.data.Poi
import com.orchidapps.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Halaman informasi POI: foto, deskripsi, pemandu audio, kegiatan, info keselamatan (PB06). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(vm: MainViewModel, poiId: String, onBack: () -> Unit) {
    // Dikoleksi agar halaman ikut diperbarui saat dataset selesai dimuat.
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val poi = remember(dataset, poiId) { vm.poiById(poiId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(poi?.name ?: "POI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        if (poi == null) {
            Text("POI $poiId tidak ditemukan.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${poi.id} · ${poi.category.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            PoiPhoto(poi)
            // Pengingat keselamatan ditampilkan sebelum narasi (PRD 4.1).
            poi.safetyNote?.let {
                SectionCard(
                    title = "Info keselamatan",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) { Text(it) }
            }
            Text(poi.description, style = MaterialTheme.typography.bodyLarge)

            SectionCard(title = "Pemandu audio") {
                NarrationControls(AudioGuide.item(poi))
            }

            SectionCard(title = "Kegiatan") {
                poi.activities.forEach { Text("• $it") }
            }

            if (poi.species.isNotEmpty()) {
                SpeciesSection(poi)
            }

            poi.infoTitle?.let { title ->
                SectionCard(title = title) {
                    Text(poi.info ?: "Belum tersedia.")
                }
            }

            SectionCard(title = "Data lokasi") {
                Text("Koordinat: ${formatCoord(poi.lat)}, ${formatCoord(poi.lng)}")
                Text("Radius pemicu: ${poi.radiusMeters} m")
                if (!poi.coordinateVerified) {
                    Text(
                        "Koordinat masih perkiraan dan perlu dicek di lapangan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Text(
                "Sumber: ${poi.source}\nDicek: ${poi.checkedAt ?: "belum dicek ke pengelola"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Foto dari `assets/images/<id POI>.jpg`; placeholder bila file belum ditambahkan. */
@Composable
private fun PoiPhoto(poi: Poi) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, poi.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(AudioGuide.photoPath(poi)).use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    val frame = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(MaterialTheme.shapes.medium)

    val bitmap = photo
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Foto ${poi.name}", modifier = frame, contentScale = ContentScale.Crop)
    } else {
        Box(frame.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Foto belum tersedia", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Informasi botani: jenis anggrek di sekitar POI beserta narasinya (PB11, F03, F08b). */
@Composable
private fun SpeciesSection(poi: Poi) {
    SectionCard(title = "Informasi botani · ${poi.species.size} jenis anggrek") {
        poi.species.forEachIndexed { index, species ->
            if (index > 0) HorizontalDivider()
            Text(species.name, style = MaterialTheme.typography.titleSmall)
            Text(
                species.latinName,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
            )
            Text("Ciri: ${species.characteristics}", style = MaterialTheme.typography.bodyMedium)
            Text("Asal: ${species.origin}", style = MaterialTheme.typography.bodyMedium)
            NarrationControls(AudioGuide.speciesItem(poi, species), playLabel = "Dengarkan info ${species.name}")
            Text(
                "Sumber: ${species.source} · Dicek: ${species.checkedAt ?: "belum"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
