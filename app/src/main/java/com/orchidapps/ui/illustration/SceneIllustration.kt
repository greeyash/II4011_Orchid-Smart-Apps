package com.orchidapps.ui.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import kotlin.math.cos
import kotlin.math.sin

/** Objek utama ilustrasi pemandangan, mengikuti gaya ilustrasi di desain hi-fi. */
enum class SceneSubject { GREENHOUSE, BRIDGE, HOUSE, CASTLE, RABBIT, FLOWER, TREES }

private val Sky = Color(0xFFF6C2CF)
private val Sun = Color(0xFFFBE3EA)
private val HillFar = Color(0xFFAABEA7)
private val HillMid = Color(0xFF7FA085)
private val HillNear = Color(0xFF5B8166)
private val Pine = Color(0xFF2F4A3A)
private val White = Color(0xFFFEFCF7)
private val Ink = Color(0xFF263B2E)
private val Petal = Color(0xFFF2A7BA)
private val PetalCenter = Color(0xFFB4506D)

/** Pilihan ilustrasi untuk tiap POI (dipakai selama foto asli lokasi belum tersedia). */
fun sceneFor(poi: Poi): SceneSubject = when (poi.id) {
    "P01" -> SceneSubject.BRIDGE
    "P02" -> SceneSubject.CASTLE
    "P04", "P16", "P18" -> SceneSubject.GREENHOUSE
    "P07", "P13", "P15" -> SceneSubject.FLOWER
    "P08", "P11", "P14" -> SceneSubject.RABBIT
    "P06" -> SceneSubject.TREES
    else -> if (poi.category == PoiCategory.WISATA_ALAM) SceneSubject.TREES else SceneSubject.HOUSE
}

@Composable
fun SceneIllustration(
    subject: SceneSubject,
    modifier: Modifier = Modifier,
    showFlowers: Boolean = true,
) {
    Canvas(modifier) {
        drawRect(Sky)
        drawCircle(Sun, radius = size.minDimension * 0.17f, center = Offset(size.width * 0.78f, size.height * 0.24f))
        hill(HillFar, baseY = 0.56f, amplitude = 0.06f, phase = 0f)
        pine(0.12f, 0.62f, 0.11f)
        pine(0.88f, 0.60f, 0.13f)
        hill(HillMid, baseY = 0.70f, amplitude = 0.05f, phase = 1.6f)
        when (subject) {
            SceneSubject.GREENHOUSE -> greenhouse()
            SceneSubject.BRIDGE -> bridge()
            SceneSubject.HOUSE -> house()
            SceneSubject.CASTLE -> castle()
            SceneSubject.RABBIT -> rabbit()
            SceneSubject.FLOWER -> orchidFlower(size.width * 0.5f, size.height * 0.48f, size.minDimension * 0.24f)
            SceneSubject.TREES -> {
                pine(0.38f, 0.74f, 0.20f)
                pine(0.58f, 0.76f, 0.26f)
                lights()
            }
        }
        hill(HillNear, baseY = 0.84f, amplitude = 0.04f, phase = 3.1f)
        if (showFlowers) {
            listOf(0.18f to 0.88f, 0.48f to 0.92f, 0.80f to 0.89f).forEach { (x, y) ->
                smallFlower(size.width * x, size.height * y, size.minDimension * 0.05f)
            }
        }
    }
}

/** Ilustrasi bunga anggrek untuk kartu dan halaman spesies. */
@Composable
fun OrchidIllustration(
    petalColor: Color,
    modifier: Modifier = Modifier,
    centerColor: Color = PetalCenter,
) {
    Canvas(modifier) {
        drawRect(Sky)
        hill(HillNear, baseY = 0.78f, amplitude = 0.05f, phase = 0.8f)
        drawLine(Ink, Offset(size.width * 0.5f, size.height * 0.42f), Offset(size.width * 0.5f, size.height * 0.9f), strokeWidth = size.minDimension * 0.02f)
        orchidFlower(size.width * 0.5f, size.height * 0.38f, size.minDimension * 0.26f, petalColor, centerColor)
    }
}

private fun DrawScope.hill(color: Color, baseY: Float, amplitude: Float, phase: Float) {
    val path = Path().apply {
        moveTo(0f, size.height)
        val steps = 24
        for (i in 0..steps) {
            val x = size.width * i / steps
            val y = size.height * (baseY + amplitude * sin(i / steps.toFloat() * 6.28f + phase))
            lineTo(x, y)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.pine(xFrac: Float, baseFrac: Float, heightFrac: Float) {
    val x = size.width * xFrac
    val base = size.height * baseFrac
    val h = size.height * heightFrac
    val w = h * 0.55f
    val path = Path().apply {
        moveTo(x, base - h)
        lineTo(x + w / 2, base)
        lineTo(x - w / 2, base)
        close()
    }
    drawPath(path, Pine)
}

private fun DrawScope.greenhouse() {
    val w = size.width * 0.36f
    val h = size.height * 0.24f
    val left = size.width * 0.5f - w / 2
    val bottom = size.height * 0.76f
    val top = bottom - h
    val stroke = size.minDimension * 0.012f
    val dome = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + h * 0.35f)
        cubicTo(left, top - h * 0.25f, left + w, top - h * 0.25f, left + w, top + h * 0.35f)
        lineTo(left + w, bottom)
        close()
    }
    drawPath(dome, White)
    drawPath(dome, Ink, style = Stroke(stroke))
    for (i in 1..3) {
        val x = left + w * i / 4
        drawLine(Ink, Offset(x, top - h * 0.05f), Offset(x, bottom), stroke)
    }
    drawLine(Ink, Offset(left, top + h * 0.45f), Offset(left + w, top + h * 0.45f), stroke)
    for (i in 0..3) smallFlower(left + w * (i + 0.5f) / 4, bottom - h * 0.12f, w * 0.06f)
}

private fun DrawScope.bridge() {
    val y = size.height * 0.64f
    val sag = size.height * 0.06f
    val stroke = size.minDimension * 0.014f
    val deck = Path().apply {
        moveTo(size.width * 0.08f, y)
        quadraticTo(size.width * 0.5f, y + sag * 2, size.width * 0.92f, y)
    }
    drawPath(deck, White, style = Stroke(size.minDimension * 0.035f))
    val rope = Path().apply {
        moveTo(size.width * 0.08f, y - size.height * 0.08f)
        quadraticTo(size.width * 0.5f, y + sag * 1.2f, size.width * 0.92f, y - size.height * 0.08f)
    }
    drawPath(rope, White, style = Stroke(stroke))
    for (i in 1..9) {
        val t = i / 10f
        val x = size.width * (0.08f + 0.84f * t)
        val deckY = y + sag * 2 * 2 * t * (1 - t)
        val ropeY = y - size.height * 0.08f + (sag * 1.2f + size.height * 0.08f) * 2 * t * (1 - t)
        drawLine(White, Offset(x, ropeY), Offset(x, deckY), stroke * 0.7f)
    }
    drawRect(White, Offset(size.width * 0.07f, y - size.height * 0.1f), Size(stroke * 2.2f, size.height * 0.12f))
    drawRect(White, Offset(size.width * 0.91f, y - size.height * 0.1f), Size(stroke * 2.2f, size.height * 0.12f))
}

private fun DrawScope.house() {
    val w = size.width * 0.32f
    val h = size.height * 0.2f
    val left = size.width * 0.5f - w / 2
    val bottom = size.height * 0.78f
    drawRect(White, Offset(left, bottom - h), Size(w, h))
    val roof = Path().apply {
        moveTo(left - w * 0.1f, bottom - h)
        lineTo(left + w / 2, bottom - h * 1.9f)
        lineTo(left + w * 1.1f, bottom - h)
        close()
    }
    drawPath(roof, Pine)
    drawRoundRect(HillNear, Offset(left + w * 0.4f, bottom - h * 0.65f), Size(w * 0.2f, h * 0.65f), CornerRadius(w * 0.03f))
    drawRect(Petal, Offset(left + w * 0.1f, bottom - h * 0.7f), Size(w * 0.18f, h * 0.25f))
    drawRect(Petal, Offset(left + w * 0.72f, bottom - h * 0.7f), Size(w * 0.18f, h * 0.25f))
}

private fun DrawScope.castle() {
    val w = size.width * 0.34f
    val h = size.height * 0.22f
    val left = size.width * 0.5f - w / 2
    val bottom = size.height * 0.78f
    drawRect(White, Offset(left, bottom - h), Size(w, h))
    val notch = w / 7
    for (i in 0 until 7 step 2) drawRect(White, Offset(left + notch * i, bottom - h - notch), Size(notch, notch))
    drawRoundRect(HillNear, Offset(left + w * 0.38f, bottom - h * 0.6f), Size(w * 0.24f, h * 0.6f), CornerRadius(w * 0.12f))
    drawLine(Ink, Offset(left + w / 2, bottom - h - notch), Offset(left + w / 2, bottom - h * 1.9f), size.minDimension * 0.01f)
    val flag = Path().apply {
        moveTo(left + w / 2, bottom - h * 1.9f)
        lineTo(left + w * 0.75f, bottom - h * 1.75f)
        lineTo(left + w / 2, bottom - h * 1.6f)
        close()
    }
    drawPath(flag, PetalCenter)
}

private fun DrawScope.rabbit() {
    val cx = size.width * 0.5f
    val base = size.height * 0.80f
    val r = size.minDimension * 0.11f
    drawOval(White, Offset(cx - r * 1.2f, base - r * 1.7f), Size(r * 2.4f, r * 1.8f))
    drawCircle(White, r * 0.8f, Offset(cx, base - r * 2.1f))
    listOf(-0.35f, 0.35f).forEach { dx ->
        drawOval(White, Offset(cx + r * dx - r * 0.22f, base - r * 4.0f), Size(r * 0.44f, r * 1.5f))
        drawOval(Petal, Offset(cx + r * dx - r * 0.1f, base - r * 3.8f), Size(r * 0.2f, r * 1.1f))
    }
    drawCircle(Ink, r * 0.09f, Offset(cx - r * 0.3f, base - r * 2.2f))
    drawCircle(Ink, r * 0.09f, Offset(cx + r * 0.3f, base - r * 2.2f))
    drawCircle(PetalCenter, r * 0.08f, Offset(cx, base - r * 1.95f))
}

private fun DrawScope.lights() {
    val stroke = size.minDimension * 0.006f
    val path = Path().apply {
        moveTo(size.width * 0.1f, size.height * 0.45f)
        quadraticTo(size.width * 0.5f, size.height * 0.62f, size.width * 0.9f, size.height * 0.45f)
    }
    drawPath(path, Ink, style = Stroke(stroke))
    for (i in 1..8) {
        val t = i / 9f
        val x = size.width * (0.1f + 0.8f * t)
        val y = size.height * (0.45f + 0.17f * 2 * t * (1 - t))
        drawCircle(Color(0xFFFFF3C4), size.minDimension * 0.018f, Offset(x, y + size.minDimension * 0.02f))
    }
}

private fun DrawScope.smallFlower(cx: Float, cy: Float, r: Float) {
    for (i in 0 until 5) {
        val a = i * 72f * (Math.PI / 180).toFloat()
        drawCircle(Petal, r * 0.55f, Offset(cx + cos(a) * r * 0.6f, cy + sin(a) * r * 0.6f))
    }
    drawCircle(PetalCenter, r * 0.35f, Offset(cx, cy))
}

/** Bunga anggrek sederhana: tiga kelopak besar dan bibir bunga di tengah. */
private fun DrawScope.orchidFlower(
    cx: Float,
    cy: Float,
    r: Float,
    petal: Color = White,
    center: Color = PetalCenter,
) {
    val stroke = Stroke(r * 0.05f)
    listOf(-90f, 30f, 150f).forEach { angle ->
        rotate(angle, Offset(cx, cy)) {
            val rect = Rect(Offset(cx + r * 0.15f, cy - r * 0.32f), Size(r * 0.85f, r * 0.64f))
            drawOval(petal, rect.topLeft, rect.size)
            drawOval(Ink, rect.topLeft, rect.size, style = stroke)
        }
    }
    drawCircle(Petal, r * 0.3f, Offset(cx, cy))
    drawCircle(center, r * 0.16f, Offset(cx, cy))
    drawCircle(Ink, r * 0.3f, Offset(cx, cy), style = stroke)
}
