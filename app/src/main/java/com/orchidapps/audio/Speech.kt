package com.orchidapps.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpeechUnavailableException(message: String) : Exception(message)

/**
 * Text-to-Speech Bahasa Indonesia di perangkat (tanpa internet), dipakai bersama oleh
 * pengumuman kedatangan (PB07) dan pembuatan file narasi (PB09).
 */
class Speech(context: Context) {

    private val context = context.applicationContext
    private val initLock = Mutex()
    private var tts: TextToSpeech? = null
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    /** Membacakan [text] langsung dan menunggu sampai selesai. */
    suspend fun speak(text: String) {
        val engine = engine()
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Boolean>().also { pending[id] = it }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        done.await()
    }

    /** Menyimpan [text] sebagai file audio agar bisa diputar dengan pemutar media. */
    suspend fun synthesizeToFile(text: String, file: File) {
        val engine = engine()
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Boolean>().also { pending[id] = it }
        val result = engine.synthesizeToFile(text, Bundle(), tmp, id)
        if (result != TextToSpeech.SUCCESS) {
            pending.remove(id)
            throw SpeechUnavailableException("Gagal menyiapkan suara narasi.")
        }
        if (!done.await() || !tmp.renameTo(file)) {
            tmp.delete()
            throw SpeechUnavailableException("Gagal menyiapkan suara narasi.")
        }
    }

    fun stop() {
        tts?.stop()
    }

    private suspend fun engine(): TextToSpeech = initLock.withLock {
        tts?.let { return it }
        val ready = CompletableDeferred<Int>()
        val engine = TextToSpeech(context) { status -> ready.complete(status) }
        if (ready.await() != TextToSpeech.SUCCESS) {
            engine.shutdown()
            throw SpeechUnavailableException("Mesin suara (Text-to-Speech) tidak tersedia di HP ini.")
        }
        val lang = engine.setLanguage(INDONESIAN)
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.shutdown()
            throw SpeechUnavailableException(
                "Suara Bahasa Indonesia belum terpasang. Pasang lewat " +
                    "Setelan → Bahasa & masukan → Output text-to-speech."
            )
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { pending.remove(it)?.complete(true) }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                utteranceId?.let { pending.remove(it)?.complete(false) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { pending.remove(it)?.complete(false) }
            }
        })
        tts = engine
        engine
    }

    companion object {
        private val INDONESIAN = Locale.forLanguageTag("id-ID")
    }
}
