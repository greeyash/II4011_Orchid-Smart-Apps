package com.orchidapps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orchidapps.exploration.ExplorationSession
import com.orchidapps.exploration.ExplorationSettings
import com.orchidapps.ui.screens.ArrivalBanner
import com.orchidapps.ui.screens.MiniPlayerBar
import com.orchidapps.ui.screens.PermissionScreen
import com.orchidapps.ui.screens.PoiDetailScreen
import com.orchidapps.ui.screens.PoiListScreen
import com.orchidapps.ui.screens.PoiMapScreen
import com.orchidapps.ui.screens.SettingsScreen
import com.orchidapps.ui.screens.SpeciesDetailScreen
import com.orchidapps.ui.screens.WelcomeScreen
import com.orchidapps.ui.theme.OrchidColors

private object Routes {
    const val WELCOME = "welcome"
    const val PERMISSION = "permission"
    const val MAIN = "main"
    const val POI_DETAIL = "poi/{id}"
    const val SPECIES = "poi/{id}/species/{speciesId}"
    fun poiDetail(id: String) = "poi/$id"
    fun species(poiId: String, speciesId: String) = "poi/$poiId/species/$speciesId"
}

private enum class Tab(val label: String, val icon: ImageVector) {
    MAP("Peta", Icons.Filled.Map),
    SEARCH("Cari", Icons.Filled.Search),
    PLACES("Tempat", Icons.AutoMirrored.Filled.List),
    SETTINGS("Pengaturan", Icons.Filled.Settings),
}

@Composable
fun OrchidApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()
    val settings = remember { ExplorationSettings.get(context) }
    val locationState by vm.location.collectAsStateWithLifecycle()
    val arrival by vm.arrival.collectAsStateWithLifecycle()
    val autoPlay by settings.autoPlay.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LifecycleResumeEffect(Unit) {
        vm.refreshPermission()
        onPauseOrDispose { }
    }

    // Lokasi dibaca selama aplikasi terlihat untuk peta dan daftar tempat. Geofence dan
    // pelacakan saat layar mati berjalan di ExplorationService selama sesi jelajah (PB08).
    val granted = locationState.permission == PermissionStatus.GRANTED
    LifecycleStartEffect(granted) {
        if (granted) vm.startTracking()
        onStopOrDispose { vm.stopTracking() }
    }

    val openPoi: (String) -> Unit = { navController.navigate(Routes.poiDetail(it)) }
    val goMain: () -> Unit = {
        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
    }

    // Ketukan pada notifikasi kedatangan langsung membuka halaman POI (F03 AC4).
    val openRequest by ExplorationSession.openPoiRequest.collectAsStateWithLifecycle()
    LaunchedEffect(openRequest, route) {
        val id = openRequest ?: return@LaunchedEffect
        if (route == null || route == Routes.WELCOME || route == Routes.PERMISSION) return@LaunchedEffect
        ExplorationSession.consumeOpenPoiRequest()
        vm.dismissArrival()
        openPoi(id)
    }

    val start = when {
        !settings.welcomeSeen -> Routes.WELCOME
        else -> Routes.MAIN
    }

    Box(Modifier.fillMaxSize().background(OrchidColors.Cream)) {
        NavHost(navController = navController, startDestination = start) {
            composable(Routes.WELCOME) {
                WelcomeScreen(onStart = {
                    settings.markWelcomeSeen()
                    if (vm.location.value.permission == PermissionStatus.GRANTED) goMain()
                    else navController.navigate(Routes.PERMISSION)
                })
            }
            composable(Routes.PERMISSION) {
                PermissionScreen(vm, onDone = goMain)
            }
            composable(Routes.MAIN) {
                MainScreen(vm, onOpenPoi = openPoi)
            }
            composable(
                Routes.POI_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                PoiDetailScreen(
                    vm = vm,
                    poiId = id,
                    onBack = { navController.popBackStack() },
                    onOpenSpecies = { navController.navigate(Routes.species(id, it)) },
                )
            }
            composable(
                Routes.SPECIES,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("speciesId") { type = NavType.StringType },
                ),
            ) { entry ->
                SpeciesDetailScreen(
                    vm = vm,
                    poiId = entry.arguments?.getString("id").orEmpty(),
                    speciesId = entry.arguments?.getString("speciesId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        val onboarding = route == Routes.WELCOME || route == Routes.PERMISSION
        // L04-A: kartu "Anda berada di …" di bagian atas layar.
        if (!onboarding) {
            arrival?.let { poi ->
                ArrivalBanner(
                    poi = poi,
                    autoPlay = autoPlay,
                    onOpen = {
                        vm.dismissArrival()
                        openPoi(poi.id)
                    },
                    onDismiss = vm::dismissArrival,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(12.dp),
                )
            }
        }
        // Kontrol audio ringkas di halaman detail (UI04); di layar utama diatur oleh MainScreen.
        if (route == Routes.POI_DETAIL || route == Routes.SPECIES) {
            MiniPlayerBar(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
            )
        }
    }
}

/** Layar utama dengan menu bawah: Peta, Cari, Tempat, Pengaturan. */
@Composable
private fun MainScreen(vm: MainViewModel, onOpenPoi: (String) -> Unit) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var focusSearch by remember { androidx.compose.runtime.mutableStateOf(false) }
    val tab = Tab.entries[tabIndex]

    Scaffold(
        containerColor = OrchidColors.Cream,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = OrchidColors.Paper) {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tabIndex == i,
                        onClick = {
                            focusSearch = false
                            tabIndex = i
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrchidColors.ForestDark,
                            selectedTextColor = OrchidColors.ForestDark,
                            indicatorColor = OrchidColors.Blush,
                            unselectedIconColor = OrchidColors.Muted,
                            unselectedTextColor = OrchidColors.Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.MAP -> PoiMapScreen(
                    vm,
                    onOpenSearch = {
                        focusSearch = true
                        tabIndex = Tab.SEARCH.ordinal
                    },
                    onOpenPoi = onOpenPoi,
                )
                Tab.SEARCH -> PoiListScreen(vm, onOpenPoi, showSearch = true, autoFocus = focusSearch)
                Tab.PLACES -> PoiListScreen(vm, onOpenPoi, showSearch = false)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
            if (tab != Tab.MAP) {
                MiniPlayerBar(Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
        }
    }
}
