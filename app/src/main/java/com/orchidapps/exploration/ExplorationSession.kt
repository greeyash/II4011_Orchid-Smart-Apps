package com.orchidapps.exploration

import android.content.Context
import com.orchidapps.data.Poi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Status sesi jelajah yang dibagi antara [ExplorationService] dan UI (PB08).
 * Hidup selama proses aplikasi; sesi berakhir saat pengguna menekan "Akhiri Jelajah".
 */
object ExplorationSession {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    /** POI yang sudah dikunjungi pada sesi ini (dipakai rekomendasi F07 di Sprint 4). */
    private val _visited = MutableStateFlow<Set<String>>(emptySet())
    val visited: StateFlow<Set<String>> = _visited.asStateFlow()

    /** POI yang baru saja dimasuki; ditampilkan sebagai kartu kedatangan di aplikasi. */
    private val _arrival = MutableStateFlow<Poi?>(null)
    val arrival: StateFlow<Poi?> = _arrival.asStateFlow()

    private val _insidePoiIds = MutableStateFlow<Set<String>>(emptySet())
    val insidePoiIds: StateFlow<Set<String>> = _insidePoiIds.asStateFlow()

    /** Permintaan membuka halaman POI dari notifikasi (F03 AC4). */
    private val _openPoiRequest = MutableStateFlow<String?>(null)
    val openPoiRequest: StateFlow<String?> = _openPoiRequest.asStateFlow()

    internal fun onStarted() {
        _visited.value = emptySet()
        _arrival.value = null
        _insidePoiIds.value = emptySet()
        _active.value = true
    }

    internal fun onEnded() {
        _active.value = false
        _arrival.value = null
        _insidePoiIds.value = emptySet()
    }

    internal fun onArrived(poi: Poi) {
        _visited.update { it + poi.id }
        _arrival.value = poi
    }

    internal fun onInsideChanged(ids: Set<String>) {
        _insidePoiIds.value = ids
        val current = _arrival.value
        // Kartu kedatangan ditutup bila pengguna sudah meninggalkan POI tersebut.
        if (current != null && current.id !in ids) _arrival.value = null
    }

    fun dismissArrival() {
        _arrival.value = null
    }

    fun requestOpenPoi(id: String) {
        _openPoiRequest.value = id
    }

    fun consumeOpenPoiRequest() {
        _openPoiRequest.value = null
    }

    fun start(context: Context) = ExplorationService.start(context)

    fun end(context: Context) = ExplorationService.stop(context)
}
