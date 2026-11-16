package com.orchidapps.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.orchidapps.ui.screens.findActivity

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Meminta izin lokasi (PB02) dan meneruskan hasilnya ke ViewModel. */
@Composable
fun rememberLocationPermissionRequest(vm: MainViewModel, onResult: (Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        // Setelah ditolak, rationale=false berarti pengguna memilih "Jangan tanya lagi".
        val activity = context.findActivity()
        val permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        vm.onPermissionResult(granted, permanentlyDenied)
        onResult(granted)
    }
    return {
        if (vm.location.value.permission == PermissionStatus.PERMANENTLY_DENIED) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } else {
            launcher.launch(LOCATION_PERMISSIONS)
        }
    }
}

/**
 * "Mulai Jelajah" (PB08). Izin notifikasi (Android 13+) diminta lebih dulu; sesi tetap jalan bila
 * ditolak karena getar dan suara tidak membutuhkan izin ini.
 */
@Composable
fun rememberStartExploration(vm: MainViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.startExploration()
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startExploration()
        }
    }
}
