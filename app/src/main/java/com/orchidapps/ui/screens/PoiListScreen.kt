package com.orchidapps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.data.PoiCategory
import com.orchidapps.search.PoiSearch
import com.orchidapps.search.PoiSearchResult
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.theme.OrchidColors

/**
 * Tab Cari (PB10, F05) dan Tempat. Dengan [showSearch] = false layar ini menjadi daftar semua tempat,
 * yang juga dipakai sebagai "mode manual" saat izin lokasi ditolak (PB02).
 * Hasil diurutkan dari yang terdekat, lengkap dengan jarak dan perkiraan waktu jalan.
 */
@Composable
fun PoiListScreen(
    vm: MainViewModel,
    onOpenPoi: (String) -> Unit,
    showSearch: Boolean = true,
    autoFocus: Boolean = false,
) {
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val visited by vm.visitedPoiIds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<PoiCategory?>(null) }
    val focus = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxSize()
            .background(OrchidColors.Cream)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (showSearch) "Cari" else "Tempat", style = MaterialTheme.typography.headlineMedium)
            if (showSearch) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    placeholder = { Text("Cari tempat atau kegiatan, mis. kopi") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = OrchidColors.Paper,
                        unfocusedContainerColor = OrchidColors.Paper,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
                if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            }
            CategoryChips(category, onChange = { category = it })
        }

        when (val state = datasetState) {
            DatasetState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is DatasetState.Error -> Text(
                "Data POI gagal dibaca: ${state.message}",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )
            is DatasetState.Loaded -> {
                val userLoc = location.location
                val effectiveQuery = if (showSearch) query else ""
                val results = remember(state.dataset, effectiveQuery, category, userLoc) {
                    PoiSearch.search(state.dataset.pois, effectiveQuery, category, userLoc?.lat, userLoc?.lng)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (results.isEmpty()) {
                        item {
                            NoResults(query = effectiveQuery, onPickCategory = {
                                query = ""
                                category = it
                            })
                        }
                    }
                    items(results, key = { it.poi.id }) { result ->
                        PoiRow(result, visited = result.poi.id in visited, onClick = { onOpenPoi(result.poi.id) })
                    }
                    if (userLoc == null && results.isNotEmpty()) {
                        item {
                            Text(
                                "Jarak dan waktu jalan tampil setelah posisi Anda terbaca.",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrchidColors.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** F05 AC4: pesan bila kata kunci tidak ditemukan, dengan saran kategori. */
@Composable
private fun NoResults(query: String, onPickCategory: (PoiCategory) -> Unit) {
    SectionCard(title = "Tidak ditemukan") {
        Text(
            if (query.isBlank()) "Tidak ada tempat pada kategori ini."
            else "Tidak ada tempat yang cocok dengan \"$query\". Coba kata lain atau pilih kategori:",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PoiCategory.entries.forEach { c ->
                Box(Modifier.clip(CircleShape).clickable { onPickCategory(c) }) { Pill(c.label) }
            }
        }
    }
}

@Composable
private fun PoiRow(result: PoiSearchResult, visited: Boolean, onClick: () -> Unit) {
    val poi = result.poi
    PaperCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PoiImage(poi, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), showFlowers = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(poi.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(poi.category.label)
                    if (visited) Pill("Dikunjungi", style = PillStyle.DARK, leading = "✓")
                }
                Text(
                    result.distanceMeters?.let { "${formatDistance(it)} · ±${result.walkingMinutes} menit jalan kaki" }
                        ?: poi.shortDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrchidColors.Muted,
                    maxLines = 2,
                )
            }
            PoiNumberBadge(poi.number(), color = poi.category.markerColor)
        }
    }
}
