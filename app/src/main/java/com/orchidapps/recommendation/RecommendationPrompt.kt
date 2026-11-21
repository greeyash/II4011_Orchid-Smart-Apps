package com.orchidapps.recommendation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Permintaan menampilkan "Lanjut ke Mana?" (PB13): diisi saat pengunjung menutup halaman POI,
 * berisi ID POI yang baru ditutup agar tidak ikut direkomendasikan.
 */
object RecommendationPrompt {
    private val _anchor = MutableStateFlow<String?>(null)
    val anchor: StateFlow<String?> = _anchor.asStateFlow()

    fun show(afterPoiId: String) {
        _anchor.value = afterPoiId
    }

    fun dismiss() {
        _anchor.value = null
    }
}
