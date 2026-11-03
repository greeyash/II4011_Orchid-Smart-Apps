package com.orchidapps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orchidapps.data.Poi
import com.orchidapps.data.PoiDataset
import com.orchidapps.data.PoiRepository
import com.orchidapps.location.GeofenceEngine
import com.orchidapps.location.GeofenceTransition
import com.orchidapps.location.LocationRepository
import com.orchidapps.location.UserLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DatasetState {
    data object Loading : DatasetState
    data class Loaded(val dataset: PoiDataset) : DatasetState
    data class Error(val message: String) : DatasetState
}

enum class PermissionStatus { NOT_REQUESTED, GRANTED, DENIED, PERMANENTLY_DENIED }

data class LocationUiState(
    val permission: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val locationEnabled: Boolean = true,
    val location: UserLocation? = null,
    /** True bila belum ada posisi 10 detik setelah pelacakan dimulai (target F01). */
    val noFixTimeout: Boolean = false,
    val error: String? = null,
) {
    val showWeakGps: Boolean get() = noFixTimeout || location?.isWeak == true
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val poiRepository = PoiRepository(app)
    private val locationRepository = LocationRepository(app)

    private val _dataset = MutableStateFlow<DatasetState>(DatasetState.Loading)
    val dataset: StateFlow<DatasetState> = _dataset.asStateFlow()

    private val _location = MutableStateFlow(
        LocationUiState(
            permission = if (locationRepository.hasPermission()) PermissionStatus.GRANTED
            else PermissionStatus.NOT_REQUESTED
        )
    )
    val location: StateFlow<LocationUiState> = _location.asStateFlow()

    private var trackingJob: Job? = null

    private val geofence = GeofenceEngine()

    /** POI yang baru saja dimasuki pengguna (PB05); null bila tidak ada atau sudah ditutup. */
    private val _arrival = MutableStateFlow<Poi?>(null)
    val arrival: StateFlow<Poi?> = _arrival.asStateFlow()

    private val _insidePoiIds = MutableStateFlow<Set<String>>(emptySet())
    val insidePoiIds: StateFlow<Set<String>> = _insidePoiIds.asStateFlow()

    init {
        loadDataset()
    }

    fun loadDataset() {
        _dataset.value = DatasetState.Loading
        viewModelScope.launch {
            _dataset.value = try {
                DatasetState.Loaded(poiRepository.loadDataset())
            } catch (e: Exception) {
                DatasetState.Error(e.message ?: "Data POI gagal dibaca")
            }
        }
    }

    fun poiById(id: String): Poi? =
        (_dataset.value as? DatasetState.Loaded)?.dataset?.pois?.firstOrNull { it.id == id }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        val status = when {
            granted -> PermissionStatus.GRANTED
            permanentlyDenied -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.DENIED
        }
        _location.update { it.copy(permission = status) }
    }

    /** Dipanggil saat layar kembali aktif, mis. setelah pengguna mengubah izin di Pengaturan. */
    fun refreshPermission() {
        if (locationRepository.hasPermission()) {
            _location.update { it.copy(permission = PermissionStatus.GRANTED) }
        } else if (_location.value.permission == PermissionStatus.GRANTED) {
            _location.update { it.copy(permission = PermissionStatus.DENIED) }
        }
    }

    fun startTracking() {
        if (trackingJob?.isActive == true) return
        if (!locationRepository.hasPermission()) return
        val enabled = locationRepository.isLocationEnabled()
        _location.update { it.copy(locationEnabled = enabled, noFixTimeout = false, error = null) }
        if (!enabled) return

        trackingJob = viewModelScope.launch {
            launch {
                delay(FIX_TIMEOUT_MS)
                if (_location.value.location == null) {
                    _location.update { it.copy(noFixTimeout = true) }
                }
            }
            launch {
                try {
                    locationRepository.currentLocation()?.let(::onNewLocation)
                } catch (e: Exception) {
                    _location.update { it.copy(error = e.message) }
                }
            }
            locationRepository.locationUpdates().collect { onNewLocation(it) }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    fun dismissArrival() {
        _arrival.value = null
    }

    private fun onNewLocation(loc: UserLocation) {
        _location.update { it.copy(location = loc, noFixTimeout = false, error = null) }

        val pois = (_dataset.value as? DatasetState.Loaded)?.dataset?.pois ?: return
        val events = geofence.update(loc, pois)
        _insidePoiIds.value = geofence.insidePoiIds
        events.filter { it.transition == GeofenceTransition.ENTER }
            .minByOrNull { it.distanceMeters }
            ?.let { _arrival.value = it.poi }
        // Tutup pemberitahuan bila pengguna sudah meninggalkan POI tersebut.
        val current = _arrival.value
        if (current != null && current.id !in geofence.insidePoiIds) _arrival.value = null
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 10_000L
    }
}
