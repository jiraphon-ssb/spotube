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

package org.spotube.spotube.core.ui.theming

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.compose.rememberAsyncImagePainter
import kotlin.math.max
import kotlin.math.min

/** Side of the thumbnail the artwork is averaged over. Tiny on purpose: we only want the mood. */
private const val SAMPLE = 12

/** Max covers remembered; oldest are dropped first. */
private const val CACHE_SIZE = 256

/**
 * Process-wide cache: the same cover appears on many screens. A plain map on purpose: snapshot
 * state here would recompose every tinted screen on each write. Only touched on the main thread.
 */
private val tintCache = object : LinkedHashMap<Any, Color>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, Color>?): Boolean = size > CACHE_SIZE
}

/**
 * A colour taken from [model]'s artwork, tuned to sit behind white text on the dark canvas:
 * the average hue of the cover, saturation kept, lightness clamped into a deep, rich band.
 *
 * Used for the artwork-tinted washes (collection/artist headers, the full-window player).
 * Returns [fallback] until the image has loaded (and when it fails), animating between values.
 */
@Composable
fun rememberArtworkTint(model: Any?, fallback: Color = AppTheme.colors.surfaceContainerHigh): State<Color> {
    // Explicit small size: a painter that is never drawn would otherwise wait for its layout size
    // and never load.
    val context = LocalPlatformContext.current
    val request = remember(model, context) {
        ImageRequest.Builder(context).data(model).size(64).softwareBitmaps().build()
    }
    val painter = rememberAsyncImagePainter(request)
    val state by painter.state.collectAsState()
    val key = model ?: Unit
    val computed = (state as? AsyncImagePainter.State.Success)?.let { success ->
        remember(key, success) { averageColor(success.painter)?.let(::moodColor) }
    }
    // Cache outside composition (writing the map we just read would force an extra pass).
    SideEffect { if (computed != null && tintCache[key] == null) tintCache[key] = computed }
    val extracted = tintCache[key] ?: computed
    return animateColorAsState(extracted ?: fallback, tween(400), label = "artwork_tint")
}

/** Draws [painter] into a SAMPLE×SAMPLE bitmap and averages its pixels (ARGB ints). */
private fun averageColor(painter: Painter): Color? = runCatching {
    val bitmap = ImageBitmap(SAMPLE, SAMPLE)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(SAMPLE.toFloat(), SAMPLE.toFloat())
    ) {
        with(painter) { draw(Size(SAMPLE.toFloat(), SAMPLE.toFloat())) }
    }
    val pixels = IntArray(SAMPLE * SAMPLE)
    bitmap.readPixels(pixels)
    var r = 0L
    var g = 0L
    var b = 0L
    pixels.forEach { argb ->
        r += (argb shr 16) and 0xFF
        g += (argb shr 8) and 0xFF
        b += argb and 0xFF
    }
    val n = pixels.size
    Color(red = (r / n).toInt(), green = (g / n).toInt(), blue = (b / n).toInt())
}.getOrNull()

/**
 * Keeps the cover's hue and most of its saturation but forces lightness into 22–34% so a
 * wash of it never washes out white text or turns into mud.
 */
private fun moodColor(c: Color): Color {
    val r = c.red
    val g = c.green
    val b = c.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d < 0.0001f) return Color.hsl(0f, 0f, 0.22f)
    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when (mx) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Color.hsl(h, (s * 0.9f).coerceIn(0f, 0.85f), l.coerceIn(0.22f, 0.34f))
}
