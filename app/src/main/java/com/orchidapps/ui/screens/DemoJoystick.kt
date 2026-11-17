package com.orchidapps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.orchidapps.location.DemoLocation
import com.orchidapps.ui.theme.OrchidColors
import kotlin.math.roundToInt

/**
 * Joystick Mode Demo Lokasi: geser tombol bulat ke arah mana pun dan posisi demo ikut berjalan
 * ke arah itu. Makin jauh digeser, makin cepat; dilepas berarti berhenti.
 */
@Composable
fun DemoJoystick(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val baseSize = 120.dp
    val knobSize = 52.dp
    val radiusPx = with(density) { ((baseSize - knobSize) / 2).toPx() }
    var knob by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) { onDispose { DemoLocation.setVelocity(0.0, 0.0) } }

    Box(
        modifier
            .size(baseSize)
            .clip(CircleShape)
            .background(OrchidColors.Paper.copy(alpha = 0.85f))
            .border(1.5.dp, OrchidColors.Forest, CircleShape)
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    val length = offset.getDistance()
                    knob = if (length > radiusPx) offset * (radiusPx / length) else offset
                    // Layar: x ke kanan = timur, y ke bawah = selatan.
                    DemoLocation.setVelocity(knob.x / radiusPx.toDouble(), -knob.y / radiusPx.toDouble())
                }
                detectDragGestures(
                    onDragStart = { start -> update(start - Offset(size.width / 2f, size.height / 2f)) },
                    onDragEnd = {
                        knob = Offset.Zero
                        DemoLocation.setVelocity(0.0, 0.0)
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        DemoLocation.setVelocity(0.0, 0.0)
                    },
                ) { change, drag ->
                    change.consume()
                    update(knob + drag)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(OrchidColors.Forest),
        )
    }
}
