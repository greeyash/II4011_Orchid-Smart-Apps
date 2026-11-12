package com.orchidapps.exploration

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.orchidapps.data.Poi
import com.orchidapps.data.PoiRepository
import com.orchidapps.location.GeofenceEngine
import com.orchidapps.location.GeofenceTransition
import com.orchidapps.location.LocationRepository
import com.orchidapps.location.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Sesi jelajah (PB08): Foreground Service yang membaca lokasi setiap beberapa detik, juga saat
 * layar mati, lalu menjalankan geofence ke seluruh POI. Berhenti saat "Akhiri Jelajah".
 */
class ExplorationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var locationRepository: LocationRepository
    private val geofence = GeofenceEngine()
    private var pois: List<Poi> = emptyList()
    private var trackingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository(this)
        ExplorationNotifications.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                endSession()
                return START_NOT_STICKY
            }
            else -> startSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession() {
        if (trackingJob?.isActive == true) return
        if (!locationRepository.hasPermission()) {
            stopSelf()
            return
        }
        ServiceCompat.startForeground(
            this,
            ExplorationNotifications.NOTIFICATION_SESSION,
            ExplorationNotifications.session(this, visitedCount = 0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        geofence.reset()
        ExplorationSession.onStarted()
        trackingJob = scope.launch {
            pois = PoiRepository(this@ExplorationService).loadDataset().pois
            onSessionReady(pois)
            locationRepository.locationUpdates().collect(::onLocation)
        }
    }

    /** Titik perluasan untuk persiapan saat data POI sudah dimuat. */
    private fun onSessionReady(pois: List<Poi>) = Unit

    private fun onLocation(location: UserLocation) {
        val events = geofence.update(location, pois)
        ExplorationSession.onInsideChanged(geofence.insidePoiIds)
        // Bila dua POI berdekatan terpicu bersamaan, POI terdekat didahulukan (F02).
        events.filter { it.transition == GeofenceTransition.ENTER }
            .minByOrNull { it.distanceMeters }
            ?.let { onArrived(it.poi) }
    }

    private fun onArrived(poi: Poi) {
        ExplorationSession.onArrived(poi)
        updateSessionNotification()
    }

    private fun updateSessionNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this).notify(
                ExplorationNotifications.NOTIFICATION_SESSION,
                ExplorationNotifications.session(this, ExplorationSession.visited.value.size),
            )
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut; sesi tetap berjalan.
        }
    }

    private fun endSession() {
        trackingJob?.cancel()
        trackingJob = null
        geofence.reset()
        ExplorationSession.onEnded()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (ExplorationSession.active.value) ExplorationSession.onEnded()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.orchidapps.exploration.START"
        const val ACTION_STOP = "com.orchidapps.exploration.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ExplorationService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            if (!ExplorationSession.active.value) return
            context.startService(Intent(context, ExplorationService::class.java).setAction(ACTION_STOP))
        }

        internal fun actionIntent(context: Context, action: String, poiId: String? = null): PendingIntent {
            val intent = Intent(context, ExplorationService::class.java).setAction(action)
            poiId?.let { intent.putExtra(EXTRA_POI_ID, it) }
            return PendingIntent.getService(
                context,
                (action + poiId).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal const val EXTRA_POI_ID = "poi_id"
    }
}
