package com.orchidapps.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PoiCategory(val label: String) {
    @SerialName("WISATA_ALAM") WISATA_ALAM("Wisata Alam"),
    @SerialName("WISATA_TEMPAT") WISATA_TEMPAT("Wisata Tempat"),
    @SerialName("TEMPAT_MAKAN") TEMPAT_MAKAN("Tempat Makan"),
}

@Serializable
data class Poi(
    val id: String,
    val qrCode: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: PoiCategory,
    val lat: Double,
    val lng: Double,
    /** Radius geofence dalam meter; PRD menetapkan rentang 20-40 m. */
    val radiusMeters: Int,
    /** False selama koordinat belum dicek langsung di lapangan. */
    val coordinateVerified: Boolean = false,
    val shortDescription: String,
    val description: String,
    val activities: List<String>,
    /** Pengingat keselamatan; dibacakan sebelum narasi (PRD 4.1). */
    val safetyNote: String? = null,
    /** Judul bagian informasi khusus POI, mis. "Informasi botani" atau "Menu dan paket piknik". */
    val infoTitle: String? = null,
    /** Isi bagian informasi; null bila datanya belum dikumpulkan. */
    val info: String? = null,
    val source: String,
    /** Tanggal pengecekan ke pengelola (A03), format yyyy-MM-dd. */
    val checkedAt: String? = null,
)

@Serializable
data class PoiArea(
    val name: String,
    val plusCode: String,
    val centerLat: Double,
    val centerLng: Double,
)

@Serializable
data class PoiDataset(
    val version: Int,
    val area: PoiArea,
    val pois: List<Poi>,
)
