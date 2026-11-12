package com.orchidapps.audio

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class NarrationStatus { IDLE, PREPARING, PLAYING, PAUSED, ENDED, ERROR }

/** Satu narasi yang bisa diputar: narasi POI atau narasi spesies anggrek. */
data class NarrationItem(
    val id: String,
    val title: String,
    val text: String,
    /** Rekaman tim di assets; bila belum ada, [text] dibacakan suara HP. */
    val recordingAsset: String? = null,
)

data class NarrationState(
    val item: NarrationItem? = null,
    val status: NarrationStatus = NarrationStatus.IDLE,
    val usesTts: Boolean = false,
    val message: String? = null,
) {
    val isActive: Boolean
        get() = item != null && status != NarrationStatus.IDLE
}

/**
 * Pemutar narasi untuk seluruh aplikasi (PB06, PB09).
 *
 * Narasi diputar lewat ExoPlayer yang terhubung ke MediaSession, sehingga bisa dijeda dan
 * dilanjutkan dari tombol earphone serta kontrol media di layar kunci. Bila rekaman tim belum
 * ada, teks narasi diubah menjadi file audio oleh Text-to-Speech lalu disimpan di cache.
 */
class NarrationPlayer private constructor(context: Context) {

    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val speech = Speech(this.context)

    private val player: ExoPlayer = ExoPlayer.Builder(this.context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    /** Sesi media: sumber kontrol layar kunci dan tombol earphone. */
    val session: MediaSession = MediaSession.Builder(this.context, player)
        .setId("orchid-narration")
        .build()

    private val _state = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = syncStatus()
            override fun onPlaybackStateChanged(playbackState: Int) = syncStatus()
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(status = NarrationStatus.ERROR, message = "Audio gagal diputar.") }
            }
        })
    }

    fun play(item: NarrationItem) {
        loadJob?.cancel()
        speech.stop()
        player.stop()
        val usesTts = item.recordingAsset == null || !assetExists(item.recordingAsset)
        _state.value = NarrationState(item, NarrationStatus.PREPARING, usesTts)
        loadJob = scope.launch {
            try {
                val uri = resolve(item, usesTts)
                player.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.id)
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(item.title)
                                .setArtist("Orchid Forest Cikole")
                                .build()
                        )
                        .build()
                )
                player.prepare()
                player.play()
            } catch (e: SpeechUnavailableException) {
                _state.value = NarrationState(item, NarrationStatus.ERROR, usesTts, e.message)
            } catch (e: IOException) {
                _state.value = NarrationState(item, NarrationStatus.ERROR, usesTts, "Audio gagal disiapkan.")
            }
        }
    }

    fun pause() = player.pause()

    fun resume() {
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    fun replay() {
        player.seekTo(0)
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else resume()
    }

    fun stop() {
        loadJob?.cancel()
        speech.stop()
        player.stop()
        player.clearMediaItems()
        _state.value = NarrationState()
    }

    /** Menyiapkan file suara narasi lebih awal agar Auto-Play bisa mulai ≤3 detik (F04). */
    fun prefetch(items: List<NarrationItem>) {
        scope.launch {
            items.filter { it.recordingAsset == null || !assetExists(it.recordingAsset) }
                .forEach { runCatching { ttsFile(it) } }
        }
    }

    private suspend fun resolve(item: NarrationItem, usesTts: Boolean): Uri =
        if (!usesTts) Uri.parse("asset:///${item.recordingAsset}") else Uri.fromFile(ttsFile(item))

    private suspend fun ttsFile(item: NarrationItem): File {
        val file = File(context.cacheDir, "narration/${item.id}-${item.text.hashCode().toUInt()}.wav")
        if (!withContext(Dispatchers.IO) { file.exists() }) speech.synthesizeToFile(item.text, file)
        return file
    }

    private fun syncStatus() {
        val current = _state.value
        if (current.item == null || current.status == NarrationStatus.ERROR) return
        val status = when {
            player.isPlaying -> NarrationStatus.PLAYING
            player.playbackState == Player.STATE_ENDED -> NarrationStatus.ENDED
            player.playbackState == Player.STATE_READY -> NarrationStatus.PAUSED
            player.playbackState == Player.STATE_BUFFERING -> NarrationStatus.PREPARING
            else -> current.status
        }
        _state.value = current.copy(status = status)
    }

    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).close()
        true
    } catch (_: IOException) {
        false
    }

    companion object {
        // Hanya menyimpan applicationContext, jadi tidak menahan Activity.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: NarrationPlayer? = null

        /** Harus dipanggil dari main thread. */
        fun get(context: Context): NarrationPlayer =
            instance ?: synchronized(this) {
                instance ?: NarrationPlayer(context).also { instance = it }
            }
    }
}
