package com.orchidapps.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.orchidapps.data.Poi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Locale

enum class AudioStatus { IDLE, PREPARING, PLAYING, UNAVAILABLE }

data class AudioState(
    val status: AudioStatus = AudioStatus.IDLE,
    /** True bila yang diputar adalah deskripsi yang dibacakan suara HP, bukan rekaman. */
    val usesTts: Boolean = false,
    val message: String? = null,
)

/**
 * Pemandu audio POI (PB06).
 *
 * Memutar rekaman `assets/audio/<id POI>.mp3` bila ada. Selama rekaman belum dibuat,
 * deskripsi POI dibacakan oleh Text-to-Speech Bahasa Indonesia di perangkat (tanpa internet).
 * Kontrol di layar kunci menyusul di PB09.
 */
class AudioGuide(context: Context) {

    private val context = context.applicationContext
    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingText: String? = null

    fun hasRecording(poi: Poi): Boolean = assetExists(recordingPath(poi))

    fun play(poi: Poi) {
        stop()
        val path = recordingPath(poi)
        if (assetExists(path)) playRecording(path) else speak(narration(poi))
    }

    fun stop() {
        player?.run {
            stop()
            release()
        }
        player = null
        tts?.stop()
        pendingText = null
        if (_state.value.status != AudioStatus.UNAVAILABLE) _state.value = AudioState()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    private fun playRecording(path: String) {
        try {
            val mp = MediaPlayer()
            context.assets.openFd(path).use { fd ->
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            mp.setOnCompletionListener {
                it.release()
                player = null
                _state.value = AudioState()
            }
            mp.prepare()
            mp.start()
            player = mp
            _state.value = AudioState(AudioStatus.PLAYING, usesTts = false)
        } catch (e: IOException) {
            unavailable("Rekaman audio gagal diputar: ${e.message}")
        }
    }

    private fun speak(text: String) {
        val engine = tts
        if (engine == null) {
            pendingText = text
            _state.value = AudioState(AudioStatus.PREPARING, usesTts = true)
            tts = TextToSpeech(context, ::onTtsInit)
            return
        }
        if (!ttsReady) {
            pendingText = text
            return
        }
        startSpeaking(engine, text)
    }

    private fun onTtsInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            unavailable("Mesin suara (Text-to-Speech) tidak tersedia di HP ini.")
            return
        }
        val result = engine.setLanguage(INDONESIAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            unavailable(
                "Suara Bahasa Indonesia belum terpasang. Pasang lewat " +
                    "Setelan → Bahasa & masukan → Output text-to-speech."
            )
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                _state.value = AudioState()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = AudioState(AudioStatus.UNAVAILABLE, message = "Gagal membacakan deskripsi.")
            }
        })
        ttsReady = true
        pendingText?.let {
            pendingText = null
            startSpeaking(engine, it)
        }
    }

    private fun startSpeaking(engine: TextToSpeech, text: String) {
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        _state.value = AudioState(AudioStatus.PLAYING, usesTts = true)
    }

    /** Mesin suara dilepas agar tombol putar berikutnya mencoba inisialisasi ulang. */
    private fun unavailable(message: String) {
        tts?.shutdown()
        tts = null
        ttsReady = false
        pendingText = null
        _state.value = AudioState(AudioStatus.UNAVAILABLE, message = message)
    }

    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).close()
        true
    } catch (_: IOException) {
        false
    }

    companion object {
        private val INDONESIAN = Locale.forLanguageTag("id-ID")
        private const val UTTERANCE_ID = "poi-narration"

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
    }
}
