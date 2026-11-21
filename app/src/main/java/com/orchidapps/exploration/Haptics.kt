package com.orchidapps.exploration

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal object Haptics {

    /** Getar dua kali saat tiba di POI (F02, F04a). */
    private val PATTERN = longArrayOf(0, 300, 200, 300)

    /**
     * Getar dua kali. Getaran diberi kategori "alarm" karena dipicu dari Foreground Service saat
     * layar mati: tanpa kategori, Android mengabaikan getaran aplikasi di latar belakang, dan
     * kategori notifikasi ikut dimatikan saat HP dalam mode senyap. Pengunjung sudah memilih
     * "Mulai Jelajah", jadi getar kedatangan tetap terasa walau HP disenyapkan.
     */
    fun vibrateTwice(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }
}
