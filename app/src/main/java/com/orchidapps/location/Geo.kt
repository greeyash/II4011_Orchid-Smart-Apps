package com.orchidapps.location

import com.orchidapps.data.Poi
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Kecepatan jalan kaki rata-rata untuk estimasi waktu (m/detik). */
    const val WALKING_SPEED_MPS = 1.2

    /** Jarak great-circle (Haversine) dalam meter. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    fun walkingMinutes(distanceMeters: Double): Int =
        maxOf(1, (distanceMeters / WALKING_SPEED_MPS / 60).toInt())

    /** POI terdekat dari posisi pengguna, atau null bila daftar kosong. */
    fun nearestPoi(lat: Double, lng: Double, pois: List<Poi>): Pair<Poi, Double>? =
        pois.map { it to distanceMeters(lat, lng, it.lat, it.lng) }.minByOrNull { it.second }

    /**
     * Titik-titik lingkaran berjari-jari [radiusMeters] untuk digambar di peta (PB04),
     * sebagai pasangan (lat, lng). Cincin tertutup: titik terakhir sama dengan titik pertama.
     */
    fun circlePolygon(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        segments: Int = 48,
    ): List<Pair<Double, Double>> {
        val dLat = Math.toDegrees(radiusMeters / EARTH_RADIUS_M)
        val dLng = dLat / cos(Math.toRadians(lat))
        return (0..segments).map { i ->
            val angle = 2 * PI * (i % segments) / segments
            Pair(lat + dLat * sin(angle), lng + dLng * cos(angle))
        }
    }
}
