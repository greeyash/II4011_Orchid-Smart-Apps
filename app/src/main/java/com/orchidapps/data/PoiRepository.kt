package com.orchidapps.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Membaca dataset POI dari assets/pois.json (PB03, A01: data tersimpan lokal, tanpa internet).
 */
class PoiRepository(private val context: Context) {

    suspend fun loadDataset(): PoiDataset = withContext(Dispatchers.IO) {
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        parseDataset(raw)
    }

    companion object {
        const val ASSET_FILE = "pois.json"

        private val json = Json { ignoreUnknownKeys = true }

        /** Dipisah dari [loadDataset] agar bisa diuji tanpa Context Android. */
        fun parseDataset(raw: String): PoiDataset {
            val dataset = json.decodeFromString<PoiDataset>(raw)
            validate(dataset)
            return dataset
        }

        private fun validate(dataset: PoiDataset) {
            require(dataset.pois.isNotEmpty()) { "Dataset POI kosong" }
            val duplicateIds = dataset.pois.groupBy { it.id }.filterValues { it.size > 1 }.keys
            require(duplicateIds.isEmpty()) { "ID POI duplikat: $duplicateIds" }
            val qrCodes = dataset.pois.flatMap { poi -> listOf(poi.qrCode) + poi.species.map { it.qrCode } }
            val duplicateQr = qrCodes.groupBy { it }.filterValues { it.size > 1 }.keys
            require(duplicateQr.isEmpty()) { "Kode QR duplikat: $duplicateQr" }
            dataset.pois.forEach { poi ->
                require(poi.lat in -90.0..90.0 && poi.lng in -180.0..180.0) {
                    "Koordinat ${poi.id} tidak valid"
                }
                require(poi.radiusMeters in 20..40) {
                    "Radius ${poi.id} harus 20-40 m, tertulis ${poi.radiusMeters} m"
                }
                require(poi.activities.isNotEmpty()) { "${poi.id} belum punya daftar kegiatan" }
                poi.species.forEach { s ->
                    require(s.name.isNotBlank() && s.characteristics.isNotBlank() && s.origin.isNotBlank()) {
                        "Data spesies ${poi.id}-${s.id} belum lengkap"
                    }
                    require(s.source.isNotBlank()) { "Spesies ${poi.id}-${s.id} belum punya sumber" }
                }
            }
        }
    }
}
