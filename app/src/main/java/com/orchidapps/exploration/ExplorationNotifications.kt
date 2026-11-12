package com.orchidapps.exploration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.orchidapps.MainActivity
import com.orchidapps.R

/** Kanal dan isi notifikasi sesi jelajah. */
internal object ExplorationNotifications {

    const val CHANNEL_SESSION = "exploration_session"
    const val NOTIFICATION_SESSION = 1

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, "Sesi jelajah", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ditampilkan selama aplikasi memantau lokasi Anda di Orchid Forest."
                setShowBadge(false)
            }
        )
    }

    fun session(context: Context, visitedCount: Int): android.app.Notification =
        NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Jelajah Orchid Forest aktif")
            .setContentText(
                if (visitedCount == 0) "Kami akan memberi tahu saat Anda tiba di titik wisata."
                else "$visitedCount titik wisata sudah dikunjungi."
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent(context, poiId = null))
            .addAction(0, "Akhiri Jelajah", ExplorationService.actionIntent(context, ExplorationService.ACTION_STOP))
            .build()

    fun openAppIntent(context: Context, poiId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        poiId?.let { intent.putExtra(MainActivity.EXTRA_POI_ID, it) }
        return PendingIntent.getActivity(
            context,
            poiId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
