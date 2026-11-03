package com.orchidapps.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.data.Poi
import com.orchidapps.location.Geo
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel

/** Daftar POI; juga menjadi "mode manual" saat izin lokasi ditolak (PB02). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenPoi: (String) -> Unit,
) {
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih POI") },
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
                val rows = state.dataset.pois
                    .map { poi -> poi to userLoc?.let { Geo.distanceMeters(it.lat, it.lng, poi.lat, poi.lng) } }
                    .sortedBy { it.second ?: Double.MAX_VALUE }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rows, key = { it.first.id }) { (poi, distance) ->
                        PoiRow(poi, distance, onClick = { onOpenPoi(poi.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PoiRow(poi: Poi, distance: Double?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${poi.id} · ${poi.name}", style = MaterialTheme.typography.titleMedium)
                distance?.let {
                    Text(formatDistance(it), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                poi.category.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(poi.shortDescription, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
