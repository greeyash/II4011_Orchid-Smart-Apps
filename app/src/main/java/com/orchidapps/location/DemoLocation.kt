package com.orchidapps.location

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
import kotlin.math.cos

/**
 * Mode Demo Lokasi: posisi palsu di kawasan Orchid Forest untuk uji coba dan rekaman video demo
 * tanpa harus berada di lokasi. Selama aktif, [LocationRepository] memakai posisi ini sebagai
 * pengganti GPS, sehingga geofence, getar, notifikasi, suara, dan Auto-Play terpicu seperti asli.
 *
 * Posisi digerakkan manual lewat joystick di peta ([setVelocity]), seolah pengunjung berjalan sendiri.
 */
object DemoLocation {

    /** Titik awal: sekitar pintu masuk, cukup jauh dari radius POI mana pun. */
    private const val START_LAT = -6.780550
    private const val START_LNG = 107.636659

    /** Kecepatan maksimum saat joystick didorong penuh (m/detik). */
    const val MAX_SPEED_MPS = 4.0

    private const val MOVE_TICK_MS = 100L
    private const val EMIT_MS = 500L
    private const val ACCURACY_M = 5f
    private const val METERS_PER_DEGREE = 111_320.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var moveJob: Job? = null

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _position = MutableStateFlow(START_LAT to START_LNG)

    /** Kecepatan saat ini (m/detik) ke timur dan ke utara, diatur oleh joystick. */
    @Volatile
    private var velocity = 0.0 to 0.0

    fun setEnabled(on: Boolean) {
        if (on && !_enabled.value) {
            _position.value = START_LAT to START_LNG
            startMoving()
        }
        if (!on) stopMoving()
        _enabled.value = on
    }

    fun current(): UserLocation = _position.value.let { (lat, lng) ->
        UserLocation(lat, lng, ACCURACY_M, System.currentTimeMillis())
    }

    /** Posisi demo dikirim tiap setengah detik, juga saat diam, seperti GPS sungguhan. */
    fun updates(): Flow<UserLocation> = flow {
        while (true) {
            emit(current())
            delay(EMIT_MS)
        }
    }

    /**
     * Arah dan kekuatan joystick: [east] dan [north] masing-masing -1..1.
     * (0, 0) berarti berhenti.
     */
    fun setVelocity(east: Double, north: Double) {
        velocity = east * MAX_SPEED_MPS to north * MAX_SPEED_MPS
    }

    /** Kembali ke titik awal (pintu masuk). */
    fun reset() {
        velocity = 0.0 to 0.0
        _position.value = START_LAT to START_LNG
    }

    private fun startMoving() {
        moveJob?.cancel()
        moveJob = scope.launch {
            while (isActive) {
                val (vEast, vNorth) = velocity
                if (vEast != 0.0 || vNorth != 0.0) {
                    val dt = MOVE_TICK_MS / 1000.0
                    val (lat, lng) = _position.value
                    val dLat = vNorth * dt / METERS_PER_DEGREE
                    val dLng = vEast * dt / (METERS_PER_DEGREE * cos(Math.toRadians(lat)))
                    _position.value = (lat + dLat) to (lng + dLng)
                }
                delay(MOVE_TICK_MS)
            }
        }
    }

    private fun stopMoving() {
        velocity = 0.0 to 0.0
        moveJob?.cancel()
        moveJob = null
    }
}
