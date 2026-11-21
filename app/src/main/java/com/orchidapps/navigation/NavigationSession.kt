package com.orchidapps.navigation

import android.content.Context
import com.orchidapps.audio.NarrationPlayer
import com.orchidapps.data.Poi
import com.orchidapps.location.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Navigasi yang sedang berjalan (PB12), dibagi antara UI dan [com.orchidapps.exploration.ExplorationService].
 * Posisi baru diteruskan lewat [onLocation] dari pelacakan layar aktif maupun sesi jelajah, sehingga
 * arahan suara tetap terdengar saat layar mati selama sesi jelajah aktif.
 */
object NavigationSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = NavigationEngine()
    private var speakJob: Job? = null
    private var player: NarrationPlayer? = null

    private val _state = MutableStateFlow<NavigationState?>(null)
    val state: StateFlow<NavigationState?> = _state.asStateFlow()

    /** Jarak awal, untuk progress bar. */
    private val _startDistance = MutableStateFlow(0.0)
    val startDistance: StateFlow<Double> = _startDistance.asStateFlow()

    /** Dipanggil saat tiba lewat navigasi; dipakai untuk menampilkan rekomendasi berikutnya (PB13). */
    private val _arrivals = MutableStateFlow<Poi?>(null)
    val lastArrival: StateFlow<Poi?> = _arrivals.asStateFlow()

    fun start(context: Context, target: Poi, from: UserLocation) {
        player = NarrationPlayer.get(context)
        val (state, events) = engine.start(target, from)
        _startDistance.value = state.remainingMeters
        _state.value = state
        handle(events)
    }

    fun onLocation(loc: UserLocation) {
        val current = _state.value ?: return
        val (next, events) = engine.update(current, loc)
        _state.value = next
        handle(events)
    }

    fun stop() {
        engine.reset()
        speakJob?.cancel()
        _state.value = null
    }

    fun consumeArrival() {
        _arrivals.value = null
    }

    private fun handle(events: List<NavigationEvent>) {
        events.forEach { event ->
            when (event) {
                is NavigationEvent.Announce -> speak(event.text)
                is NavigationEvent.Arrived -> _arrivals.value = event.poi
                NavigationEvent.Rerouted -> Unit
            }
        }
    }

    /** Arahan navigasi lewat suara (F04e); arahan baru menggantikan yang belum selesai diucapkan. */
    private fun speak(text: String) {
        val speech = player?.speech ?: return
        speakJob?.cancel()
        speakJob = scope.launch { runCatching { speech.speak(text) } }
    }
}
