package com.orchidapps.location

import com.orchidapps.data.Poi

enum class GeofenceTransition { ENTER, EXIT }

data class GeofenceEvent(
    val poi: Poi,
    val transition: GeofenceTransition,
    val distanceMeters: Double,
)

/**
 * Deteksi masuk/keluar radius POI dari aliran posisi (PB05).
 *
 * Dibuat sendiri (bukan GeofencingClient) agar bisa diuji tanpa perangkat dan dipakai ulang
 * oleh Foreground Service. Aturan anti-pemicu-palsu:
 * - posisi dengan akurasi lebih buruk dari [maxAccuracyMeters] diabaikan;
 * - masuk baru dihitung setelah [requiredFixes] posisi berturut-turut di dalam radius;
 * - keluar baru dihitung setelah melewati radius + [exitBufferMeters] (hysteresis);
 * - POI yang sama tidak memicu ENTER lagi sebelum [cooldownMillis] berlalu.
 *
 * Untuk POI yang radiusnya tumpang tindih (F02: POI terdekat didahulukan):
 * - pengunjung hanya dianggap berada di satu POI, yaitu yang paling dekat di antara POI yang
 *   radiusnya dimasuki; pindah ke POI tetangga baru terjadi bila POI itu lebih dekat
 *   minimal [switchMarginMeters], agar tidak bolak-balik di garis batas;
 * - dua kedatangan berjarak minimal [minGapMillis]; kedatangan berikutnya ditunda (bukan
 *   dibuang) sampai jeda selesai, selama pengunjung masih berada di POI tersebut.
 */
class GeofenceEngine(
    private val maxAccuracyMeters: Float = 40f,
    private val requiredFixes: Int = 2,
    private val exitBufferMeters: Double = 10.0,
    private val cooldownMillis: Long = 10 * 60_000L,
    private val switchMarginMeters: Double = 5.0,
    private val minGapMillis: Long = 30_000L,
) {
    /** POI tempat pengunjung saat ini berada; paling banyak satu. */
    private var current: String? = null
    private var candidate: String? = null
    private var candidateFixes = 0
    private val lastEnterAt = mutableMapOf<String, Long>()
    private var lastAnyEnterAt: Long? = null

    /** POI yang saat ini dianggap sedang dikunjungi (paling banyak satu). */
    val insidePoiIds: Set<String> get() = setOfNotNull(current)

    fun update(location: UserLocation, pois: List<Poi>): List<GeofenceEvent> {
        val accuracy = location.accuracyMeters
        if (accuracy == null || accuracy > maxAccuracyMeters) return emptyList()

        val distances = pois.associateWith { Geo.distanceMeters(location.lat, location.lng, it.lat, it.lng) }
        val nearest = distances.filter { (poi, d) -> d <= poi.radiusMeters }.minByOrNull { it.value }
        val events = mutableListOf<GeofenceEvent>()

        // Keluar dari POI sekarang: melewati radius + buffer, atau POI lain jelas lebih dekat.
        val currentPoi = pois.firstOrNull { it.id == current }
        if (currentPoi != null) {
            val d = distances.getValue(currentPoi)
            val beyond = d > currentPoi.radiusMeters + exitBufferMeters
            val switched = nearest != null && nearest.key.id != currentPoi.id &&
                nearest.value + switchMarginMeters < d
            if (beyond || switched) {
                current = null
                events += GeofenceEvent(currentPoi, GeofenceTransition.EXIT, d)
            }
        } else if (current != null) {
            current = null
        }

        if (current == null) {
            if (nearest == null) {
                candidate = null
                candidateFixes = 0
            } else {
                val poi = nearest.key
                if (candidate == poi.id) candidateFixes++ else {
                    candidate = poi.id
                    candidateFixes = 1
                }
                val gapOk = lastAnyEnterAt?.let { location.timeMillis - it >= minGapMillis } ?: true
                if (candidateFixes >= requiredFixes && gapOk) {
                    current = poi.id
                    candidate = null
                    candidateFixes = 0
                    val last = lastEnterAt[poi.id]
                    if (last == null || location.timeMillis - last >= cooldownMillis) {
                        lastEnterAt[poi.id] = location.timeMillis
                        lastAnyEnterAt = location.timeMillis
                        events += GeofenceEvent(poi, GeofenceTransition.ENTER, nearest.value)
                    }
                }
            }
        }
        return events
    }

    fun reset() {
        current = null
        candidate = null
        candidateFixes = 0
        lastEnterAt.clear()
        lastAnyEnterAt = null
    }
}
