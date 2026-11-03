package com.orchidapps

import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoTest {

    @Test
    fun distanceToSamePointIsZero() {
        assertEquals(0.0, Geo.distanceMeters(-6.78, 107.63, -6.78, 107.63), 0.001)
    }

    @Test
    fun oneThousandthDegreeLatitudeIsAbout111Meters() {
        val d = Geo.distanceMeters(-6.780, 107.634, -6.781, 107.634)
        assertEquals(111.2, d, 0.5)
    }

    @Test
    fun nearestPoiPicksSmallestDistance() {
        val near = poi("P01", -6.7801, 107.6340)
        val far = poi("P02", -6.7850, 107.6400)
        val (result, _) = Geo.nearestPoi(-6.7800, 107.6340, listOf(far, near))!!
        assertEquals("P01", result.id)
    }

    @Test
    fun nearestPoiOfEmptyListIsNull() {
        assertNull(Geo.nearestPoi(-6.78, 107.63, emptyList()))
    }

    @Test
    fun walkingMinutesIsAtLeastOne() {
        assertEquals(1, Geo.walkingMinutes(10.0))
        assertEquals(2, Geo.walkingMinutes(150.0))
    }

    @Test
    fun circlePolygonIsClosedAndHasTheRightRadius() {
        val ring = Geo.circlePolygon(-6.7809, 107.6342, 30.0)
        assertEquals(ring.first(), ring.last())
        ring.forEach { (lat, lng) ->
            assertEquals(30.0, Geo.distanceMeters(-6.7809, 107.6342, lat, lng), 0.3)
        }
    }

    private fun poi(id: String, lat: Double, lng: Double) = Poi(
        id = id,
        qrCode = "ORCHID-$id",
        name = id,
        category = PoiCategory.WISATA_ALAM,
        lat = lat,
        lng = lng,
        radiusMeters = 30,
        shortDescription = "",
        description = "",
        activities = listOf("a"),
        source = "test",
    )
}
