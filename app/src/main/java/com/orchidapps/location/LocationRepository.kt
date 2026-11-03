package com.orchidapps.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class UserLocation(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float?,
    val timeMillis: Long,
) {
    /** Akurasi di atas ambang ini dianggap "GPS lemah" (F01). */
    val isWeak: Boolean get() = accuracyMeters == null || accuracyMeters > WEAK_ACCURACY_METERS

    companion object {
        const val WEAK_ACCURACY_METERS = 30f
    }
}

/**
 * Pembacaan lokasi melalui Fused Location Provider (PB02).
 * Pelacakan latar belakang (Foreground Service) dikerjakan di Sprint 3 (PB08).
 */
class LocationRepository(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): UserLocation? {
        if (!hasPermission()) return null
        val cts = CancellationTokenSource()
        return try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()?.toUserLocation()
                ?: client.lastLocation.await()?.toUserLocation()
        } finally {
            cts.cancel()
        }
    }

    /** Pembaruan posisi berkala selama layar aktif; berhenti saat collector dibatalkan. */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long = UPDATE_INTERVAL_MS): Flow<UserLocation> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toUserLocation()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun Location.toUserLocation() = UserLocation(
        lat = latitude,
        lng = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        timeMillis = time,
    )

    companion object {
        const val UPDATE_INTERVAL_MS = 3_000L
    }
}
