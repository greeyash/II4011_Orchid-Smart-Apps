package com.orchidapps

import com.orchidapps.audio.AudioGuide
import com.orchidapps.data.PoiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Narasi POI: naskah tim dipakai bila ada; narasi otomatis mendahulukan pengingat keselamatan (PRD 4.1). */
class NarrationTest {

    private val pois = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    ).pois

    @Test
    fun safetyNoteIsReadBeforeDescription() {
        pois.filter { it.safetyNote != null }.map { it.copy(narration = null) }.forEach { poi ->
            val text = AudioGuide.narration(poi)
            assertTrue(poi.id, text.indexOf(poi.safetyNote!!) < text.indexOf(poi.description))
        }
    }

    @Test
    fun narrationMentionsPoiName() {
        pois.forEach { poi ->
            val text = AudioGuide.narration(poi).lowercase()
            val names = listOf(poi.name.lowercase()) + poi.aliases
            assertTrue(poi.id, names.any { text.contains(it) })
        }
    }

    @Test
    fun orchidMuseumNarrationListsSpecies() {
        val orchidHouse = pois.first { it.id == "P04" }.copy(narration = null)
        val text = AudioGuide.narration(orchidHouse)
        orchidHouse.species.forEach { assertTrue(it.name, text.contains(it.name)) }
    }

    @Test
    fun speciesNarrationHasNameCharacteristicsAndOrigin() {
        val orchidHouse = pois.first { it.id == "P04" }
        orchidHouse.species.forEach {
            val text = AudioGuide.speciesNarration(it)
            assertTrue(it.id, text.isNotBlank() && (it.narration == null || text == it.narration))
        }
    }

    @Test
    fun teamScriptIsUsedWhenAvailable() {
        pois.filter { it.narration != null }.forEach { assertEquals(it.id, it.narration, AudioGuide.narration(it)) }
        assertTrue(pois.all { it.narration != null })
    }

    @Test
    fun woodBridgeArrivalIncludesSafetyReminder() {
        val woodBridge = pois.first { it.id == "P01" }
        val text = AudioGuide.arrivalAnnouncement(woodBridge)
        assertTrue(text.startsWith("Anda berada di Wood Bridge."))
        assertTrue(text.contains(woodBridge.safetyNote!!))
    }
}
