package com.orchidapps.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.data.PoiCategory
import com.orchidapps.search.PoiSearch
import com.orchidapps.search.PoiSearchResult
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel

/**
 * Pencarian dan daftar POI (PB10, F05); juga menjadi "mode manual" saat izin lokasi ditolak (PB02).
 * Hasil diurutkan dari yang terdekat, lengkap dengan jarak dan perkiraan waktu jalan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenPoi: (String) -> Unit,
) {
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val visited by vm.visitedPoiIds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<PoiCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cari POI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = datasetState) {
            DatasetState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is DatasetState.Error -> Text(
                "Data POI gagal dibaca: ${state.message}",
                modifier = Modifier.padding(padding).padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )
            is DatasetState.Loaded -> {
                val userLoc = location.location
                val results = remember(state.dataset, query, category, userLoc) {
                    PoiSearch.search(state.dataset.pois, query, category, userLoc?.lat, userLoc?.lng)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SearchControls(
                            query = query,
                            onQueryChange = { query = it },
                            category = category,
                            onCategoryChange = { category = it },
                        )
                    }
                    if (results.isEmpty()) {
                        item {
                            NoResults(
                                query = query,
                                onPickCategory = {
                                    query = ""
                                    category = it
                                },
                            )
                        }
                    }
                    items(results, key = { it.poi.id }) { result ->
                        PoiRow(result, visited = result.poi.id in visited, onClick = { onOpenPoi(result.poi.id) })
                    }
                    if (userLoc == null && results.isNotEmpty()) {
                        item {
                            Text(
                                "Jarak dan waktu jalan tampil setelah posisi Anda terbaca.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchControls(
    query: String,
    onQueryChange: (String) -> Unit,
    category: PoiCategory?,
    onCategoryChange: (PoiCategory?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari tempat, mis. kopi, bridge, anggrek") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = category == null,
                onClick = { onCategoryChange(null) },
                label = { Text("Semua") },
            )
            PoiCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { onCategoryChange(if (category == c) null else c) },
                    label = { Text(c.label) },
                )
            }
        }
    }
}

/** F05 AC4: pesan bila kata kunci tidak ditemukan, dengan saran kategori. */
@Composable
private fun NoResults(query: String, onPickCategory: (PoiCategory) -> Unit) {
    SectionCard(title = "Tidak ditemukan") {
        Text(
            if (query.isBlank()) "Tidak ada POI pada kategori ini."
            else "Tidak ada tempat yang cocok dengan \"$query\". Coba kata lain atau pilih kategori:"
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PoiCategory.entries.forEach { c ->
                AssistChip(onClick = { onPickCategory(c) }, label = { Text(c.label) })
            }
        }
    }
}

@Composable
private fun PoiRow(result: PoiSearchResult, visited: Boolean, onClick: () -> Unit) {
    val poi = result.poi
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(poi.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                result.distanceMeters?.let {
                    Text(
                        "${formatDistance(it)} · ±${result.walkingMinutes} mnt",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Text(
                poi.category.label + if (visited) " · sudah dikunjungi" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(poi.shortDescription, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
