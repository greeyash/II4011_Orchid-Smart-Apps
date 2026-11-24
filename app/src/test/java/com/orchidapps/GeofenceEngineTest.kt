package com.orchidapps

import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.GeofenceEngine
import com.orchidapps.location.GeofenceTransition
import com.orchidapps.location.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** PB05: masuk/keluar radius POI tanpa pemicu palsu. */
class GeofenceEngineTest {

    private val poi = Poi(
        id = "P01",
        qrCode = "ORCHID-P01",
        name = "Wood Bridge",
        category = PoiCategory.WISATA_ALAM,
        lat = -6.780900,
        lng = 107.634200,
        radiusMeters = 30,
        shortDescription = "",
        description = "",
        activities = listOf("a"),
        source = "test",
    )
    private val pois = listOf(poi)

    // 0,0001° lintang ≈ 11 m.
    private fun at(metersNorth: Double, timeMillis: Long, accuracy: Float? = 10f) = UserLocation(
        lat = poi.lat + metersNorth / 111_195.0,
        lng = poi.lng,
        accuracyMeters = accuracy,
        timeMillis = timeMillis,
    )

    @Test
    fun singleFixInsideDoesNotTrigger() {
        val engine = GeofenceEngine()
        assertTrue(engine.update(at(5.0, 0), pois).isEmpty())
    }

    @Test
    fun twoFixesInsideTriggerEnterOnce() {
        val engine = GeofenceEngine()
        engine.update(at(5.0, 0), pois)
        val events = engine.update(at(5.0, 3_000), pois)
        assertEquals(listOf(GeofenceTransition.ENTER), events.map { it.transition })
        assertTrue(engine.update(at(5.0, 6_000), pois).isEmpty())
        assertEquals(setOf("P01"), engine.insidePoiIds)
    }

    @Test
    fun fixOutsideResetsTheCount() {
        val engine = GeofenceEngine()
        engine.update(at(5.0, 0), pois)
        engine.update(at(80.0, 3_000), pois)
        assertTrue(engine.update(at(5.0, 6_000), pois).isEmpty())
    }

    @Test
    fun inaccurateFixesAreIgnored() {
        val engine = GeofenceEngine()
        engine.update(at(5.0, 0, accuracy = 60f), pois)
        assertTrue(engine.update(at(5.0, 3_000, accuracy = null), pois).isEmpty())
    }

    @Test
    fun exitNeedsBufferBeyondRadius() {
        val engine = enteredEngine()
        // 35 m: di luar radius 30 m, tetapi masih dalam buffer 10 m.
        assertTrue(engine.update(at(35.0, 9_000), pois).isEmpty())
        val events = engine.update(at(45.0, 12_000), pois)
        assertEquals(listOf(GeofenceTransition.EXIT), events.map { it.transition })
        assertTrue(engine.insidePoiIds.isEmpty())
    }

    @Test
    fun reEnterWithinCooldownIsSilent() {
        val engine = enteredEngine()
        engine.update(at(80.0, 60_000), pois)
        engine.update(at(5.0, 120_000), pois)
        assertTrue(engine.update(at(5.0, 123_000), pois).isEmpty())
        assertEquals(setOf("P01"), engine.insidePoiIds)
    }

    @Test
    fun reEnterAfterCooldownTriggersAgain() {
        val engine = enteredEngine()
        engine.update(at(80.0, 60_000), pois)
        val later = 3_000L + 10 * 60_000L
        engine.update(at(5.0, later), pois)
        val events = engine.update(at(5.0, later + 3_000), pois)
        assertEquals(listOf(GeofenceTransition.ENTER), events.map { it.transition })
    }

    private fun enteredEngine() = GeofenceEngine().apply {
        update(at(5.0, 0), pois)
        update(at(5.0, 3_000), pois)
    }

    // Dua POI berdekatan: B 12 m di utara A, radius masing-masing 30 m (tumpang tindih).
    private val poiB = poi.copy(id = "P02", qrCode = "ORCHID-P02", name = "POI B", lat = poi.lat + 12.0 / 111_195.0)
    private val overlapping = listOf(poi, poiB)

    @Test
    fun overlappingPoisOnlyTriggerTheNearestOne() {
        val engine = GeofenceEngine()
        // 3 m dari A, 9 m dari B: keduanya dalam radius, tetapi hanya A (terdekat) yang terpicu.
        engine.update(at(3.0, 0), overlapping)
        val events = engine.update(at(3.0, 3_000), overlapping)
        assertEquals(listOf("P01"), events.filter { it.transition == GeofenceTransition.ENTER }.map { it.poi.id })
        assertEquals(setOf("P01"), engine.insidePoiIds)
    }

    @Test
    fun nextOverlappingPoiWaitsForTheGapInsteadOfStacking() {
        val engine = GeofenceEngine()
        engine.update(at(0.0, 0), overlapping)
        engine.update(at(0.0, 3_000), overlapping) // ENTER A
        // Berjalan ke B (12 m utara): B kini jelas lebih dekat, tetapi baru 9 detik sejak A.
        engine.update(at(12.0, 6_000), overlapping)
        val tooSoon = engine.update(at(12.0, 9_000), overlapping)
        assertTrue(tooSoon.none { it.transition == GeofenceTransition.ENTER })
        // Setelah jeda 30 detik dan masih di B, kedatangan B baru diumumkan.
        val later = engine.update(at(12.0, 33_000), overlapping)
        assertEquals(listOf("P02"), later.filter { it.transition == GeofenceTransition.ENTER }.map { it.poi.id })
    }

    @Test
    fun smallMovesAtTheBorderDoNotFlipBetweenPois() {
        val engine = GeofenceEngine()
        engine.update(at(5.0, 0), overlapping)
        engine.update(at(5.0, 3_000), overlapping) // ENTER A (5 m dari A, 7 m dari B)
        // 7 m dari A, 5 m dari B: B lebih dekat tetapi selisihnya < 5 m, tetap di A.
        assertTrue(engine.update(at(7.0, 40_000), overlapping).isEmpty())
        assertEquals(setOf("P01"), engine.insidePoiIds)
    }
}
