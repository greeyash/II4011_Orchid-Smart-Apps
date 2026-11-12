package com.orchidapps

import com.orchidapps.data.PoiCategory
import com.orchidapps.data.PoiRepository
import com.orchidapps.search.PoiSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PB10 / F05: kata kunci uji PRD, filter kategori, urutan terdekat, dan toleransi salah ketik. */
class PoiSearchTest {

    private val pois = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    ).pois

    private fun ids(query: String, category: PoiCategory? = null) =
        PoiSearch.search(pois, query, category).map { it.poi.id }

    @Test
    fun prdKeywordsFindExpectedPoi() {
        mapOf(
            "bridge" to "P01",
            "castle" to "P02",
            "aspasia" to "P03",
            "kopi" to "P03",
            "museum" to "P18",
            "anggrek" to "P18",
        ).forEach { (keyword, id) -> assertTrue("$keyword → $id", id in ids(keyword)) }
    }

    @Test
    fun searchIsCaseInsensitiveAndAcceptsPartialNames() {
        assertEquals("P01", ids("WOOD").first())
        assertTrue("P03" in ids("Aspa"))
        assertTrue("P17" in ids("putt"))
    }

    @Test
    fun searchToleratesLightTypos() {
        assertTrue("P01" in ids("brigde"))
        assertTrue("P03" in ids("aspasa"))
        assertTrue("P02" in ids("castel"))
    }

    @Test
    fun categoryFilterOnlyReturnsThatCategory() {
        val results = PoiSearch.search(pois, "", PoiCategory.TEMPAT_MAKAN)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.poi.category == PoiCategory.TEMPAT_MAKAN })
    }

    @Test
    fun resultsAreSortedByDistanceWithEta() {
        val woodBridge = pois.first { it.id == "P01" }
        val results = PoiSearch.search(pois, "", null, woodBridge.lat, woodBridge.lng)
        val distances = results.map { it.distanceMeters!! }
        assertEquals(distances.sorted(), distances)
        assertTrue(results.all { it.walkingMinutes!! >= 1 })
    }

    @Test
    fun unknownKeywordReturnsNothing() {
        assertTrue(ids("helikopter").isEmpty())
    }

    @Test
    fun editDistanceCountsTransposition() {
        assertEquals(1, PoiSearch.editDistance("brigde", "bridge"))
    }
}
