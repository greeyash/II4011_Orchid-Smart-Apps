package com.orchidapps

import com.orchidapps.audio.AudioGuide
import com.orchidapps.data.PoiRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PRD 4.1: pengingat keselamatan dibacakan sebelum narasi. */
class NarrationTest {

    private val pois = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    ).pois

    @Test
    fun safetyNoteIsReadBeforeDescription() {
        pois.filter { it.safetyNote != null }.forEach { poi ->
            val text = AudioGuide.narration(poi)
            assertTrue(poi.id, text.indexOf(poi.safetyNote!!) < text.indexOf(poi.description))
        }
    }

    @Test
    fun narrationMentionsPoiName() {
        pois.forEach { assertTrue(it.id, AudioGuide.narration(it).contains(it.name)) }
    }
}
