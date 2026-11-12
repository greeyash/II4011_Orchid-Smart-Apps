package com.orchidapps.audio

import com.orchidapps.data.Poi

/**
 * Isi pemandu audio POI (PB06): lokasi rekaman/foto di assets dan teks narasi yang dibacakan
 * bila rekaman tim belum ada. Pemutarannya ditangani [NarrationPlayer].
 */
object AudioGuide {

    fun recordingPath(poi: Poi) = "audio/${poi.id}.mp3"
    fun photoPath(poi: Poi) = "images/${poi.id}.jpg"

    /** Teks yang dibacakan bila rekaman belum ada. Pengingat keselamatan didahulukan (PRD 4.1). */
    fun narration(poi: Poi): String = buildString {
        append("Anda tiba di ").append(poi.name).append(". ")
        poi.safetyNote?.let { append("Perhatian: ").append(it).append(' ') }
        append(poi.description).append(' ')
        append("Kegiatan yang bisa dilakukan: ")
        append(poi.activities.joinToString(". ")).append('.')
        poi.info?.let { append(' ').append(it) }
    }.trim()

    fun item(poi: Poi) = NarrationItem(
        id = poi.id,
        title = poi.name,
        text = narration(poi),
        recordingAsset = recordingPath(poi),
    )

    /** Kalimat singkat saat tiba di POI (F02, F04b). */
    fun arrivalAnnouncement(poi: Poi) = "Anda berada di ${poi.name}."
}
