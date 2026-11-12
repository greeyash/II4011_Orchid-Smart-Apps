package com.orchidapps.exploration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal object Haptics {

    /** Getar dua kali saat tiba di POI (F02, F04a). */
    private val PATTERN = longArrayOf(0, 250, 150, 250)

    fun vibrateTwice(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, -1))
    }
}
