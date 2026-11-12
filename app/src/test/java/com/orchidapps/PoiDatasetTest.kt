package com.orchidapps

import com.orchidapps.data.PoiCategory
import com.orchidapps.data.PoiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PB03: dataset POI (PRD 4.1, P01-P17) dapat dibaca aplikasi. */
class PoiDatasetTest {

    private val dataset = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    )

    @Test
    fun containsAllPrdPois() {
        assertEquals((1..17).map { "P%02d".format(it) }, dataset.pois.map { it.id })
    }

    @Test
    fun woodBridgeRadiusFollowsPrd() {
        assertEquals(20, dataset.pois.first { it.id == "P01" }.radiusMeters)
    }

    @Test
    fun poisWithSafetyReminderInPrdHaveSafetyNote() {
        listOf("P01", "P02", "P06", "P07", "P08", "P11", "P12", "P14", "P15", "P16", "P17").forEach { id ->
            assertTrue(id, !dataset.pois.first { it.id == id }.safetyNote.isNullOrBlank())
        }
    }

    @Test
    fun everyPoiHasRequiredFields() {
        dataset.pois.forEach { poi ->
            assertTrue("${poi.id} radius", poi.radiusMeters in 20..40)
            assertTrue("${poi.id} deskripsi", poi.description.isNotBlank())
            assertTrue("${poi.id} kegiatan", poi.activities.isNotEmpty())
            assertTrue("${poi.id} sumber", poi.source.isNotBlank())
        }
    }

    @Test
    fun aspasiaCoffeeIsFoundByAliasKopi() {
        val match = dataset.pois.first { "kopi" in it.aliases }
        assertEquals("P03", match.id)
        assertEquals(PoiCategory.TEMPAT_MAKAN, match.category)
    }

    @Test
    fun prdSearchKeywordsMatchExpectedPoi() {
        mapOf("bridge" to "P01", "castle" to "P02", "aspasia" to "P03", "golf" to "P17", "kuda" to "P14")
            .forEach { (keyword, id) ->
                assertTrue(keyword, keyword in dataset.pois.first { it.id == id }.aliases)
            }
    }

    @Test
    fun foodPlacesShowMenuSection() {
        listOf("P03", "P05", "P10").forEach { id ->
            assertTrue(id, !dataset.pois.first { it.id == id }.infoTitle.isNullOrBlank())
        }
    }

    @Test
    fun woodBridgeHasSafetyNote() {
        assertTrue(dataset.pois.first { it.id == "P01" }.safetyNote!!.isNotBlank())
    }

    @Test(expected = IllegalArgumentException::class)
    fun radiusOutsideRangeIsRejected() {
        val raw = File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
            .replaceFirst("\"radiusMeters\": 20", "\"radiusMeters\": 100")
        PoiRepository.parseDataset(raw)
    }
}
