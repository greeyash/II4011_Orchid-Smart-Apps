package com.orchidapps

import com.orchidapps.data.PoiRepository
import com.orchidapps.recommendation.Recommender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PB13 / F07: 1-3 POI belum dikunjungi, terdekat lebih dulu, dengan alasan singkat. */
class RecommenderTest {

    private val pois = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    ).pois
    private val woodBridge = pois.first { it.id == "P01" }

    @Test
    fun recommendsAtMostThreeUnvisitedPoisSortedByDistance() {
        val recs = Recommender.recommend(pois, visited = setOf("P02"), woodBridge.lat, woodBridge.lng, exclude = setOf("P01"))
        assertTrue(recs.size in 1..3)
        assertTrue(recs.none { it.poi.id == "P01" || it.poi.id == "P02" })
        assertEquals(recs.map { it.distanceMeters }.sorted(), recs.map { it.distanceMeters })
        assertTrue(recs.all { it.reason.isNotBlank() })
    }

    @Test
    fun emptyWhenEverythingVisited() {
        val recs = Recommender.recommend(pois, visited = pois.map { it.id }.toSet(), woodBridge.lat, woodBridge.lng)
        assertTrue(recs.isEmpty())
        assertTrue(Recommender.spoken(recs).contains("sudah Anda jelajahi"))
    }

    @Test
    fun reasonMentionsActivity() {
        val orchidHouse = pois.first { it.id == "P04" }
        assertEquals("banyak jenis anggrek", Recommender.activityHint(orchidHouse))
        val aspasia = pois.first { it.id == "P03" }
        assertEquals("cocok untuk makan dan istirahat", Recommender.activityHint(aspasia))
    }
}
