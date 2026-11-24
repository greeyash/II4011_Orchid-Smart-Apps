package com.orchidapps.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orchidapps.audio.AudioGuide
import com.orchidapps.data.Poi
import com.orchidapps.ui.illustration.SceneIllustration
import com.orchidapps.ui.illustration.sceneFor
import com.orchidapps.ui.theme.OrchidColors
import java.util.Locale

/** Kartu putih gading dengan sudut membulat, dipakai di hampir semua layar desain. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    color: Color = OrchidColors.Paper,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color, contentColor = OrchidColors.ForestDark),
        elevation = CardDefaults.cardElevation(defaultElevation = if (elevated) 4.dp else 0.dp),
        content = content,
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = OrchidColors.Paper,
    content: @Composable () -> Unit,
) {
    PaperCard(modifier.fillMaxWidth(), color = containerColor) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** Judul bagian kecil berhuruf kapital, mis. "KEGIATAN". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.ROOT),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = OrchidColors.ForestDark,
    )
}

enum class PillStyle { PINK, DARK, LIGHT }

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, style: PillStyle = PillStyle.PINK, leading: String? = null) {
    val (bg, fg) = when (style) {
        PillStyle.PINK -> OrchidColors.Blush to OrchidColors.ForestDark
        PillStyle.DARK -> OrchidColors.Forest to OrchidColors.Paper
        PillStyle.LIGHT -> OrchidColors.Paper to OrchidColors.ForestDark
    }
    Surface(modifier = modifier, color = bg, contentColor = fg, shape = CircleShape) {
        Text(
            listOfNotNull(leading, text).joinToString(" "),
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = OrchidColors.Forest, contentColor = OrchidColors.Paper),
    ) { ButtonContent(text, icon) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, OrchidColors.Forest),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = OrchidColors.Paper, contentColor = OrchidColors.ForestDark),
    ) { ButtonContent(text, icon) }
}

@Composable
fun PinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = OrchidColors.Blush, contentColor = OrchidColors.ForestDark),
    ) { ButtonContent(text, null) }
}

@Composable
private fun RowScope.ButtonContent(text: String, icon: ImageVector?) {
    icon?.let {
        Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontWeight = FontWeight.SemiBold)
}

/** Tombol bulat kecil di atas ilustrasi, mis. tombol kembali. */
@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = OrchidColors.Paper,
        contentColor = OrchidColors.ForestDark,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription, Modifier.size(20.dp)) }
    }
}

/**
 * Gambar POI: foto asli dari `assets/images/<id>.jpg` bila ada (PB06), selain itu ilustrasi.
 */
@Composable
fun PoiImage(poi: Poi, modifier: Modifier = Modifier, showFlowers: Boolean = true) {
    val photo = rememberAssetBitmap(AudioGuide.photoPath(poi))
    if (photo != null) {
        androidx.compose.foundation.Image(
            photo,
            contentDescription = "Foto ${poi.name}",
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else {
        SceneIllustration(sceneFor(poi), modifier, showFlowers)
    }
}

@Composable
fun WeakGpsBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = OrchidColors.Blush,
        contentColor = OrchidColors.ForestDark,
        shape = CircleShape,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.SignalCellularConnectedNoInternet0Bar,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("GPS lemah", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Kartu "Anda berada di …" di atas peta saat tiba di POI (L04-A, PB05/PB07). */
@Composable
fun ArrivalBanner(
    poi: Poi,
    autoPlay: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = OrchidColors.Forest, contentColor = OrchidColors.Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PoiImage(
                    poi,
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    showFlowers = false,
                )
                Column(Modifier.weight(1f)) {
                    Text("Anda tiba", style = MaterialTheme.typography.labelSmall, color = OrchidColors.Blush)
                    Text(
                        "Anda berada di ${poi.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = OrchidColors.Paper,
                    )
                    Text(
                        poi.category.label + if (autoPlay) " · Narasi diputar otomatis" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrchidColors.HillLight,
                    )
                }
            }
            // A04: pengingat keselamatan muncul saat tiba (mis. di Wood Bridge).
            if (poi.safetyFirst) {
                poi.safetyNote?.let {
                    Text("⚠️ $it", style = MaterialTheme.typography.bodySmall, color = OrchidColors.Blush)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PinkButton("Buka Detail", onClick = onOpen, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OrchidColors.HillLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrchidColors.Paper),
                ) { Text("Tutup") }
            }
        }
    }
}

/** Titik bulat pink bernomor seperti penanda POI di peta desain. */
@Composable
fun PoiNumberBadge(number: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(OrchidColors.Blush),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, style = MaterialTheme.typography.labelSmall, color = OrchidColors.ForestDark)
    }
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m"
    else String.format(Locale.US, "%.1f km", meters / 1000)

fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

/** Nomor POI tanpa awalan "P", mis. "P01" → "1", seperti label penanda di peta. */
fun Poi.number(): String = id.removePrefix("P").trimStart('0')

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Membaca gambar dari assets di luar main thread; null bila file belum ada. */
@Composable
fun rememberAssetBitmap(path: String): androidx.compose.ui.graphics.ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

/** Daftar foto POI di assets: `images/P01.jpg`, lalu `images/P01-2.jpg`, `P01-3.jpg`, … */
@Composable
fun rememberPoiPhotoPaths(poi: Poi): List<String> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val paths by androidx.compose.runtime.produceState(emptyList<String>(), poi.id) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val pattern = Regex("^${poi.id}(-(\\d+))?\\.jpg$")
            (context.assets.list("images") ?: emptyArray())
                .mapNotNull { name -> pattern.matchEntire(name)?.let { name to (it.groupValues[2].toIntOrNull() ?: 1) } }
                .sortedBy { it.second }
                .map { "images/${it.first}" }
        }
    }
    return paths
}

/**
 * Header halaman detail (PB06): galeri foto asli yang bisa digeser bila ada lebih dari satu foto,
 * atau ilustrasi bila foto belum tersedia.
 */
@Composable
fun PoiPhotoGallery(poi: Poi, modifier: Modifier = Modifier) {
    val paths = rememberPoiPhotoPaths(poi)
    if (paths.isEmpty()) {
        SceneIllustration(sceneFor(poi), modifier)
        return
    }
    val pager = androidx.compose.foundation.pager.rememberPagerState { paths.size }
    Box(modifier) {
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.matchParentSize()) { page ->
            val bitmap = rememberAssetBitmap(paths[page])
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap,
                    contentDescription = "Foto ${poi.name} ${page + 1}",
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            } else {
                Box(Modifier.matchParentSize().background(OrchidColors.MapGreen))
            }
        }
        if (paths.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(paths.size) { i ->
                    Box(
                        Modifier
                            .size(if (pager.currentPage == i) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (pager.currentPage == i) OrchidColors.Paper else OrchidColors.Paper.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}
