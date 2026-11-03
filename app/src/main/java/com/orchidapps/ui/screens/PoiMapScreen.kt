package com.orchidapps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.data.Poi
import com.orchidapps.data.PoiArea
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.Geo
import com.orchidapps.location.UserLocation
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/** Peta kawasan dengan titik POI, radius pemicu, dan posisi pengguna (PB04). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiMapScreen(vm: MainViewModel, onBack: () -> Unit, onOpenPoi: (String) -> Unit) {
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val locationState by vm.location.collectAsStateWithLifecycle()
    val dataset = (datasetState as? DatasetState.Loaded)?.dataset

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peta Orchid Forest") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (dataset == null) {
                Text("Memuat data POI…", Modifier.align(Alignment.Center))
                return@Box
            }
            PoiMap(
                area = dataset.area,
                pois = dataset.pois,
                user = locationState.location,
                onPoiClick = onOpenPoi,
                modifier = Modifier.fillMaxSize(),
            )
            if (locationState.showWeakGps) {
                WeakGpsBadge(Modifier.align(Alignment.TopStart).padding(12.dp))
            }
        }
    }
}

@Composable
private fun PoiMap(
    area: PoiArea,
    pois: List<Poi>,
    user: UserLocation?,
    onPoiClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var offline by remember { mutableStateOf(false) }
    val currentOnPoiClick by rememberUpdatedState(onPoiClick)

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.cameraPosition = CameraPosition.Builder()
                .target(LatLng(area.centerLat, area.centerLng))
                .zoom(INITIAL_ZOOM)
                .build()
            val onStyleLoaded = Style.OnStyleLoaded { s ->
                addPoiLayers(s, withLabels = !offline)
                style = s
            }
            // Tanpa internet peta dasar gagal dimuat; POI dan posisi tetap digambar di atas latar polos.
            mapView.addOnDidFailLoadingMapListener {
                if (!offline) {
                    offline = true
                    m.setStyle(Style.Builder().fromJson(FALLBACK_STYLE), onStyleLoaded)
                }
            }
            m.setStyle(Style.Builder().fromUri(STYLE_URL), onStyleLoaded)
            m.addOnMapClickListener { point ->
                val screen = m.projection.toScreenLocation(point)
                val id = m.queryRenderedFeatures(screen, LAYER_POI_DOT, LAYER_POI_AREA)
                    .firstNotNullOfOrNull { it.getStringProperty(PROP_ID) }
                id?.let(currentOnPoiClick)
                id != null
            }
            map = m
        }
    }

    LaunchedEffect(style, pois) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_POI_AREA)?.setGeoJson(poiAreas(pois))
        s.getSourceAs<GeoJsonSource>(SOURCE_POI_POINT)?.setGeoJson(poiPoints(pois))
    }

    LaunchedEffect(style, user) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_USER_ACCURACY)?.setGeoJson(userAccuracy(user))
        s.getSourceAs<GeoJsonSource>(SOURCE_USER)?.setGeoJson(userPoint(user))
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        if (offline) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "Peta dasar butuh internet. Titik POI dan posisi Anda tetap ditampilkan.",
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PoiCategory.entries.forEach { MapLegendItem(it) }
        }

        if (user != null) {
            SmallFloatingActionButton(
                onClick = {
                    map?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(user.lat, user.lng), USER_ZOOM)
                    )
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Ke posisi saya")
            }
        }
    }
}

@Composable
private fun MapLegendItem(category: PoiCategory) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            "● ${category.label}",
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(category.mapColor)),
        )
    }
}

private fun addPoiLayers(style: Style, withLabels: Boolean) {
    style.addSource(GeoJsonSource(SOURCE_POI_AREA))
    style.addSource(GeoJsonSource(SOURCE_POI_POINT))
    style.addSource(GeoJsonSource(SOURCE_USER_ACCURACY))
    style.addSource(GeoJsonSource(SOURCE_USER))

    style.addLayer(
        FillLayer(LAYER_POI_AREA, SOURCE_POI_AREA).withProperties(
            PropertyFactory.fillColor(Expression.get(PROP_COLOR)),
            PropertyFactory.fillOpacity(0.18f),
        )
    )
    style.addLayer(
        LineLayer("poi-area-line", SOURCE_POI_AREA).withProperties(
            PropertyFactory.lineColor(Expression.get(PROP_COLOR)),
            PropertyFactory.lineWidth(1.5f),
        )
    )
    style.addLayer(
        CircleLayer(LAYER_POI_DOT, SOURCE_POI_POINT).withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor(Expression.get(PROP_COLOR)),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
    if (withLabels) {
        style.addLayer(
            SymbolLayer("poi-label", SOURCE_POI_POINT).withProperties(
                PropertyFactory.textField(Expression.get(PROP_NAME)),
                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                PropertyFactory.textSize(13f),
                PropertyFactory.textOffset(arrayOf(0f, 1.3f)),
                PropertyFactory.textAnchor("top"),
                PropertyFactory.textColor("#1B1B1B"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(1.5f),
            )
        )
    }
    style.addLayer(
        FillLayer("user-accuracy", SOURCE_USER_ACCURACY).withProperties(
            PropertyFactory.fillColor(USER_COLOR),
            PropertyFactory.fillOpacity(0.15f),
        )
    )
    style.addLayer(
        CircleLayer("user-dot", SOURCE_USER).withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor(USER_COLOR),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.5f),
        )
    )
}

private fun circle(lat: Double, lng: Double, radiusMeters: Double): Polygon {
    val ring = Geo.circlePolygon(lat, lng, radiusMeters).map { (pLat, pLng) -> Point.fromLngLat(pLng, pLat) }
    return Polygon.fromLngLats(listOf(ring))
}

private fun poiAreas(pois: List<Poi>) = FeatureCollection.fromFeatures(
    pois.map { poi ->
        Feature.fromGeometry(circle(poi.lat, poi.lng, poi.radiusMeters.toDouble())).apply {
            addStringProperty(PROP_ID, poi.id)
            addStringProperty(PROP_COLOR, poi.category.mapColor)
        }
    }
)

private fun poiPoints(pois: List<Poi>) = FeatureCollection.fromFeatures(
    pois.map { poi ->
        Feature.fromGeometry(Point.fromLngLat(poi.lng, poi.lat)).apply {
            addStringProperty(PROP_ID, poi.id)
            addStringProperty(PROP_NAME, poi.name)
            addStringProperty(PROP_COLOR, poi.category.mapColor)
        }
    }
)

private fun userPoint(user: UserLocation?) = FeatureCollection.fromFeatures(
    listOfNotNull(user?.let { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) })
)

private fun userAccuracy(user: UserLocation?) = FeatureCollection.fromFeatures(
    listOfNotNull(
        user?.accuracyMeters?.let { Feature.fromGeometry(circle(user.lat, user.lng, it.toDouble())) }
    )
)

private val PoiCategory.mapColor: String
    get() = when (this) {
        PoiCategory.WISATA_ALAM -> "#2E7D32"
        PoiCategory.WISATA_TEMPAT -> "#6A1B9A"
        PoiCategory.TEMPAT_MAKAN -> "#E65100"
    }

/** Gaya peta dasar OpenFreeMap (gratis, tanpa API key). */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FALLBACK_STYLE =
    """{"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#E8EFE3"}}]}"""

private const val INITIAL_ZOOM = 17.0
private const val USER_ZOOM = 18.0
private const val USER_COLOR = "#1565C0"

private const val SOURCE_POI_AREA = "poi-area"
private const val SOURCE_POI_POINT = "poi-point"
private const val SOURCE_USER_ACCURACY = "user-accuracy"
private const val SOURCE_USER = "user"
private const val LAYER_POI_AREA = "poi-area-fill"
private const val LAYER_POI_DOT = "poi-dot"
private const val PROP_ID = "id"
private const val PROP_NAME = "name"
private const val PROP_COLOR = "color"
