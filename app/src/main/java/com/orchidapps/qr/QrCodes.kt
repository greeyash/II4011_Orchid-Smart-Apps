package com.orchidapps.qr

import com.orchidapps.data.OrchidSpecies
import com.orchidapps.data.Poi

sealed interface QrResult {
    data class PoiCode(val poi: Poi) : QrResult
    data class SpeciesCode(val poi: Poi, val species: OrchidSpecies) : QrResult
    data class Unknown(val raw: String) : QrResult
}

/**
 * Mencocokkan isi kode QR di papan informasi (PB14, A02) dengan data lokal, tanpa internet.
 * Format: `ORCHID-P01` untuk papan POI dan `ORCHID-P04-S01` untuk papan jenis anggrek.
 */
object QrCodes {

    fun resolve(raw: String, pois: List<Poi>): QrResult {
        val code = raw.trim().uppercase()
        pois.forEach { poi ->
            if (poi.qrCode.equals(code, ignoreCase = true)) return QrResult.PoiCode(poi)
            poi.species.firstOrNull { it.qrCode.equals(code, ignoreCase = true) }
                ?.let { return QrResult.SpeciesCode(poi, it) }
        }
        return QrResult.Unknown(raw)
    }
}
