package com.orchidapps.recommendation

import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.Geo

data class Recommendation(
    val poi: Poi,
    val distanceMeters: Double,
    /** Alasan singkat, mis. "Dekat dari posisi Anda, cocok untuk berfoto". */
    val reason: String,
) {
    val walkingMinutes: Int get() = Geo.walkingMinutes(distanceMeters)
}

/**
 * Rekomendasi POI berikutnya (PB13, F07): hanya POI yang belum dikunjungi pada sesi ini,
 * diurutkan dari yang terdekat, dengan alasan singkat dari jarak dan kegiatan yang tersedia.
 */
object Recommender {

    fun recommend(
        pois: List<Poi>,
        visited: Set<String>,
        fromLat: Double,
        fromLng: Double,
        exclude: Set<String> = emptySet(),
        max: Int = 3,
    ): List<Recommendation> = pois
        .filter { it.id !in visited && it.id !in exclude }
        .map { it to Geo.distanceMeters(fromLat, fromLng, it.lat, it.lng) }
        .sortedBy { it.second }
        .take(max)
        .mapIndexed { index, (poi, distance) -> Recommendation(poi, distance, reasonFor(poi, distance, index == 0)) }

    internal fun reasonFor(poi: Poi, distance: Double, nearest: Boolean): String {
        val near = when {
            nearest -> "Paling dekat dari posisi Anda"
            distance < 150 -> "Dekat dari posisi Anda"
            else -> "Sekitar ${Geo.walkingMinutes(distance)} menit jalan kaki"
        }
        return "$near, ${activityHint(poi)}"
    }

    /** Petunjuk kegiatan dari kategori, kegiatan, dan alias POI. */
    internal fun activityHint(poi: Poi): String {
        val text = (poi.activities + poi.aliases + poi.name).joinToString(" ").lowercase()
        return when {
            "anggrek" in text -> "banyak jenis anggrek"
            poi.category == PoiCategory.TEMPAT_MAKAN || "kopi" in text || "makan" in text -> "cocok untuk makan dan istirahat"
            "kelinci" in text || "kuda" in text || "hewan" in text -> "bisa berinteraksi dengan hewan"
            "anak" in text || "ayunan" in text || "perosotan" in text -> "seru untuk bermain bersama anak"
            "golf" in text || "catur" in text || "permainan" in text -> "seru untuk bermain bersama"
            "foto" in text || "berfoto" in text -> "cocok untuk berfoto"
            "santai" in text || "live music" in text -> "cocok untuk bersantai"
            else -> "menarik untuk dikunjungi"
        }
    }

    /** Kalimat untuk dibacakan (F07: rekomendasi bisa didengar lewat audio). */
    fun spoken(recommendations: List<Recommendation>): String =
        if (recommendations.isEmpty()) {
            "Selamat, seluruh titik wisata sudah Anda jelajahi."
        } else {
            "Rekomendasi berikutnya: " + recommendations.joinToString(". ") {
                "${it.poi.name}, ${it.reason.replaceFirstChar(Char::lowercase)}"
            } + "."
        }
}
