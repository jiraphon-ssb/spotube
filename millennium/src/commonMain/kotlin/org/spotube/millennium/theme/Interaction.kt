package org.spotube.millennium.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Maps a control state to (hover level, press level), each 0 or 1: hovered and focused give
 * `1 to 0`, pressed gives `0 to 1`, every other state `0 to 0`.
 */
internal fun millenniumInteractionLevels(state: MillenniumControlState): Pair<Float, Float> = when (state) {
    MillenniumControlState.Hovered, MillenniumControlState.Focused -> 1f to 0f
    MillenniumControlState.Pressed -> 0f to 1f
    else -> 0f to 0f
}

/**
 * Upstream draws a "glass catches light" gloss here on hover/focus/press: a white-to-accent face
 * gradient and a 1dp white rim over every control. Spotube's theme already gives each control a
 * flat hover/press plate, and the gloss on top of it is what made hover states look cheap, so this
 * fork draws nothing. Kept as a no-op (same signature) so the components need no changes.
 *
 * @receiver the surface modifier chain, returned unchanged.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun Modifier.millenniumInteractionSurface(
    state: MillenniumControlState,
    shape: Shape,
    tint: Color = MillenniumTheme.colors.accent,
    animationMillis: Int = 120,
): Modifier = this

/**
 * Gradient stops (fraction to colour) for Quiet controls: accent-tinted enamel, not a flat
 * translucent Material wash. The pressed variant is reversed (dark at the top) and stronger.
 *
 * @param colors Palette to blend toward [MillenniumColors.accent].
 * @param pressed `true` for the inverted pressed lighting.
 */
internal fun millenniumQuietEnamelStops(colors: MillenniumColors, pressed: Boolean): List<Pair<Float, Color>> {
    val top = lerp(colors.raised, colors.accent, if (pressed) 0.22f else 0.06f)
    val middle = lerp(colors.surface, colors.accent, if (pressed) 0.16f else 0.10f)
    val lower = lerp(colors.inset, colors.accent, if (pressed) 0.12f else 0.20f)
    return if (pressed) listOf(0f to lower, 0.35f to middle, 1f to top)
    else listOf(0f to top, 0.46f to middle, 0.49f to lower, 1f to middle)
}
