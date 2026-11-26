package com.orchidapps

import com.orchidapps.data.PoiRepository
import com.orchidapps.qr.QrCodes
import com.orchidapps.qr.QrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PB14 / A02: kode QR papan POI dan papan anggrek (4 jenis per papan) dikenali; kode asing ditolak. */
class QrCodesTest {

    private val pois = PoiRepository.parseDataset(
        File("src/main/assets/${PoiRepository.ASSET_FILE}").readText()
    ).pois

    @Test
    fun everyPoiQrOpensTheRightPoi() {
        pois.forEach { poi ->
            val result = QrCodes.resolve(poi.qrCode, pois)
            assertEquals(poi.id, (result as QrResult.PoiCode).poi.id)
        }
    }

    @Test
    fun speciesQrOpensTheRightSpecies() {
        val result = QrCodes.resolve(" orchid-p04-s01 ", pois)
        result as QrResult.SpeciesCode
        assertEquals("P04", result.poi.id)
        assertEquals("S01", result.species.id)
    }

    @Test
    fun boardQrListsTheFourOrchidsOnThatBoard() {
        val result = QrCodes.resolve("ORCHID-P04-B3", pois)
        result as QrResult.BoardCode
        assertEquals("P04", result.poi.id)
        assertEquals(listOf("S24", "S25", "S26", "S27"), result.poi.speciesOn(result.board).map { it.id })
    }

    @Test
    fun foreignQrIsUnknown() {
        assertTrue(QrCodes.resolve("https://example.com", pois) is QrResult.Unknown)
    }
}
