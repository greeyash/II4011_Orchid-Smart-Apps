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

    @Test
    fun orchidHouseNarrationListsSpecies() {
        val orchidHouse = pois.first { it.id == "P04" }
        val text = AudioGuide.narration(orchidHouse)
        orchidHouse.species.forEach { assertTrue(it.name, text.contains(it.name)) }
    }

    @Test
    fun speciesNarrationHasNameCharacteristicsAndOrigin() {
        val orchidHouse = pois.first { it.id == "P04" }
        orchidHouse.species.forEach {
            val text = AudioGuide.speciesNarration(it)
            assertTrue(it.id, text.contains(it.latinName) && text.contains(it.characteristics) && text.contains(it.origin))
        }
    }
}
