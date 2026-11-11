/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Three-bar "now playing" equaliser. Bars bounce at different speeds while [playing]; when paused
 * they settle to a still, uneven rest so the row still reads as "this one".
 */
@Composable
fun PlayingBars(
    color: Color,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "playing_bars")
    val a by transition.animateFloat(0.25f, 1f, barBounce(520), label = "bar_a")
    val b by transition.animateFloat(0.9f, 0.3f, barBounce(440), label = "bar_b")
    val c by transition.animateFloat(0.4f, 0.95f, barBounce(610), label = "bar_c")

    // Blend between the live heights and the resting ones so pausing doesn't snap.
    val live by animateFloatAsState(if (playing) 1f else 0f, tween(260), label = "bars_live")

    // All animated values are read inside the draw block: bars redraw, the row doesn't recompose.
    Canvas(modifier = modifier.size(14.dp)) {
        val heights = listOf(
            mixBar(0.35f, a, live),
            mixBar(0.75f, b, live),
            mixBar(0.5f, c, live)
        )
        val barWidth = size.width / 5f
        heights.forEachIndexed { index, fraction ->
            val h = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(x = index * barWidth * 2f, y = size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

private fun barBounce(durationMillis: Int) =
    infiniteRepeatable<Float>(tween(durationMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse)

private fun mixBar(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction
