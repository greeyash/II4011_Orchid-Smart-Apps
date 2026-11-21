package com.orchidapps

import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.UserLocation
import com.orchidapps.navigation.NavigationEngine
import com.orchidapps.navigation.NavigationEvent
import com.orchidapps.navigation.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PB12 / F06: sisa jarak, arahan belokan, hitung ulang rute, dan pengumuman tiba. */
class NavigationEngineTest {

    // 0,0001° lintang ≈ 11 m. Tujuan 220 m di utara titik awal.
    private val start = UserLocation(-6.7810, 107.6370, 5f, 0)
    private val target = Poi(
        id = "P01", qrCode = "ORCHID-P01", name = "Wood Bridge",
        category = PoiCategory.WISATA_ALAM, lat = -6.7790, lng = 107.6370, radiusMeters = 20,
        shortDescription = "", description = "", activities = listOf("a"), source = "test",
    )

    private fun at(lat: Double, lng: Double, t: Long) = UserLocation(lat, lng, 5f, t)

    @Test
    fun remainingDistanceDecreasesWhileWalkingTowardTarget() {
        val engine = NavigationEngine()
        var (state, _) = engine.start(target, start)
        val first = state.remainingMeters
        state = engine.update(state, at(-6.7805, 107.6370, 1000)).first
        state = engine.update(state, at(-6.7800, 107.6370, 2000)).first
        assertTrue(state.remainingMeters < first)
        assertTrue(state.etaMinutes >= 1)
        assertEquals(Turn.STRAIGHT, state.turn)
    }

    @Test
    fun arrivalIsAnnouncedWithin15Meters() {
        val engine = NavigationEngine()
        val (state, _) = engine.start(target, start)
        val (arrived, events) = engine.update(state, at(-6.7791, 107.6370, 5000))
        assertTrue(arrived.arrived)
        assertTrue(events.any { it is NavigationEvent.Arrived })
    }

    @Test
    fun routeIsRecalculatedAfterDeviatingMoreThan30Meters() {
        val engine = NavigationEngine()
        val (state, _) = engine.start(target, start)
        // 0,0004° bujur ≈ 44 m ke timur dari garis rute.
        val (next, events) = engine.update(state, at(-6.7805, 107.6374, 3000))
        assertTrue(events.contains(NavigationEvent.Rerouted))
        assertEquals(-6.7805, next.originLat, 1e-9)
    }

    @Test
    fun smallDeviationDoesNotReroute() {
        val engine = NavigationEngine()
        val (state, _) = engine.start(target, start)
        val (_, events) = engine.update(state, at(-6.7805, 107.6371, 3000))
        assertFalse(events.contains(NavigationEvent.Rerouted))
    }

    @Test
    fun turnDirectionFollowsBearingDifference() {
        assertEquals(Turn.STRAIGHT, NavigationEngine.turnFor(0.0, 10.0))
        assertEquals(Turn.RIGHT, NavigationEngine.turnFor(0.0, 90.0))
        assertEquals(Turn.LEFT, NavigationEngine.turnFor(0.0, 270.0))
        assertEquals(Turn.U_TURN, NavigationEngine.turnFor(0.0, 180.0))
        assertEquals(Turn.SLIGHT_LEFT, NavigationEngine.turnFor(350.0, 315.0))
    }
}
