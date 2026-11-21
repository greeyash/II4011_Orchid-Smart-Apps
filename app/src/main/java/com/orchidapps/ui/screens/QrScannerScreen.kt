package com.orchidapps.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.orchidapps.qr.QrCodes
import com.orchidapps.qr.QrResult
import com.orchidapps.ui.DatasetState
import com.orchidapps.ui.MainViewModel
import com.orchidapps.ui.theme.OrchidColors
import java.util.concurrent.Executors

/**
 * Pemindai QR (L08-A, PB14, A02): membuka halaman POI atau spesies anggrek dari kode di papan
 * informasi, tanpa internet dan tanpa menunggu pemicu lokasi.
 */
@Composable
fun QrScannerScreen(
    vm: MainViewModel,
    onResult: (QrResult) -> Unit,
    onBack: (() -> Unit)?,
    onManual: () -> Unit,
) {
    val context = LocalContext.current
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val pois = (dataset as? DatasetState.Loaded)?.dataset?.pois.orEmpty()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    // A02: izin kamera diminta saat pertama kali membuka pemindai.
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    var unknown by remember { mutableStateOf<String?>(null) }
    var handled by remember { mutableStateOf(false) }
    val currentPois by rememberUpdatedState(pois)

    Box(Modifier.fillMaxSize().background(OrchidColors.Forest)) {
        if (granted) {
            CameraPreview(
                onCode = { raw ->
                    if (handled || unknown != null || currentPois.isEmpty()) return@CameraPreview
                    when (val result = QrCodes.resolve(raw, currentPois)) {
                        is QrResult.Unknown -> unknown = raw
                        else -> {
                            handled = true
                            onResult(result)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                onBack?.let {
                    CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", onClick = it)
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(240.dp)
                        .border(3.dp, OrchidColors.Blush, RoundedCornerShape(24.dp)),
                )
            }
            if (granted) {
                Text(
                    "Arahkan kamera ke QR di papan lokasi atau papan anggrek",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OrchidColors.Paper,
                    textAlign = TextAlign.Center,
                )
                Pill("Bisa tanpa internet")
            } else {
                // A02 AC5: izin kamera ditolak → jelaskan alasannya dan tawarkan pilih POI manual.
                Text(
                    if (asked) "Izin kamera ditolak. Kamera dibutuhkan untuk memindai kode QR di papan informasi."
                    else "Izinkan kamera untuk memindai kode QR di papan informasi.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OrchidColors.Paper,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton("Izinkan Kamera", onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Pilih POI Manual", onClick = onManual, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // A02 AC4: QR asing menampilkan pesan dan tombol kembali.
    unknown?.let {
        AlertDialog(
            onDismissRequest = { unknown = null },
            title = { Text("Kode tidak dikenal") },
            text = { Text("Kode QR ini bukan dari aplikasi Orchid Forest.") },
            confirmButton = { TextButton(onClick = { unknown = null }) { Text("Pindai lagi") } },
            dismissButton = onBack?.let { back -> { TextButton(onClick = back) { Text("Kembali") } } },
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        codes.firstNotNullOfOrNull { it.rawValue }?.let { raw ->
                            ContextCompat.getMainExecutor(context).execute { currentOnCode(raw) }
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
