package com.orchidapps.qr

import com.orchidapps.data.OrchidSpecies
import com.orchidapps.data.Poi
import com.orchidapps.data.SpeciesBoard

sealed interface QrResult {
    data class PoiCode(val poi: Poi) : QrResult
    data class BoardCode(val poi: Poi, val board: SpeciesBoard) : QrResult
    data class SpeciesCode(val poi: Poi, val species: OrchidSpecies) : QrResult
    data class Unknown(val raw: String) : QrResult
}

/**
 * Mencocokkan isi kode QR di papan informasi (PB14, A02) dengan data lokal, tanpa internet.
 * Format: `ORCHID-P01` untuk papan POI dan `ORCHID-P04-B1` untuk papan anggrek (4 jenis per papan).
 * Kode lama per jenis (`ORCHID-P04-S01`) tetap dikenali.
 */
object QrCodes {

    fun resolve(raw: String, pois: List<Poi>): QrResult {
        val code = raw.trim().uppercase()
        pois.forEach { poi ->
            if (poi.qrCode.equals(code, ignoreCase = true)) return QrResult.PoiCode(poi)
            poi.boards.firstOrNull { it.qrCode.equals(code, ignoreCase = true) }
                ?.let { return QrResult.BoardCode(poi, it) }
            poi.species.firstOrNull { it.qrCode.equals(code, ignoreCase = true) }
                ?.let { return QrResult.SpeciesCode(poi, it) }
        }
        return QrResult.Unknown(raw)
    }
}
