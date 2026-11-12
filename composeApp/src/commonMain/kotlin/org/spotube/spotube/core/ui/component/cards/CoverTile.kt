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

package org.spotube.spotube.core.ui.component.cards

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.component.BigPlayButton
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.PlayingBars
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.modules.shell.roomLightPreview
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_add_to_queue_content_description

/** Dark ink used under glass chips that sit on artwork (always dark, whatever the cover). */
private val OnArtInk = Color(0xFF06090D)

/**
 * A large cover tile for Home and Library grids: 14dp-radius artwork with a soft shadow, title and
 * a muted line under it. Pointing at the tile relights the window from its cover and raises a
 * small "add to queue" glass circle (left) and the play key (right) out of the cover's bottom edge;
 * keyboard focus inside the tile does the same, so both stay reachable with Tab.
 *
 * [isCurrent] marks the collection that is playing now: an accent outline and a "Playing" tag.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CoverTile(
    title: String,
    subtitle: String?,
    imageModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false,
    sharedElementKey: String? = null,
    /** Shown as a "Playing" tag at the cover's bottom-left while [isCurrent]. */
    playingLabel: String? = null,
    /** Small pills or badges in the cover's top-left corner (e.g. "New"). */
    topStart: (@Composable () -> Unit)? = null,
    /** Replaces the cover image (e.g. a bundled drawable). */
    artwork: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    val showActions = hovered || focusWithin || touch || playLoading || addToQueueLoading
    val reveal by animateFloatAsState(
        targetValue = if (showActions) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 220),
        label = "tile_reveal"
    )
    val elevation by animateDpAsState(if (hovered) 22.dp else 12.dp, tween(200), label = "tile_shadow")
    val shape = if (circle) CircleShape else RoundedCornerShape(14.dp)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val accent = colors.primary

    Column(
        modifier = modifier
            .onFocusChanged { focusWithin = it.hasFocus }
            .focusGroup()
            .hoverable(interaction)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // The playing collection: accent outline 3dp outside the cover.
                .then(
                    if (isCurrent) Modifier.drawBehind {
                        val gap = 3.dp.toPx()
                        val stroke = 2.dp.toPx()
                        val inflate = gap + stroke / 2f
                        val radius = if (circle) size.minDimension / 2f + inflate else 14.dp.toPx() + inflate
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(-inflate, -inflate),
                            size = Size(size.width + inflate * 2f, size.height + inflate * 2f),
                            cornerRadius = CornerRadius(radius, radius),
                            style = Stroke(width = stroke)
                        )
                    } else Modifier
                )
                .keyboardFocusRing(shape)
                .shadow(elevation, shape, clip = false)
                .clip(shape)
                .background(colors.surfaceContainerHighest)
                .sharedElementOrNone(
                    key = sharedElementKey,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .roomLightPreview(imageModel)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = onClick
                )
        ) {
            if (artwork != null) {
                artwork()
            } else if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = AppTheme.typography.headlineMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (topStart != null) {
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { topStart() }
            }

            if (isCurrent && playingLabel != null && !circle) {
                NowPlayingTag(
                    label = playingLabel,
                    playing = isPlaying,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .graphicsLayer { alpha = 1f - reveal }
                )
            }

            if (onPlay != null || onAddToQueue != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .graphicsLayer {
                            alpha = reveal
                            translationY = (1f - reveal) * 6.dp.toPx()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onAddToQueue != null && !circle) {
                        QueueAddCircle(onClick = onAddToQueue, loading = addToQueueLoading)
                    } else {
                        Box(Modifier.size(1.dp))
                    }
                    if (onPlay != null) {
                        BigPlayButton(
                            isPlaying = isPlaying,
                            loading = playLoading,
                            onClick = onPlay,
                            size = 42.dp
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            style = AppTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isCurrent) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = AppTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The small dark glass "add to queue" circle that sits on artwork. */
@Composable
fun QueueAddCircle(onClick: () -> Unit, loading: Boolean = false, modifier: Modifier = Modifier) {
    MillenniumIconButton(
        onClick = onClick,
        loading = loading,
        modifier = modifier
            .keyboardFocusRing(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        style = MillenniumIconButtonStyle.Quiet,
        background = SolidColor(OnArtInk.copy(alpha = 0.62f)),
        shape = CircleShape,
        size = DpSize(34.dp, 34.dp),
        contentDescription = stringResource(Res.string.player_add_to_queue_content_description)
    ) {
        MillenniumIcon(
            imageVector = Glyphs.AddTo,
            contentDescription = null,
            tint = Color.White,
            size = 15.dp
        )
    }
}

/** "Playing" tag with live bars, on a dark glass pill in the accent colour. */
@Composable
fun NowPlayingTag(label: String, playing: Boolean, modifier: Modifier = Modifier) {
    val accent = AppTheme.colors.primary
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(OnArtInk.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayingBars(color = accent, playing = playing, modifier = Modifier.size(10.dp))
        Text(
            text = label,
            style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
            color = accent,
            maxLines = 1
        )
    }
}

/** A small dark glass pill on artwork ("New"). */
@Composable
fun ArtPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(OnArtInk.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
