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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import kotlinx.coroutines.isActive
import org.spotube.spotube.core.ui.theming.AppTheme

/** One revolution at 33⅓ rpm. */
private const val REVOLUTION_MS = 1800

/** Where the record rests: fully out while playing, half tucked into the sleeve when paused. */
private const val OUT_PLAYING = 1f
private const val OUT_PAUSED = 0.55f

/**
 * The full-window player's artwork, staged as a record and its sleeve.
 *
 * - On open, and on every track change, the record slides back into the sleeve and out again
 *   carrying the new label (the label is the track's artwork).
 * - Playing: the record is fully out and spins at 33⅓ rpm. Paused: it coasts to a stop and
 *   tucks halfway back into the sleeve, so the state is readable at a glance.
 * - The light sheen is drawn on a layer that does not rotate, like light on a real record.
 *
 * Clicking the sleeve opens the album.
 */
@Composable
internal fun VinylArtwork(
    player: PlayerController,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isPlaying = player.ui.isPlaying
    // Streaming entries all carry url = "", so key on the track id (local files: their path).
    val entryKey: Any? = player.currentTrack?.id ?: player.ui.currentQueueEntry?.url
    val playingNow by rememberUpdatedState(isPlaying)
    val lastEntry = remember { arrayOf<Any?>(Unit) }
    val albumId = player.currentTrack?.album?.id
    val slide = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }

    // One driver for the slide: on a new track (or first open) tuck the record in, then bring
    // it out with the new label; on play/pause just move to that state's resting point.
    LaunchedEffect(entryKey, isPlaying) {
        val trackChanged = lastEntry[0] != entryKey
        lastEntry[0] = entryKey
        if (trackChanged && slide.value > 0.05f) {
            slide.animateTo(0f, tween(220, easing = FastOutLinearInEasing))
        }
        slide.animateTo(
            if (playingNow) OUT_PLAYING else OUT_PAUSED,
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
        )
    }
    // Spin while playing; coast to a stop on pause.
    LaunchedEffect(isPlaying) {
        spin.snapTo(spin.value % 360f)
        if (isPlaying) {
            while (isActive) {
                spin.animateTo(spin.value + 360f, tween(REVOLUTION_MS, easing = LinearEasing))
            }
        } else {
            spin.animateTo(spin.value + 30f, tween(600, easing = LinearOutSlowInEasing))
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // Sleeve + the part of the record that can stick out must fit the available width.
        val sleeve = min(maxWidth / 1.42f, 420.dp)
        val disc = sleeve * 0.96f
        val travel = sleeve * 0.42f

        Box(modifier = Modifier.width(sleeve + travel).height(sleeve)) {
            // The record, behind the sleeve.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = (sleeve - disc) / 2)
                    .size(disc)
                    .offset {
                        IntOffset(
                            x = ((sleeve - disc) / 2 + travel * slide.value).roundToPx(),
                            y = 0
                        )
                    }
            ) {
                // Rotation is read in the layer, so spinning redraws instead of recomposing.
                VinylDisc(artwork = player.ui.coverModel, rotation = { spin.value })
            }

            // The sleeve (artwork), on top.
            Box(
                modifier = Modifier
                    .size(sleeve)
                    .shadow(24.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(enabled = albumId != null) { albumId?.let(onOpenAlbum) }
            ) {
                AsyncImage(
                    model = player.ui.coverModel,
                    contentDescription = player.ui.album,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // The sleeve's open edge, where the record comes out.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
internal fun VinylDisc(artwork: Any?, rotation: () -> Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Rotating layer: vinyl, grooves, label, spindle hole.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation() }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(color = Color(0xFF0B0B0D), radius = r)
                // Grooves: fine rings, every third a touch brighter so the spin is visible.
                var ring = r * 0.97f
                var i = 0
                while (ring > r * 0.40f) {
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 3 == 0) 0.07f else 0.03f),
                        radius = ring,
                        style = Stroke(width = 1f)
                    )
                    ring -= r * 0.022f
                    i++
                }
                // Run-out ring around the label.
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = r * 0.385f,
                    style = Stroke(width = 1.5f)
                )
            }
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.36f)
                    .align(Alignment.Center)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0D))
            )
        }

        // Fixed sheen: two soft light wedges that stay put while the record turns.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2f, r * 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = -70f,
                sweepAngle = 28f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = Color.White.copy(alpha = 0.04f),
                startAngle = 110f,
                sweepAngle = 28f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }
    }
}
