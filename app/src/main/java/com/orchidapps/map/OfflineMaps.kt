package com.orchidapps.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.orchidapps.data.Poi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

sealed interface OfflineMapState {
    data object NotDownloaded : OfflineMapState
    data class Downloading(val progress: Float) : OfflineMapState
    data object Ready : OfflineMapState
    data class Failed(val message: String) : OfflineMapState
}

/**
 * Peta offline kawasan Orchid Forest (PB15, A01). Saat ada internet (mis. di loket), area seluruh
 * POI diunduh sekali ke penyimpanan MapLibre; setelah itu peta dasar tetap tampil dalam mode
 * pesawat karena gaya, ubin, dan huruf peta diambil dari database lokal.
 */
object OfflineMaps {

    /** Gaya peta dasar OpenFreeMap (gratis, tanpa API key); dipakai juga oleh layar peta. */
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

    private const val REGION_NAME = "orchid-forest"
    private const val MIN_ZOOM = 13.0
    private const val MAX_ZOOM = 18.0
    private const val MARGIN_DEG = 0.003

    private val _state = MutableStateFlow<OfflineMapState>(OfflineMapState.NotDownloaded)
    val state: StateFlow<OfflineMapState> = _state.asStateFlow()

    /** Memastikan area peta sudah tersimpan; mengunduh bila belum dan ada internet. */
    fun ensure(context: Context, pois: List<Poi>, force: Boolean = false) {
        if (pois.isEmpty()) return
        if (_state.value is OfflineMapState.Downloading) return
        val app = context.applicationContext
        MapLibre.getInstance(app)
        val manager = OfflineManager.getInstance(app)
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val existing = offlineRegions?.firstOrNull { String(it.metadata) == REGION_NAME }
                if (existing != null) {
                    existing.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status?.isComplete == true && !force) {
                                _state.value = OfflineMapState.Ready
                            } else if (isOnline(app)) {
                                download(existing)
                            }
                        }

                        override fun onError(error: String?) {
                            _state.value = OfflineMapState.Failed(error ?: "Status peta offline tidak terbaca.")
                        }
                    })
                } else if (isOnline(app)) {
                    create(manager, pois)
                }
            }

            override fun onError(error: String) {
                _state.value = OfflineMapState.Failed(error)
            }
        })
    }

    private fun create(manager: OfflineManager, pois: List<Poi>) {
        val bounds = LatLngBounds.from(
            pois.maxOf { it.lat } + MARGIN_DEG,
            pois.maxOf { it.lng } + MARGIN_DEG,
            pois.minOf { it.lat } - MARGIN_DEG,
            pois.minOf { it.lng } - MARGIN_DEG,
        )
        val definition = OfflineTilePyramidRegionDefinition(STYLE_URL, bounds, MIN_ZOOM, MAX_ZOOM, 2f)
        _state.value = OfflineMapState.Downloading(0f)
        manager.createOfflineRegion(definition, REGION_NAME.toByteArray(), object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) = download(offlineRegion)

            override fun onError(error: String) {
                _state.value = OfflineMapState.Failed(error)
            }
        })
    }

    private fun download(region: OfflineRegion) {
        _state.value = OfflineMapState.Downloading(0f)
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if (status.isComplete) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    _state.value = OfflineMapState.Ready
                } else if (status.requiredResourceCount > 0) {
                    _state.value = OfflineMapState.Downloading(
                        status.completedResourceCount.toFloat() / status.requiredResourceCount,
                    )
                }
            }

            override fun onError(error: OfflineRegionError) {
                // Kesalahan jaringan sementara akan dicoba ulang oleh MapLibre.
                if (error.reason != OfflineRegionError.REASON_CONNECTION) {
                    _state.value = OfflineMapState.Failed(error.message)
                }
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                _state.value = OfflineMapState.Failed("Area peta terlalu besar untuk diunduh.")
            }
        })
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
