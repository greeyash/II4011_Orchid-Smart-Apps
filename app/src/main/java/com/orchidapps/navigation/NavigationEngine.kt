package com.orchidapps.navigation

import com.orchidapps.data.Poi
import com.orchidapps.location.Geo
import com.orchidapps.location.UserLocation
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Turn(val label: String) {
    STRAIGHT("Lurus"),
    SLIGHT_LEFT("Agak ke kiri"),
    SLIGHT_RIGHT("Agak ke kanan"),
    LEFT("Belok kiri"),
    RIGHT("Belok kanan"),
    U_TURN("Putar balik"),
    UNKNOWN("Mulai berjalan"),
}

data class NavigationState(
    val target: Poi,
    /** Titik awal garis rute; diganti saat rute dihitung ulang. */
    val originLat: Double,
    val originLng: Double,
    val remainingMeters: Double,
    val turn: Turn = Turn.UNKNOWN,
    val arrived: Boolean = false,
    /** Waktu terakhir rute dihitung ulang (untuk banner "Menghitung ulang rute"). */
    val reroutedAtMillis: Long? = null,
) {
    val etaMinutes: Int get() = Geo.walkingMinutes(remainingMeters)

    /** Teks arahan seperti di desain: "Belok kanan, lurus 120 m". */
    val instruction: String
        get() = when {
            arrived -> "Anda telah tiba di ${target.name}"
            turn == Turn.STRAIGHT || turn == Turn.UNKNOWN -> "${turn.label} ${roundedDistance()}"
            else -> "${turn.label}, lalu lurus ${roundedDistance()}"
        }

    fun roundedDistance(): String {
        val m = remainingMeters
        return if (m < 100) "${(m / 5).roundToInt() * 5} m" else "${(m / 10).roundToInt() * 10} m"
    }
}

sealed interface NavigationEvent {
    data class Announce(val text: String) : NavigationEvent
    data object Rerouted : NavigationEvent
    data class Arrived(val poi: Poi) : NavigationEvent
}

/**
 * Logika navigasi jalan kaki (PB12, F06), terpisah dari Android agar bisa diuji.
 *
 * Data jalan setapak kawasan belum tersedia, sehingga rute digambar sebagai garis lurus dari titik
 * awal ke tujuan. Arahan belokan dihitung dari arah jalan pengunjung (dua posisi terakhir) terhadap
 * arah ke tujuan. Rute dihitung ulang bila pengunjung menyimpang lebih dari [rerouteMeters] dari
 * garis rute, dan kedatangan diumumkan saat jarak ke tujuan ≤ [arrivalMeters].
 */
class NavigationEngine(
    private val arrivalMeters: Double = 15.0,
    private val rerouteMeters: Double = 30.0,
    /** Jarak minimum antara dua posisi untuk menentukan arah jalan. */
    private val headingMinMeters: Double = 4.0,
    /** Pengingat jarak diucapkan tiap kelipatan ini. */
    private val reminderEveryMeters: Double = 50.0,
) {
    private var lastHeadingFix: UserLocation? = null
    private var heading: Double? = null
    private var lastAnnouncedTurn: Turn? = null
    private var pendingTurn: Turn? = null
    private var lastReminderBucket: Int? = null

    fun start(target: Poi, from: UserLocation): Pair<NavigationState, List<NavigationEvent>> {
        reset()
        lastHeadingFix = from
        val distance = Geo.distanceMeters(from.lat, from.lng, target.lat, target.lng)
        val state = NavigationState(target, from.lat, from.lng, distance)
        lastReminderBucket = bucket(distance)
        return state to listOf(
            NavigationEvent.Announce("Navigasi ke ${target.name} dimulai. Jarak ${state.roundedDistance()}."),
        )
    }

    fun update(state: NavigationState, loc: UserLocation): Pair<NavigationState, List<NavigationEvent>> {
        if (state.arrived) return state to emptyList()
        val target = state.target
        val events = mutableListOf<NavigationEvent>()
        val remaining = Geo.distanceMeters(loc.lat, loc.lng, target.lat, target.lng)

        if (remaining <= arrivalMeters) {
            val arrived = state.copy(remainingMeters = remaining, arrived = true)
            events += NavigationEvent.Announce("Anda telah tiba di ${target.name}.")
            events += NavigationEvent.Arrived(target)
            return arrived to events
        }

        updateHeading(loc)

        var next = state.copy(remainingMeters = remaining)
        val offRoute = Geo.crossTrackMeters(
            state.originLat, state.originLng, target.lat, target.lng, loc.lat, loc.lng,
        )
        if (offRoute > rerouteMeters) {
            next = next.copy(originLat = loc.lat, originLng = loc.lng, reroutedAtMillis = loc.timeMillis)
            events += NavigationEvent.Rerouted
            events += NavigationEvent.Announce("Rute dihitung ulang.")
            lastAnnouncedTurn = null
        }

        val turn = heading?.let { turnFor(it, Geo.bearingDegrees(loc.lat, loc.lng, target.lat, target.lng)) }
            ?: Turn.UNKNOWN
        next = next.copy(turn = turn)

        // Arahan baru diucapkan setelah stabil dua posisi berturut-turut, agar tidak "goyang".
        if (turn != Turn.UNKNOWN && turn != lastAnnouncedTurn) {
            if (pendingTurn == turn) {
                lastAnnouncedTurn = turn
                pendingTurn = null
                events += NavigationEvent.Announce(next.instruction + ".")
            } else {
                pendingTurn = turn
            }
        } else if (turn == lastAnnouncedTurn) {
            pendingTurn = null
        }

        val b = bucket(remaining)
        if (b != lastReminderBucket && b < (lastReminderBucket ?: Int.MAX_VALUE)) {
            lastReminderBucket = b
            if (events.none { it is NavigationEvent.Announce }) {
                events += NavigationEvent.Announce("Sisa ${next.roundedDistance()} menuju ${target.name}.")
            }
        }
        return next to events
    }

    fun reset() {
        lastHeadingFix = null
        heading = null
        lastAnnouncedTurn = null
        pendingTurn = null
        lastReminderBucket = null
    }

    private fun updateHeading(loc: UserLocation) {
        val prev = lastHeadingFix
        if (prev == null) {
            lastHeadingFix = loc
            return
        }
        if (Geo.distanceMeters(prev.lat, prev.lng, loc.lat, loc.lng) >= headingMinMeters) {
            heading = Geo.bearingDegrees(prev.lat, prev.lng, loc.lat, loc.lng)
            lastHeadingFix = loc
        }
    }

    private fun bucket(distance: Double): Int = (distance / reminderEveryMeters).toInt()

    companion object {
        /** Arahan dari selisih arah jalan dan arah ke tujuan (derajat, positif = kanan). */
        fun turnFor(heading: Double, bearingToTarget: Double): Turn {
            val diff = ((bearingToTarget - heading + 540) % 360) - 180
            val a = abs(diff)
            return when {
                a <= 20 -> Turn.STRAIGHT
                a <= 50 -> if (diff > 0) Turn.SLIGHT_RIGHT else Turn.SLIGHT_LEFT
                a <= 145 -> if (diff > 0) Turn.RIGHT else Turn.LEFT
                else -> Turn.U_TURN
            }
        }
    }
}
