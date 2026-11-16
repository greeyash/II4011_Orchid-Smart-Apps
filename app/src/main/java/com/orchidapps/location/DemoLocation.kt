package com.orchidapps.location

import com.orchidapps.data.Poi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Mode Demo Lokasi: posisi palsu di kawasan Orchid Forest untuk uji coba dan rekaman video demo
 * tanpa harus berada di lokasi. Selama aktif, [LocationRepository] memakai posisi ini sebagai
 * pengganti GPS, sehingga geofence, getar, notifikasi, suara, dan Auto-Play terpicu seperti asli.
 *
 * Posisi bisa "berjalan" ke sebuah POI ([walkTo]) atau berkeliling ke semua POI ([startTour]).
 */
object DemoLocation {

    /** Titik awal: sekitar pintu masuk, cukup jauh dari radius POI mana pun. */
    private const val START_LAT = -6.780550
    private const val START_LNG = 107.636659

    /** Kecepatan jalan simulasi (m/detik); sedikit lebih cepat dari jalan kaki agar video ringkas. */
    private const val SPEED_MPS = 3.0
    private const val TICK_MS = 1_000L
    private const val ACCURACY_M = 5f

    /** Lama berhenti di tiap POI saat tur otomatis, cukup untuk notifikasi dan awal narasi. */
    const val TOUR_DWELL_MS = 25_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var moveJob: Job? = null

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _position = MutableStateFlow(START_LAT to START_LNG)

    /** Tujuan yang sedang dituju, untuk ditampilkan di UI; null bila sedang diam. */
    private val _target = MutableStateFlow<String?>(null)
    val target: StateFlow<String?> = _target.asStateFlow()

    fun setEnabled(on: Boolean) {
        if (!on) stop()
        if (on && !_enabled.value) _position.value = START_LAT to START_LNG
        _enabled.value = on
    }

    fun current(): UserLocation = _position.value.let { (lat, lng) ->
        UserLocation(lat, lng, ACCURACY_M, System.currentTimeMillis())
    }

    /** Posisi demo dikirim tiap detik, juga saat diam, seperti GPS sungguhan. */
    fun updates(): Flow<UserLocation> = flow {
        while (true) {
            emit(current())
            delay(TICK_MS)
        }
    }

    /** Berjalan dari posisi sekarang ke [poi], lalu berhenti di titik tengahnya. */
    fun walkTo(poi: Poi) {
        moveJob?.cancel()
        moveJob = scope.launch {
            walk(poi)
            _target.value = null
        }
    }

    /** Tur otomatis: mengunjungi POI satu per satu (terdekat lebih dulu) dan berhenti sebentar. */
    fun startTour(pois: List<Poi>) {
        moveJob?.cancel()
        moveJob = scope.launch {
            val remaining = pois.toMutableList()
            while (remaining.isNotEmpty() && isActive) {
                val (lat, lng) = _position.value
                val next = remaining.minBy { Geo.distanceMeters(lat, lng, it.lat, it.lng) }
                remaining.remove(next)
                walk(next)
                delay(TOUR_DWELL_MS)
            }
            _target.value = null
        }
    }

    fun stop() {
        moveJob?.cancel()
        moveJob = null
        _target.value = null
    }

    /** Kembali ke titik awal (pintu masuk). */
    fun reset() {
        stop()
        _position.value = START_LAT to START_LNG
    }

    private suspend fun walk(poi: Poi) {
        _target.value = poi.name
        val (startLat, startLng) = _position.value
        val distance = Geo.distanceMeters(startLat, startLng, poi.lat, poi.lng)
        val steps = max(1, (distance / SPEED_MPS).toInt())
        for (i in 1..steps) {
            val t = i.toDouble() / steps
            _position.value = (startLat + (poi.lat - startLat) * t) to (startLng + (poi.lng - startLng) * t)
            delay(TICK_MS)
        }
    }
}
