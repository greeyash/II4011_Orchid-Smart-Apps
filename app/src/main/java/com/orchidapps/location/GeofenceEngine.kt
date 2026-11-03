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
 * oleh Foreground Service di Sprint 3. Aturan anti-pemicu-palsu:
 * - posisi dengan akurasi lebih buruk dari [maxAccuracyMeters] diabaikan;
 * - masuk baru dihitung setelah [requiredFixes] posisi berturut-turut di dalam radius;
 * - keluar baru dihitung setelah melewati radius + [exitBufferMeters] (hysteresis);
 * - POI yang sama tidak memicu ENTER lagi sebelum [cooldownMillis] berlalu.
 */
class GeofenceEngine(
    private val maxAccuracyMeters: Float = 40f,
    private val requiredFixes: Int = 2,
    private val exitBufferMeters: Double = 10.0,
    private val cooldownMillis: Long = 10 * 60_000L,
) {
    private val inside = mutableSetOf<String>()
    private val consecutiveInside = mutableMapOf<String, Int>()
    private val lastEnterAt = mutableMapOf<String, Long>()

    /** POI yang saat ini dianggap sedang dikunjungi. */
    val insidePoiIds: Set<String> get() = inside.toSet()

    fun update(location: UserLocation, pois: List<Poi>): List<GeofenceEvent> {
        val accuracy = location.accuracyMeters
        if (accuracy == null || accuracy > maxAccuracyMeters) return emptyList()

        val events = mutableListOf<GeofenceEvent>()
        for (poi in pois) {
            val distance = Geo.distanceMeters(location.lat, location.lng, poi.lat, poi.lng)
            if (poi.id in inside) {
                if (distance > poi.radiusMeters + exitBufferMeters) {
                    inside.remove(poi.id)
                    consecutiveInside.remove(poi.id)
                    events += GeofenceEvent(poi, GeofenceTransition.EXIT, distance)
                }
                continue
            }
            if (distance > poi.radiusMeters) {
                consecutiveInside.remove(poi.id)
                continue
            }
            val count = (consecutiveInside[poi.id] ?: 0) + 1
            consecutiveInside[poi.id] = count
            if (count < requiredFixes) continue

            inside.add(poi.id)
            val last = lastEnterAt[poi.id]
            if (last == null || location.timeMillis - last >= cooldownMillis) {
                lastEnterAt[poi.id] = location.timeMillis
                events += GeofenceEvent(poi, GeofenceTransition.ENTER, distance)
            }
        }
        return events
    }

    fun reset() {
        inside.clear()
        consecutiveInside.clear()
        lastEnterAt.clear()
    }
}
