package com.orchidapps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orchidapps.ui.screens.ArrivalBanner
import com.orchidapps.ui.screens.HomeScreen
import com.orchidapps.ui.screens.MiniPlayerBar
import com.orchidapps.ui.screens.PoiDetailScreen
import com.orchidapps.ui.screens.PoiListScreen
import com.orchidapps.ui.screens.PoiMapScreen

private object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val POI_LIST = "pois"
    const val POI_DETAIL = "poi/{id}"
    fun poiDetail(id: String) = "poi/$id"
}

@Composable
fun OrchidApp() {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()
    val locationState by vm.location.collectAsStateWithLifecycle()
    val arrival by vm.arrival.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        vm.refreshPermission()
        onPauseOrDispose { }
    }

    // Lokasi dibaca selama aplikasi terlihat, di layar mana pun (dipakai peta dan geofence).
    // Pelacakan saat layar mati menyusul di PB08.
    val granted = locationState.permission == PermissionStatus.GRANTED
    LifecycleStartEffect(granted) {
        if (granted) vm.startTracking()
        onStopOrDispose { vm.stopTracking() }
    }

    val openPoi: (String) -> Unit = { navController.navigate(Routes.poiDetail(it)) }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onOpenPoiList = { navController.navigate(Routes.POI_LIST) },
                    onOpenPoi = openPoi,
                )
            }
            composable(Routes.MAP) {
                PoiMapScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPoi = openPoi,
                )
            }
            composable(Routes.POI_LIST) {
                PoiListScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPoi = openPoi,
                )
            }
            composable(
                Routes.POI_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                PoiDetailScreen(
                    vm = vm,
                    poiId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            arrival?.let { poi ->
                ArrivalBanner(
                    poi = poi,
                    onOpen = {
                        vm.dismissArrival()
                        openPoi(poi.id)
                    },
                    onDismiss = vm::dismissArrival,
                )
            }
            MiniPlayerBar()
        }
    }
}
