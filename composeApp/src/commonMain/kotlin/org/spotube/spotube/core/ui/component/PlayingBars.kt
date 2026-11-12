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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Three-bar "now playing" equaliser. Bars bounce at different speeds while [playing]; when paused
 * they settle to a still, uneven rest so the row still reads as "this one". The infinite
 * transition only exists while playing (plus the short settle), so paused rows cost no frames.
 */
@Composable
fun PlayingBars(
    color: Color,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    // Blend between the live heights and the resting ones so pausing doesn't snap.
    val live = animateFloatAsState(if (playing) 1f else 0f, tween(260), label = "bars_live")
    val animating by remember(playing) { derivedStateOf { playing || live.value > 0f } }

    if (animating) {
        val transition = rememberInfiniteTransition(label = "playing_bars")
        val a = transition.animateFloat(0.25f, 1f, barBounce(520), label = "bar_a")
        val b = transition.animateFloat(0.9f, 0.3f, barBounce(440), label = "bar_b")
        val c = transition.animateFloat(0.4f, 0.95f, barBounce(610), label = "bar_c")
        // Animated values are read inside the draw block: bars redraw, the row doesn't recompose.
        BarsCanvas(color, modifier) { index ->
            when (index) {
                0 -> mixBar(REST[0], a.value, live.value)
                1 -> mixBar(REST[1], b.value, live.value)
                else -> mixBar(REST[2], c.value, live.value)
            }
        }
    } else {
        BarsCanvas(color, modifier) { index -> REST[index] }
    }
}

/** Resting bar heights (fraction of the box) shown while paused. */
private val REST = floatArrayOf(0.35f, 0.75f, 0.5f)

@Composable
private fun BarsCanvas(color: Color, modifier: Modifier, height: (Int) -> Float) {
    Canvas(modifier = modifier.size(14.dp)) {
        val barWidth = size.width / 5f
        for (index in 0 until 3) {
            val h = size.height * height(index)
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
