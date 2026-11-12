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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import com.composeunstyled.currentWindowWidthBreakpoint
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.VinylDisc
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import org.spotube.spotube.resources.glyphs.Glyphs
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.User
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetails(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    /** Guest in a jam session: play/shuffle are replaced by Add to Jam. */
    isJamGuest: Boolean = false,
    onAddToJam: (() -> Unit)? = null,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    isPlayLoading: Boolean = false,
    isShuffleLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false,
    onFollowClick: () -> Unit = { },
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null) {
    val colors = AppTheme.colors
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    // The sleeve pulled out of the crate: no box, no wash, the artwork and its record carry it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 8.dp else 16.dp)
    ) {
        val artworkSize = if (isCompact) 120.dp else 208.dp
        val artwork = @Composable {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
                    .sharedElementOrNone(
                        key = sharedElementKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope)
                    .shimmerApply(),
                contentAlignment = Alignment.Center) {
                when {
                    imageURL.isNotBlank() -> AsyncImage(
                        model = imageURL,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                    imageResource != null -> Image(
                        painter = painterResource(imageResource),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                    else -> Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = AppTheme.typography.displaySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        }

        // The title is the hero: as large as its length allows.
        val titleStyle = when {
            isCompact -> AppTheme.typography.headlineSmall
            title.length <= 22 -> AppTheme.typography.displayLarge
            title.length <= 40 -> AppTheme.typography.displaySmall
            else -> AppTheme.typography.headlineLarge
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isCompact) 16.dp else 32.dp, bottom = if (isCompact) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 16.dp else 24.dp),
            verticalAlignment = Alignment.Bottom) {
            if (isCompact) {
                artwork()
            } else {
                SleeveWithRecord(
                    isPlaying = isPlaying,
                    labelModel = imageURL.ifBlank { null },
                    sleeveSize = artworkSize,
                    sleeve = artwork
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextWithShimmer(
                    text = title,
                    style = titleStyle,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                OwnerLine(ownerName = ownerName, ownerImageURL = ownerImageURL, onOwnerClick = onOwnerClick)
                // The description reads as the owner's own note, by hand.
                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = AppTheme.typography.hand,
                        color = colors.kraft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (isJamGuest) {
                if (onAddToJam != null) {
                    MillenniumButton(onClick = onAddToJam) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxAddSquare,
                            contentDescription = null)
                        TextWithShimmer(text = stringResource(Res.string.track_options_add_to_jam))
                    }
                }
            } else {
                BigPlayButton(
                    isPlaying = isPlaying,
                    loading = isPlayLoading,
                    onClick = onPlay,
                    modifier = Modifier.padding(end = 8.dp))
                HeaderAction(
                    icon = Glyphs.Shuffle,
                    contentDescription = stringResource(Res.string.collection_details_shuffle_play),
                    onClick = onShufflePlay,
                    loading = isShuffleLoading)
            }
            HeaderAction(
                icon = Glyphs.AddTo,
                contentDescription = stringResource(Res.string.player_add_to_queue_content_description),
                onClick = onAddToQueue,
                loading = isAddToQueueLoading)
            if (showFollowButton) {
                HeaderAction(
                    icon = if (isFollowing) Iconsax.IconsaxHeart2 else Glyphs.Heart,
                    contentDescription = stringResource(
                        if (isFollowing) Res.string.collection_details_unfollow
                        else Res.string.collection_details_follow),
                    onClick = onFollowClick,
                    loading = isFollowLoading,
                    tint = if (isFollowing) colors.primary else null)
            }
            if (onEdit != null) {
                HeaderAction(
                    icon = Iconsax.IconsaxEdit,
                    contentDescription = stringResource(Res.string.collection_details_edit_playlist),
                    onClick = onEdit)
            }
        }
    }
}

/** One revolution at 33⅓ rpm, as in the full-window player. */
private const val RevolutionMs = 1800

/**
 * The sleeve with its record behind it. The record peeks out at rest and slides further out and
 * spins while this collection is playing; on pause it coasts to a stop and tucks back in.
 */
@Composable
private fun SleeveWithRecord(
    isPlaying: Boolean,
    labelModel: Any?,
    sleeveSize: Dp,
    sleeve: @Composable () -> Unit
) {
    val slide by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "record_slide"
    )
    val spin = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        spin.snapTo(spin.value % 360f)
        if (isPlaying) {
            while (isActive) spin.animateTo(spin.value + 360f, tween(RevolutionMs, easing = LinearEasing))
        } else {
            spin.animateTo(spin.value + 30f, tween(600, easing = LinearOutSlowInEasing))
        }
    }
    val disc = sleeveSize * 0.94f
    val travel = sleeveSize * 0.46f
    Box(modifier = Modifier.width(sleeveSize + travel).height(sleeveSize)) {
        Box(
            modifier = Modifier
                .size(disc)
                .align(Alignment.CenterStart)
                .offset { IntOffset(((sleeveSize - disc) / 2 + travel * slide).roundToPx(), 0) }
        ) {
            VinylDisc(artwork = labelModel, rotation = { spin.value })
        }
        sleeve()
    }
}

/** A quiet 40dp action next to the big play key: 24dp glyph, muted until hovered. */
@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    tint: Color? = null) {
    MillenniumIconButton(
        onClick = onClick,
        loading = loading,
        style = MillenniumIconButtonStyle.Quiet,
        contentDescription = contentDescription,
        size = DpSize(40.dp, 40.dp)) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: AppTheme.colors.onSurfaceVariant,
            size = 24.dp)
    }
}

@Composable
private fun OwnerLine(ownerName: String, ownerImageURL: String?, onOwnerClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onOwnerClick),
        verticalAlignment = Alignment.CenterVertically) {
        if (ownerImageURL != null) {
            AsyncImage(
                model = ownerImageURL,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(CircleShape).shimmerApply(),
                contentScale = ContentScale.Crop)
        } else {
            MillenniumIcon(
                imageVector = Iconsax.User,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .background(AppTheme.colors.surfaceContainerHigh, CircleShape)
                    .padding(4.dp),
                tint = AppTheme.colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        TextWithShimmer(
            text = stringResource(Res.string.collection_details_by_owner, ownerName),
            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CollectionDetailsPreview() {
    CollectionDetails(
        title = "My Playlist",
        description = "A collection of my favorite songs.",
        imageURL = "https://example.com/playlist.jpg",
        ownerName = "John Doe",
        ownerImageURL = "https://example.com/john.jpg",
        onOwnerClick = {},
        onPlay = {},
        onShufflePlay = {},
        onAddToQueue = {})
}
