package com.orchidapps.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.audio.AudioGuide
import com.orchidapps.data.OrchidSpecies
import com.orchidapps.data.Poi
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.illustration.OrchidIllustration
import com.orchidapps.ui.theme.OrchidColors

private val Rose = Color(0xFF9A4A62)

/**
 * Halaman detail POI (L05-A/L05-B, PB06): gambar, deskripsi, kegiatan, info keselamatan,
 * narasi audio, dan informasi botani untuk POI yang punya daftar anggrek (PB11).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoiDetailScreen(
    vm: MainViewModel,
    poiId: String,
    onBack: () -> Unit,
    onOpenSpecies: (String) -> Unit,
    onNavigate: (Poi) -> Unit,
    onScan: () -> Unit,
) {
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val visited by vm.visitedPoiIds.collectAsStateWithLifecycle()
    val poi = remember(dataset, poiId) { vm.poiById(poiId) }

    if (poi == null) {
        Box(Modifier.fillMaxSize().background(OrchidColors.Cream), contentAlignment = Alignment.Center) {
            Text("POI $poiId tidak ditemukan.")
        }
        return
    }

    Box(Modifier.fillMaxSize().background(OrchidColors.Cream)) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box {
            PoiPhotoGallery(poi, Modifier.fillMaxWidth().height(280.dp))
            CircleIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                onClick = onBack,
                modifier = Modifier.statusBarsPadding().padding(16.dp),
            )
        }
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(poi.category.label)
                if (poi.id in visited) Pill("Sudah dikunjungi", style = PillStyle.DARK, leading = "✓")
            }
            Text(poi.name, style = MaterialTheme.typography.headlineMedium)
            Text(poi.description, style = MaterialTheme.typography.bodyMedium, color = OrchidColors.Muted)

            SectionLabel("Kegiatan")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                poi.activities.forEach { Pill(it, style = PillStyle.LIGHT) }
            }

            // Pengingat keselamatan ditampilkan sebelum narasi (PRD 4.1, A04).
            poi.safetyNote?.let { SafetyCard(it) }

            NarrationCard(AudioGuide.item(poi))

            if (poi.species.isNotEmpty()) {
                SpeciesSection(poi, onOpenSpecies)
                // L05-B/F08c: pindai QR di papan anggrek untuk langsung mendengar info spesies.
                SecondaryButton(
                    "Scan QR Spesies",
                    onClick = onScan,
                    icon = Icons.Filled.QrCodeScanner,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            poi.infoTitle?.let { title ->
                SectionCard(title = title) {
                    Text(poi.info ?: "Belum tersedia.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    // F05/F06: mulai navigasi langsung dari halaman detail.
    PrimaryButton(
        "Mulai Navigasi",
        onClick = { onNavigate(poi) },
        icon = Icons.Filled.Directions,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
    )
    }
}

@Composable
private fun SafetyCard(text: String) {
    PaperCard(Modifier.fillMaxWidth(), color = OrchidColors.BlushLight) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("⚠️ Keselamatan", style = MaterialTheme.typography.titleSmall, color = Rose)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Rose)
        }
    }
}

/** "Kenali Anggrek" (L05-B): kartu tiap jenis anggrek dengan fotonya, membuka halaman spesies. */
@Composable
private fun SpeciesSection(poi: Poi, onOpenSpecies: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Kenali anggrek · ${poi.species.size} jenis", Modifier.weight(1f))
        if (poi.species.any { it.checkedAt == null }) {
            Text("Belum dicek pengelola", style = MaterialTheme.typography.labelSmall, color = Rose)
        }
    }
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        poi.species.forEachIndexed { index, species ->
            PaperCard(
                Modifier
                    .width(132.dp)
                    .clickable { onOpenSpecies(species.id) },
            ) {
                SpeciesImage(
                    poi, species, index,
                    Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                )
                Text(
                    species.name,
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Foto spesies dari naskah tim; ilustrasi bila foto belum ada. */
@Composable
private fun SpeciesImage(poi: Poi, species: OrchidSpecies, index: Int, modifier: Modifier = Modifier) {
    val photo = rememberAssetBitmap(AudioGuide.speciesPhotoPath(poi, species))
    if (photo != null) {
        Image(photo, contentDescription = "Foto ${species.name}", modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        OrchidIllustration(speciesPetalColor(index), modifier)
    }
}

/** Halaman detail satu jenis anggrek (L09, UI14): foto, ciri, asal, klasifikasi, dan narasinya (F08b). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeciesDetailScreen(vm: MainViewModel, poiId: String, speciesId: String, onBack: () -> Unit) {
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val poi = remember(dataset, poiId) { vm.poiById(poiId) }
    val index = poi?.species?.indexOfFirst { it.id == speciesId } ?: -1
    val species: OrchidSpecies? = poi?.species?.getOrNull(index)

    if (poi == null || species == null) {
        Box(Modifier.fillMaxSize().background(OrchidColors.Cream), contentAlignment = Alignment.Center) {
            Text("Spesies tidak ditemukan.")
        }
        return
    }

    Column(Modifier.fillMaxSize().background(OrchidColors.Cream)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box {
                SpeciesImage(poi, species, index, Modifier.fillMaxWidth().height(280.dp))
                CircleIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(16.dp),
                )
            }
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Spesies anggrek")
                    Pill(poi.name, style = PillStyle.LIGHT)
                }
                Text(species.name, style = MaterialTheme.typography.headlineMedium, fontStyle = FontStyle.Italic)

                NarrationCard(AudioGuide.speciesItem(poi, species))

                SectionCard("Ciri") {
                    species.characteristics.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
                SectionCard("Asal") {
                    Text(species.origin, style = MaterialTheme.typography.bodyMedium)
                }
                if (species.classification.isNotEmpty()) {
                    SectionCard("Klasifikasi") {
                        species.classification.forEach { line ->
                            val (rank, value) = line.split(':', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
                            Row {
                                Text(rank, Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = OrchidColors.Muted)
                                Text(value, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton(
            "Kembali ke ${poi.name}",
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

/** Warna kelopak ilustrasi per jenis: putih, krem, ungu, lilac. */
private fun speciesPetalColor(index: Int): Color = when (index % 4) {
    0 -> Color(0xFFFEFCF7)
    1 -> Color(0xFFFFF1C9)
    2 -> Color(0xFFD9B8E6)
    else -> Color(0xFFF0C4E4)
}
