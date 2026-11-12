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


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import org.spotube.spotube.core.ui.component.BigPlayButton
import org.spotube.spotube.resources.glyphs.Glyphs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_add_to_queue_content_description
import spotube.composeapp.generated.resources.media_preview_title
import spotube.composeapp.generated.resources.media_preview_subtitle

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayableCard(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
    listMode: Boolean = false,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false
) {
    if (listMode) {
        PlayableListTile(
            title,
            subtitle,
            imageURL,
            isPlaying,
            onClick,
            onPlay,
            onAddToQueue,
            modifier,
            sharedElementKey,
            playLoading = playLoading,
            addToQueueLoading = addToQueueLoading
        )
        return
    }
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    MediaCardSurface(
        modifier = modifier.width(160.dp),
        onClick = onClick ?: onPlay,
        interactive = onClick != null || onPlay != null || onAddToQueue != null
    ) { highlighted ->
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().shimmerApply()) {
                MediaArtwork(
                    model = imageURL,
                    title = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .sharedElementOrNone(
                            key = sharedElementKey,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (onPlay != null || onAddToQueue != null)
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // The play key rises out of the artwork's corner on hover (always shown on
                        // touch, and while this collection plays or is starting).
                        this@Column.AnimatedVisibility(
                            visible = highlighted || touchInput || (isPlaying && onPlay != null) ||
                                playLoading || addToQueueLoading,
                            enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 3 },
                            exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 3 }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // While this collection is playing only the play/pause key stays.
                                onAddToQueue?.takeIf { !isPlaying }?.let {
                                    MillenniumIconButton(
                                        onClick = onAddToQueue,
                                        loading = addToQueueLoading,
                                        style = MillenniumIconButtonStyle.Quiet,
                                        background = SolidColor(Color.Black.copy(alpha = 0.6f)),
                                        shape = CircleShape,
                                        size = DpSize(36.dp, 36.dp),
                                        contentDescription = stringResource(
                                            Res.string.player_add_to_queue_content_description
                                        )
                                    ) {
                                        MillenniumIcon(
                                            imageVector = Glyphs.AddTo,
                                            contentDescription = null,
                                            tint = Color.White,
                                            size = 18.dp
                                        )
                                    }
                                }
                                onPlay?.let {
                                    BigPlayButton(
                                        isPlaying = isPlaying,
                                        loading = playLoading,
                                        onClick = onPlay,
                                        size = 44.dp,
                                        modifier = Modifier.shadow(10.dp, CircleShape)
                                    )
                                }
                            }
                        }
                    }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 4.dp)
            ) {
                TextWithShimmer(
                    text = title,
                    style = AppTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                subtitle?.let {
                    TextWithShimmer(
                        text = subtitle,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

    }
}

@PreviewLightDark
@Composable
private fun PlayableCardPreview() {
    Box {
        PlayableCard(
            title = stringResource(Res.string.media_preview_title),
            subtitle = stringResource(Res.string.media_preview_subtitle),
            imageURL = "https://placehold.co/600x400",
            isPlaying = true,
            onClick = {},
            onPlay = {},
            onAddToQueue = {}
        )
    }
}
