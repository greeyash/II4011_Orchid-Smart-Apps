package com.orchidapps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orchidapps.audio.NarrationStatus
import com.orchidapps.data.Poi
import com.orchidapps.data.PoiArea
import com.orchidapps.data.PoiCategory
import com.orchidapps.exploration.ExplorationSettings
import com.orchidapps.location.DemoLocation
import com.orchidapps.location.Geo
import com.orchidapps.location.UserLocation
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.PermissionStatus
import com.orchidapps.ui.rememberLocationPermissionRequest
import com.orchidapps.ui.rememberStartExploration
import com.orchidapps.ui.theme.OrchidColors
import org.maplibre.geojson.LineString
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.LineLayer
import com.orchidapps.map.OfflineMapState
import com.orchidapps.map.OfflineMaps
import com.orchidapps.navigation.NavigationSession
import com.orchidapps.recommendation.Recommendation
import com.orchidapps.recommendation.RecommendationPrompt
import com.orchidapps.recommendation.Recommender
import androidx.compose.material.icons.filled.Directions
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
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/**
 * Layar utama Peta (L02 sebelum jelajah, L02-B ringkasan POI, L03 jelajah aktif; PB04, PB08).
 * Peta menampilkan posisi pengguna dan penanda POI bernomor; panel bawah berganti sesuai kondisi.
 */
@Composable
fun PoiMapScreen(vm: MainViewModel, onOpenSearch: () -> Unit, onOpenPoi: (String) -> Unit) {
    val context = LocalContext.current
    val datasetState by vm.dataset.collectAsStateWithLifecycle()
    val locationState by vm.location.collectAsStateWithLifecycle()
    val active by vm.explorationActive.collectAsStateWithLifecycle()
    val visited by vm.visitedPoiIds.collectAsStateWithLifecycle()
    val settings = remember { ExplorationSettings.get(context) }
    val autoPlay by settings.autoPlay.collectAsStateWithLifecycle()
    val narration by rememberNarrationPlayer().state.collectAsStateWithLifecycle()
    val dataset = (datasetState as? DatasetState.Loaded)?.dataset
    val demo by DemoLocation.enabled.collectAsStateWithLifecycle()
    val nav by NavigationSession.state.collectAsStateWithLifecycle()
    val navStartDistance by NavigationSession.startDistance.collectAsStateWithLifecycle()
    val recommendAfter by RecommendationPrompt.anchor.collectAsStateWithLifecycle()
    val rainMode by settings.rainMode.collectAsStateWithLifecycle()
    val offlineMap by OfflineMaps.state.collectAsStateWithLifecycle()
    fun recommendationsFrom(anchorId: String?): List<Recommendation> = vm.recommendationsAfter(anchorId)
    val startNavigation: (Poi) -> Unit = { poi ->
        if (!vm.startNavigation(poi)) {
            Toast.makeText(context, "Posisi Anda belum terbaca. Coba lagi sebentar.", Toast.LENGTH_SHORT).show()
        }
    }

    var category by rememberSaveable { mutableStateOf<PoiCategory?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var recenter by remember { mutableStateOf(0) }
    val startExploration = rememberStartExploration(vm)
    val speechWarning by vm.speechWarning.collectAsStateWithLifecycle()
    speechWarning?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissSpeechWarning,
            title = { Text("Suara belum siap") },
            text = { Text("$message\n\nTanpa suara, getar dan notifikasi tetap berjalan, tetapi nama lokasi dan narasi tidak terdengar.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissSpeechWarning()
                    runCatching { context.startActivity(android.content.Intent("com.android.settings.TTS_SETTINGS")) }
                }) { Text("Buka Pengaturan Suara") }
            },
            dismissButton = { TextButton(onClick = vm::dismissSpeechWarning) { Text("Nanti") } },
        )
    }
    val requestPermission = rememberLocationPermissionRequest(vm)

    Box(Modifier.fillMaxSize().background(OrchidColors.MapGreen)) {
        if (dataset != null) {
            val pois = dataset.pois.filter { category == null || it.category == category }
            PoiMap(
                area = dataset.area,
                pois = pois,
                visited = visited,
                selectedId = selectedId,
                user = locationState.location,
                recenterSignal = recenter,
                followUser = demo || nav != null,
                route = nav?.takeIf { !it.arrived }?.let { (it.originLat to it.originLng) to (it.target.lat to it.target.lng) },
                onPoiClick = { selectedId = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("Memuat data POI…", Modifier.align(Alignment.Center))
        }

        // Atas: status, kolom cari, filter kategori.
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (active) Pill("Jelajah Aktif", style = PillStyle.DARK, leading = "●")
                if (demo) Pill("Mode demo lokasi")
                // A01: peta kawasan sudah tersimpan di HP.
                if (offlineMap is OfflineMapState.Ready) Pill("Tersedia offline", style = PillStyle.LIGHT, leading = "✓")
                if (locationState.showWeakGps && !demo) WeakGpsBadge()
                Spacer(Modifier.weight(1f))
                // A05: Mode Hujan/Kabut diaktifkan manual.
                FilterChip(
                    selected = rainMode,
                    onClick = { settings.setRainMode(!rainMode) },
                    label = { Text("Hujan/Kabut") },
                    leadingIcon = { Icon(Icons.Filled.Umbrella, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = OrchidColors.Paper,
                        labelColor = OrchidColors.ForestDark,
                        iconColor = OrchidColors.ForestDark,
                        selectedContainerColor = OrchidColors.Forest,
                        selectedLabelColor = OrchidColors.Paper,
                        selectedLeadingIconColor = OrchidColors.Paper,
                    ),
                )
            }
            val navState = nav
            if (navState != null && !navState.arrived) {
                NavigationTopCard(navState)
            } else {
                SearchBarButton(onClick = onOpenSearch)
                CategoryChips(category, onChange = { category = it }, Modifier.fillMaxWidth())
            }
        }

        // Kanan: tombol ke posisi saya.
        if (locationState.location != null) {
            CircleIconButton(
                Icons.Filled.MyLocation,
                contentDescription = "Ke posisi saya",
                onClick = { recenter++ },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            )
        }

        // Mode Demo Lokasi: joystick untuk menggerakkan posisi seolah berjalan sendiri.
        if (demo) {
            DemoJoystick(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .offset(y = 40.dp),
            )
        }

        // Bawah: ringkasan POI / siap menjelajah / jelajah aktif.
        val selected = dataset?.pois?.firstOrNull { it.id == selectedId }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val navState = nav
            when {
                navState != null && navState.arrived -> NavigationArrivedCard(
                    state = navState,
                    onOpen = {
                        vm.stopNavigation()
                        onOpenPoi(navState.target.id)
                    },
                    onDone = vm::stopNavigation,
                ) {
                    // F07: setelah tiba di tujuan, tampilkan rekomendasi berikutnya.
                    Text("Lanjut ke mana?", style = MaterialTheme.typography.titleSmall)
                    RecommendationList(
                        recommendationsFrom(navState.target.id),
                        onNavigate = { startNavigation(it.poi) },
                        onOpen = {
                            vm.stopNavigation()
                            onOpenPoi(it.poi.id)
                        },
                    )
                }
                navState != null -> NavigationBottomCard(
                    state = navState,
                    startDistance = navStartDistance,
                    onStop = vm::stopNavigation,
                )
                selected != null -> PoiPreviewCard(
                    poi = selected,
                    visited = selected.id in visited,
                    user = locationState.location,
                    onOpen = { onOpenPoi(selected.id) },
                    onClose = { selectedId = null },
                    onNavigate = {
                        startNavigation(selected)
                        selectedId = null
                    },
                )
                rainMode -> ShelterCard(
                    shelters = dataset?.pois.orEmpty().filter { it.shelter }.let { shelters ->
                        val loc = locationState.location ?: return@let emptyList()
                        Recommender.recommend(shelters, emptySet(), loc.lat, loc.lng, max = 2)
                            .map { it.copy(reason = "Tempat berteduh, ${Recommender.activityHint(it.poi)}") }
                    },
                    onNavigate = {
                        settings.setRainMode(false)
                        startNavigation(it.poi)
                    },
                    onOpen = { onOpenPoi(it.poi.id) },
                    onTurnOff = { settings.setRainMode(false) },
                )
                recommendAfter != null -> RecommendationCard(
                    recommendations = recommendationsFrom(recommendAfter),
                    onNavigate = {
                        RecommendationPrompt.dismiss()
                        startNavigation(it.poi)
                    },
                    onOpen = {
                        RecommendationPrompt.dismiss()
                        onOpenPoi(it.poi.id)
                    },
                    onClose = RecommendationPrompt::dismiss,
                )
                active -> {
                    if (narration.isActive && narration.status != NarrationStatus.IDLE) MiniPlayerBar()
                    SecondaryButton(
                        "Akhiri Jelajah",
                        onClick = vm::endExploration,
                        icon = Icons.Filled.Stop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                locationState.approximateOnly && !demo -> ApproximateLocationCard(onFix = requestPermission)
                else -> ReadyCard(
                    hasPermission = locationState.permission == PermissionStatus.GRANTED,
                    autoPlay = autoPlay,
                    onAutoPlayChange = settings::setAutoPlay,
                    onStart = startExploration,
                    onRequestPermission = requestPermission,
                )
            }
        }
    }
}

@Composable
private fun SearchBarButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = OrchidColors.Paper,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = OrchidColors.ForestDark)
            Text(
                "Cari tempat atau kegiatan",
                Modifier.weight(1f).padding(start = 12.dp),
                color = OrchidColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(Icons.Filled.Tune, contentDescription = null, tint = OrchidColors.ForestDark)
        }
    }
}

/** Chip kategori: Semua, Wisata Alam, Wisata Tempat, Tempat Makan (F05). */
@Composable
fun CategoryChips(selected: PoiCategory?, onChange: (PoiCategory?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (listOf<PoiCategory?>(null) + PoiCategory.entries).forEach { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onChange(if (selected == c) null else c) },
                label = { Text(c?.label ?: "Semua") },
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = OrchidColors.Paper,
                    labelColor = OrchidColors.ForestDark,
                    selectedContainerColor = OrchidColors.Forest,
                    selectedLabelColor = OrchidColors.Paper,
                ),
            )
        }
    }
}

/** "Siap menjelajah?" (L02): Audio Otomatis dan tombol Mulai Jelajah. */
@Composable
private fun ReadyCard(
    hasPermission: Boolean,
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    PaperCard(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Siap menjelajah?", style = MaterialTheme.typography.titleLarge)
            Text(
                if (hasPermission) "Pakai earphone, lalu simpan ponsel Anda."
                else "Izinkan lokasi agar aplikasi memberi tahu saat Anda tiba di titik wisata.",
                style = MaterialTheme.typography.bodySmall,
                color = OrchidColors.Muted,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrchidColors.Cream)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Audio Otomatis", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = autoPlay,
                    onCheckedChange = onAutoPlayChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = OrchidColors.Forest),
                )
            }
            if (hasPermission) {
                PrimaryButton("Mulai Jelajah", onClick = onStart, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton("Izinkan Lokasi", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Ringkasan POI yang diketuk di peta (L02-B, UI21). */
@Composable
private fun PoiPreviewCard(
    poi: Poi,
    visited: Boolean,
    user: UserLocation?,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
) {
    PaperCard(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OrchidColors.Blush),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PoiImage(poi, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), showFlowers = false)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(poi.name, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(poi.category.label)
                        if (visited) Pill("Sudah dikunjungi", style = PillStyle.DARK, leading = "✓")
                        else Pill("Belum dikunjungi", style = PillStyle.LIGHT, leading = "•")
                    }
                    user?.let {
                        val d = Geo.distanceMeters(it.lat, it.lng, poi.lat, poi.lng)
                        Text(
                            "${formatDistance(d)} · ±${Geo.walkingMinutes(d)} menit jalan kaki",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrchidColors.Muted,
                        )
                    }
                }
            }
            Text(poi.shortDescription, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Navigasi", onClick = onNavigate, icon = Icons.Filled.Directions, modifier = Modifier.weight(1f))
                SecondaryButton("Lihat Detail", onClick = onOpen, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Tutup", color = OrchidColors.ForestDark)
            }
        }
    }
}

@Composable
private fun PoiMap(
    area: PoiArea,
    pois: List<Poi>,
    visited: Set<String>,
    selectedId: String?,
    user: UserLocation?,
    recenterSignal: Int,
    followUser: Boolean,
    route: Pair<Pair<Double, Double>, Pair<Double, Double>>?,
    onPoiClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Pusat peta digeser agar titik posisi tidak tertutup kolom cari (atas) dan kartu (bawah).
    val topPadPx = with(density) { 170.dp.toPx() }.toDouble()
    val bottomPadPx = with(density) { 300.dp.toPx() }.toDouble()
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
            m.uiSettings.isCompassEnabled = false
            m.uiSettings.isAttributionEnabled = false
            m.uiSettings.isLogoEnabled = false
            // Peta selalu menghadap utara agar arah joystick demo sesuai arah di layar.
            m.uiSettings.isRotateGesturesEnabled = false
            m.cameraPosition = CameraPosition.Builder()
                .target(LatLng(area.centerLat, area.centerLng))
                .zoom(INITIAL_ZOOM)
                .padding(0.0, topPadPx, 0.0, bottomPadPx)
                .build()
            val onStyleLoaded = Style.OnStyleLoaded { s ->
                addPoiLayers(s, withNumbers = !offline)
                style = s
            }
            // Tanpa internet peta dasar gagal dimuat; POI dan posisi tetap digambar di atas latar polos.
            mapView.addOnDidFailLoadingMapListener {
                if (!offline) {
                    offline = true
                    m.setStyle(Style.Builder().fromJson(FALLBACK_STYLE), onStyleLoaded)
                }
            }
            m.setStyle(Style.Builder().fromUri(OfflineMaps.STYLE_URL), onStyleLoaded)
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

    LaunchedEffect(style, pois, visited, selectedId) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_POI_AREA)?.setGeoJson(poiAreas(pois))
        s.getSourceAs<GeoJsonSource>(SOURCE_POI_POINT)?.setGeoJson(poiPoints(pois, visited, selectedId))
    }

    LaunchedEffect(style, route) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_ROUTE)?.setGeoJson(routeLine(route))
    }

    LaunchedEffect(style, user) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_USER_ACCURACY)?.setGeoJson(userAccuracy(user))
        s.getSourceAs<GeoJsonSource>(SOURCE_USER)?.setGeoJson(userPoint(user))
    }

    // Mode Demo Lokasi: kamera mengikuti titik posisi agar selalu terlihat selama "berjalan".
    LaunchedEffect(map, user, followUser) {
        val m = map ?: return@LaunchedEffect
        if (followUser && user != null) {
            m.easeCamera(CameraUpdateFactory.newLatLng(LatLng(user.lat, user.lng)), 450)
        }
    }

    LaunchedEffect(recenterSignal) {
        if (recenterSignal > 0 && user != null) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.lat, user.lng), USER_ZOOM))
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (offline) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = OrchidColors.Paper,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "Peta dasar butuh internet. Titik POI dan posisi Anda tetap ditampilkan.",
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun addPoiLayers(style: Style, withNumbers: Boolean) {
    style.addSource(GeoJsonSource(SOURCE_POI_AREA))
    style.addSource(GeoJsonSource(SOURCE_POI_POINT))
    style.addSource(GeoJsonSource(SOURCE_ROUTE))
    style.addSource(GeoJsonSource(SOURCE_USER_ACCURACY))
    style.addSource(GeoJsonSource(SOURCE_USER))

    style.addLayer(
        FillLayer(LAYER_POI_AREA, SOURCE_POI_AREA).withProperties(
            PropertyFactory.fillColor(BLUSH),
            PropertyFactory.fillOpacity(0.25f),
        )
    )
    // Garis rute navigasi (PB12).
    style.addLayer(
        LineLayer("route-line", SOURCE_ROUTE).withProperties(
            PropertyFactory.lineColor(FOREST),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineDasharray(arrayOf(1.5f, 1f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        )
    )
    // Penanda POI: lingkaran pink bernomor; POI terpilih/sudah dikunjungi berwarna hijau tua.
    style.addLayer(
        CircleLayer(LAYER_POI_DOT, SOURCE_POI_POINT).withProperties(
            PropertyFactory.circleRadius(Expression.get(PROP_RADIUS)),
            PropertyFactory.circleColor(Expression.get(PROP_FILL)),
            PropertyFactory.circleStrokeColor(FOREST),
            PropertyFactory.circleStrokeWidth(1.5f),
        )
    )
    if (withNumbers) {
        style.addLayer(
            SymbolLayer("poi-number", SOURCE_POI_POINT).withProperties(
                PropertyFactory.textField(Expression.get(PROP_NUMBER)),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Expression.get(PROP_TEXT)),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true),
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
            PropertyFactory.circleRadius(9f),
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
        }
    }
)

private fun poiPoints(pois: List<Poi>, visited: Set<String>, selectedId: String?) = FeatureCollection.fromFeatures(
    pois.map { poi ->
        val highlighted = poi.id == selectedId || poi.id in visited
        Feature.fromGeometry(Point.fromLngLat(poi.lng, poi.lat)).apply {
            addStringProperty(PROP_ID, poi.id)
            addStringProperty(PROP_NUMBER, poi.number())
            addStringProperty(PROP_FILL, if (highlighted) FOREST else BLUSH)
            addStringProperty(PROP_TEXT, if (highlighted) "#FEFCF7" else FOREST)
            addNumberProperty(PROP_RADIUS, if (poi.id == selectedId) 15f else 12f)
        }
    }
)

private fun routeLine(route: Pair<Pair<Double, Double>, Pair<Double, Double>>?) = FeatureCollection.fromFeatures(
    listOfNotNull(
        route?.let { (from, to) ->
            Feature.fromGeometry(
                LineString.fromLngLats(listOf(Point.fromLngLat(from.second, from.first), Point.fromLngLat(to.second, to.first)))
            )
        }
    )
)

private fun userPoint(user: UserLocation?) = FeatureCollection.fromFeatures(
    listOfNotNull(user?.let { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) })
)

private fun userAccuracy(user: UserLocation?) = FeatureCollection.fromFeatures(
    listOfNotNull(
        user?.accuracyMeters?.let { Feature.fromGeometry(circle(user.lat, user.lng, it.toDouble())) }
    )
)

private const val FALLBACK_STYLE =
    """{"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#E5E9DA"}}]}"""

private const val INITIAL_ZOOM = 17.0
private const val USER_ZOOM = 18.0
private const val USER_COLOR = "#1565C0"
private const val BLUSH = "#F6C2CF"
private const val FOREST = "#324A3A"

private const val SOURCE_POI_AREA = "poi-area"
private const val SOURCE_POI_POINT = "poi-point"
private const val SOURCE_USER_ACCURACY = "user-accuracy"
private const val SOURCE_USER = "user"
private const val SOURCE_ROUTE = "route"
private const val LAYER_POI_AREA = "poi-area-fill"
private const val LAYER_POI_DOT = "poi-dot"
private const val PROP_ID = "id"
private const val PROP_NUMBER = "number"
private const val PROP_FILL = "fill"
private const val PROP_TEXT = "text"
private const val PROP_RADIUS = "radius"

/**
 * Izin lokasi hanya "perkiraan" (Android 12+): akurasi 1-3 km membuat pemicu lokasi tidak berjalan,
 * jadi pengunjung diminta mengganti ke lokasi akurat.
 */
@Composable
private fun ApproximateLocationCard(onFix: () -> Unit) {
    PaperCard(Modifier.fillMaxWidth(), elevated = true, color = OrchidColors.BlushLight) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lokasi masih perkiraan", style = MaterialTheme.typography.titleLarge)
            Text(
                "Anda memberi izin lokasi perkiraan, jadi aplikasi tidak bisa tahu saat Anda tiba di " +
                    "titik wisata. Pilih \"Lokasi akurat\" agar notifikasi dan navigasi berjalan.",
                style = MaterialTheme.typography.bodySmall,
            )
            PrimaryButton("Izinkan Lokasi Akurat", onClick = onFix, modifier = Modifier.fillMaxWidth())
        }
    }
}
