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

package org.spotube.spotube.modules.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.core.ui.theming.coolGlow
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.time.TimeSource

/**
 * Drives the window-wide "room light": which artwork colours the room, and whether it is lit.
 *
 * The room normally takes its colour from the playing track. Screens can borrow it for a moment,
 * e.g. while the pointer rests on a cover ([preview] / [clearPreview], or [roomLightPreview]).
 */
@Stable
class RoomLightController {
    /** Settings-driven switch. Off draws the plain canvas only. */
    var enabled: Boolean by mutableStateOf(true)

    /** The artwork model currently lighting the room instead of the playing track, if any. */
    var previewModel: Any? by mutableStateOf(null)
        private set

    /** Lights the room from [model]'s artwork until [clearPreview] (null clears). */
    fun preview(model: Any?) {
        previewModel = model
    }

    /** Returns the room to the playing track's colour. */
    fun clearPreview() {
        previewModel = null
    }
}

/** The shell's [RoomLightController]. Outside the shell it is an inert default instance. */
val LocalRoomLight = staticCompositionLocalOf { RoomLightController() }

/**
 * Lights the room from [model]'s artwork while the pointer is over this element, and hands it
 * back to the playing track when the pointer leaves (or the element leaves composition).
 */
fun Modifier.roomLightPreview(model: Any?): Modifier = composed {
    val controller = LocalRoomLight.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(hovered, model, controller) {
        if (hovered && model != null) {
            controller.preview(model)
        } else if (controller.previewModel == model) {
            controller.clearPreview()
        }
    }
    DisposableEffect(controller, model) {
        onDispose { if (controller.previewModel == model) controller.clearPreview() }
    }
    hoverable(interactionSource)
}

/** How long the room takes to change colour (track change or hover preview). */
private const val ROOM_TINT_MILLIS = 1200

/**
 * The drift is very slow (a few px per second), so it is stepped at ~15 fps instead of every
 * frame: the whole window recomposites on each step, and the eye can't tell the difference.
 */
private const val DRIFT_STEP_MILLIS = 66L

/**
 * Soft coloured light behind the whole wide layout: three large radial blobs tinted from the
 * playing track's cover (pulled toward the theme's cool [org.spotube.spotube.core.ui.theming.AppColors.ambient]),
 * drifting slowly, under a vertical veil of the canvas colour that darkens toward the bottom so
 * text and the dock stay readable.
 *
 * Drawn with radial gradients rather than a blur modifier: a blur over a window-sized layer costs
 * a full-screen pass every frame. Everything animated is read in the draw phase only.
 */
@Composable
fun RoomLight(
    currentCoverModel: Any?,
    modifier: Modifier = Modifier,
    controller: RoomLightController = LocalRoomLight.current
) {
    val colors = AppTheme.colors
    val canvas = colors.background
    val reduceMotion = LocalReduceMotion.current
    // With nothing playing the room still has a faint cool light rather than a flat void.
    val idle = remember(colors) { lerp(colors.ambient, canvas, 0.55f) }
    val baseTint by rememberArtworkTint(currentCoverModel, fallback = idle)
    // While a preview cover is still loading this resolves to the current room colour, so the
    // room never flashes through the fallback.
    val target by rememberArtworkTint(controller.previewModel ?: currentCoverModel, fallback = baseTint)
    val tint = animateColorAsState(
        targetValue = target,
        animationSpec = tween(ROOM_TINT_MILLIS, easing = FastOutSlowInEasing),
        label = "room_tint"
    )
    val strength = animateFloatAsState(
        targetValue = if (controller.enabled) 1f else 0f,
        animationSpec = tween(600),
        label = "room_strength"
    )
    val clock = rememberDriftClock(running = controller.enabled && !reduceMotion)

    Box(
        modifier = modifier
            // Own layer: the drift redraws only this, not the content above it.
            .graphicsLayer {}
            .drawBehind {
                drawRect(canvas)
                val s = strength.value
                if (s <= 0.001f) return@drawBehind
                val t = clock.value
                val base = colors.coolGlow(tint.value, 0.3f)
                val c1 = lerp(base, Color.White, 0.10f)
                val c3 = lerp(base, canvas, 0.5f)
                // Positions follow the mockup's room (inset -20%, blurred 80px), mapped onto the
                // window: centre and half-size as fractions, drift as a fraction of blob size.
                drawBlob(c1, s, cx = 0.22f, cy = 0.21f, rx = 0.34f, ry = 0.41f,
                    dx = 0.08f, dy = 0.10f, grow = 0.10f, phase = pingPong(t, 26f))
                drawBlob(base, s, cx = 0.89f, cy = 0.04f, rx = 0.28f, ry = 0.32f,
                    dx = -0.10f, dy = 0.08f, grow = -0.08f, phase = pingPong(t, 31f))
                drawBlob(c3, s, cx = 0.67f, cy = 0.93f, rx = 0.36f, ry = 0.35f,
                    dx = 0.06f, dy = -0.08f, grow = 0.08f, phase = pingPong(t, 35f))
                drawRect(
                    Brush.verticalGradient(
                        0f to canvas.copy(alpha = 0.10f),
                        0.52f to canvas.copy(alpha = 0.55f),
                        1f to canvas.copy(alpha = 0.92f)
                    )
                )
            }
    )
}

/** 0 → 1 → 0 with ease-in-out, one way per [periodSeconds] (CSS `alternate ease-in-out`). */
private fun pingPong(seconds: Float, periodSeconds: Float): Float =
    ((1.0 - cos(PI * seconds / periodSeconds)) / 2.0).toFloat()

private fun DrawScope.drawBlob(
    color: Color,
    strength: Float,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    dx: Float,
    dy: Float,
    grow: Float,
    phase: Float
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val growth = 1f + grow * phase
    val halfW = rx * w * growth
    val halfH = ry * h * growth
    val center = Offset(
        x = cx * w + dx * (2f * rx * w) * phase,
        y = cy * h + dy * (2f * ry * h) * phase
    )
    // Radial gradients are circular: draw a circle of the blob's half-width and squash it
    // vertically into the blob's ellipse. The extra reach stands in for the mockup's blur.
    val radius = halfW * 1.35f
    scale(scaleX = 1f, scaleY = halfH / halfW, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = 0.9f * strength),
                0.55f to color.copy(alpha = 0.5f * strength),
                1f to color.copy(alpha = 0f),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** Seconds of drift so far; frozen (and not ticking) while [running] is false. */
@Composable
private fun rememberDriftClock(running: Boolean): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val start = TimeSource.Monotonic.markNow()
        val offset = seconds.floatValue
        while (true) {
            delay(DRIFT_STEP_MILLIS)
            seconds.floatValue = offset + start.elapsedNow().inWholeMilliseconds / 1000f
        }
    }
    return seconds
}
