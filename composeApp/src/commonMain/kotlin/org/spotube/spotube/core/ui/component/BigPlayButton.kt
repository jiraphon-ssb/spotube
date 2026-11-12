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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LoadingRing
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description

/**
 * The one loud control on a collection or artist page: a round accent key with an ink glyph.
 * It grows slightly and brightens under the pointer.
 */
@Composable
fun BigPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    size: Dp = 56.dp,
    /** Overrides the default "Play"/"Pause" label, e.g. "Play <title>". */
    contentDescription: String? = null
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.04f else 1f,
        animationSpec = tween(140),
        label = "big_play_scale"
    )
    val ink = colors.onAccentFill
    val defaultLabel = stringResource(
        if (isPlaying) Res.string.player_pause_content_description
        else Res.string.player_play_content_description
    )
    val label = contentDescription ?: defaultLabel
    MillenniumIconButton(
        // Ignore presses while a start is in flight so one click can't queue two plays.
        onClick = { if (!loading) onClick() },
        modifier = modifier
            // The Quiet style's own focus ring is accent on an accent key (invisible); draw a
            // text-coloured one outside it. Before graphicsLayer so the ring doesn't scale.
            .keyboardFocusRing(CircleShape)
            .hoverable(interaction)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        style = MillenniumIconButtonStyle.Quiet,
        contentDescription = label,
        background = if (hovered) colors.accentBrushHover else colors.accentBrush,
        foreground = ink,
        shape = CircleShape,
        size = DpSize(size, size)) {
        if (loading) {
            LoadingRing(color = ink, size = size * 0.4f)
        } else {
            MillenniumIcon(
                imageVector = if (isPlaying) Glyphs.Pause else Glyphs.Play,
                contentDescription = null,
                tint = ink,
                size = size * 0.4f
            )
        }
    }
}
