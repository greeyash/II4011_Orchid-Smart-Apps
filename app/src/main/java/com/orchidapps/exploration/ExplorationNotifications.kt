package com.orchidapps.exploration

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.orchidapps.MainActivity
import com.orchidapps.R
import com.orchidapps.audio.NarrationState
import com.orchidapps.audio.NarrationStatus
import com.orchidapps.data.Poi

/** Kanal dan isi notifikasi sesi jelajah. */
internal object ExplorationNotifications {

    const val CHANNEL_SESSION = "exploration_session"
    const val NOTIFICATION_SESSION = 1
    const val CHANNEL_ARRIVAL = "poi_arrival"
    const val NOTIFICATION_ARRIVAL = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, "Sesi jelajah", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ditampilkan selama aplikasi memantau lokasi Anda di Orchid Forest."
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ARRIVAL, "Tiba di titik wisata", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pemberitahuan \"Anda berada di …\" saat memasuki area sebuah POI."
                // Getar 2x dan suara nama lokasi diatur sendiri oleh aplikasi (F02, F04).
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Notifikasi kedatangan dengan tombol Dengarkan/Lewati (PB07, F04d). */
    fun arrival(context: Context, poi: Poi): Notification =
        NotificationCompat.Builder(context, CHANNEL_ARRIVAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Anda berada di ${poi.name}")
            .setContentText(poi.shortDescription)
            .setStyle(NotificationCompat.BigTextStyle().bigText(poi.shortDescription))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, poi.id))
            .addAction(
                0, "Dengarkan",
                ExplorationService.actionIntent(context, ExplorationService.ACTION_LISTEN, poi.id),
            )
            .addAction(
                0, "Lewati",
                ExplorationService.actionIntent(context, ExplorationService.ACTION_SKIP, poi.id),
            )
            .build()

    /**
     * Notifikasi sesi jelajah. Selama narasi aktif, notifikasi ini memakai MediaStyle yang terhubung
     * ke sesi media sehingga narasi bisa dijeda/dilanjutkan dari layar kunci (PB09, F04d).
     */
    @OptIn(UnstableApi::class)
    fun session(
        context: Context,
        visitedCount: Int,
        narration: NarrationState?,
        session: MediaSession,
    ): Notification {
        val visitedText = if (visitedCount == 0) "Kami akan memberi tahu saat Anda tiba di titik wisata."
        else "$visitedCount titik wisata sudah dikunjungi."
        val builder = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent(context, poiId = null))
        val item = narration?.item
        if (item == null) {
            return builder
                .setContentTitle("Jelajah Orchid Forest aktif")
                .setContentText(visitedText)
                .addAction(0, "Akhiri Jelajah", ExplorationService.actionIntent(context, ExplorationService.ACTION_STOP))
                .build()
        }
        val playing = narration.status == NarrationStatus.PLAYING
        return builder
            .setContentTitle(item.title)
            .setContentText("Narasi Orchid Forest · $visitedText")
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Jeda" else "Lanjutkan",
                ExplorationService.actionIntent(context, ExplorationService.ACTION_TOGGLE_PLAY),
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Ulang",
                ExplorationService.actionIntent(context, ExplorationService.ACTION_REPLAY),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Akhiri Jelajah",
                ExplorationService.actionIntent(context, ExplorationService.ACTION_STOP),
            )
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session).setShowActionsInCompactView(0, 1))
            .build()
    }

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
