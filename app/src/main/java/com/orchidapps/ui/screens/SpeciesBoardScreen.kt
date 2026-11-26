package com.orchidapps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.theme.OrchidColors

/**
 * Hasil scan QR papan anggrek (PB14, F08c): satu papan memuat 4 jenis anggrek, jadi pengunjung
 * melihat daftarnya dulu lalu memilih jenis yang ingin dibuka dan didengarkan.
 */
@Composable
fun SpeciesBoardScreen(
    vm: MainViewModel,
    poiId: String,
    boardId: String,
    onBack: () -> Unit,
    onOpenSpecies: (String) -> Unit,
    onScanAgain: () -> Unit,
    onOpenPoi: () -> Unit,
) {
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val poi = remember(dataset, poiId) { vm.poiById(poiId) }
    val board = poi?.boards?.firstOrNull { it.id == boardId }

    if (poi == null || board == null) {
        Box(Modifier.fillMaxSize().background(OrchidColors.Cream), contentAlignment = Alignment.Center) {
            Text("Papan anggrek tidak ditemukan.")
        }
        return
    }
    val species = poi.speciesOn(board)

    Column(
        Modifier
            .fillMaxSize()
            .background(OrchidColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Papan ${board.number} dari ${poi.boards.size}")
                Pill(poi.name, style = PillStyle.LIGHT)
            }
            Text(board.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Ada ${species.size} jenis anggrek di papan ini. Pilih yang ingin Anda kenali lebih dulu.",
                style = MaterialTheme.typography.bodyMedium,
                color = OrchidColors.Muted,
            )
            species.forEach { s ->
                val index = poi.species.indexOf(s)
                PaperCard(Modifier.fillMaxWidth().clickable { onOpenSpecies(s.id) }) {
                    Row(
                        Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpeciesImage(poi, s, index, Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
                            Text(
                                s.origin,
                                style = MaterialTheme.typography.labelSmall,
                                color = OrchidColors.Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Buka ${s.name}",
                            tint = OrchidColors.ForestDark,
                        )
                    }
                }
            }
            TextButton(onClick = onOpenPoi, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Lihat semua anggrek di ${poi.name}", color = OrchidColors.ForestDark)
            }
        }
        SecondaryButton(
            "Scan papan lain",
            onClick = onScanAgain,
            icon = Icons.Filled.QrCodeScanner,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
