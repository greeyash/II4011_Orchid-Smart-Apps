package com.orchidapps.exploration

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pengaturan jelajah yang disimpan di perangkat. */
class ExplorationSettings private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("exploration", Context.MODE_PRIVATE)

    /** Auto-Play (F04c): narasi langsung diputar setelah nama lokasi disebutkan. */
    private val _autoPlay = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PLAY, true))
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    fun setAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
        _autoPlay.value = enabled
    }

    companion object {
        private const val KEY_AUTO_PLAY = "auto_play"

        @Volatile
        private var instance: ExplorationSettings? = null

        fun get(context: Context): ExplorationSettings =
            instance ?: synchronized(this) {
                instance ?: ExplorationSettings(context).also { instance = it }
            }
    }
}
