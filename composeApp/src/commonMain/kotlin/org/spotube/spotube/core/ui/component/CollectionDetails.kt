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
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import org.spotube.spotube.core.ui.theming.coolGlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.spotube.core.ui.component.cards.glassPlate
import org.spotube.spotube.core.ui.component.cards.isWideShell
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.modules.shell.LocalRoomLight
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
    sharedElementKey: String? = null,
    /** Song count for the meta row, when the whole list is known. */
    trackCount: Int? = null,
    /** Total length for the meta row, when the whole list is known. */
    totalDurationMs: Long? = null) {
    val colors = AppTheme.colors
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    if (!isCompact) {
        PosterCollectionHeader(
            modifier = modifier,
            title = title,
            description = description,
            imageURL = imageURL,
            imageResource = imageResource,
            ownerName = ownerName,
            onOwnerClick = onOwnerClick,
            onPlay = onPlay,
            onShufflePlay = onShufflePlay,
            onAddToQueue = onAddToQueue,
            isPlaying = isPlaying,
            isJamGuest = isJamGuest,
            onAddToJam = onAddToJam,
            isFollowing = isFollowing,
            isFollowLoading = isFollowLoading,
            isPlayLoading = isPlayLoading,
            isShuffleLoading = isShuffleLoading,
            isAddToQueueLoading = isAddToQueueLoading,
            onFollowClick = onFollowClick,
            showFollowButton = showFollowButton,
            onEdit = onEdit,
            sharedElementKey = sharedElementKey,
            trackCount = trackCount,
            totalDurationMs = totalDurationMs
        )
        return
    }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val tint by rememberArtworkTint(imageURL.ifBlank { null }, fallback = colors.ambient)

    // The cover lights the header: its colour, pulled toward the theme's cool light, falls from the
    // top and fades into the canvas. It bleeds past the list's side padding.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val glow = colors.coolGlow(tint, 0.35f)
                val bleed = 24.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to glow.copy(alpha = 0.55f),
                        0.65f to glow.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = size.height
                    ),
                    topLeft = Offset(-bleed, 0f),
                    size = Size(size.width + bleed * 2, size.height)
                )
            }
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
            artwork()
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
                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
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

/**
 * Wide playlist/album header, "poster" style: the title written huge in outline only (14% white,
 * tight tracking, clipped by the page) behind a 256dp cover with a deep shadow; beside the cover
 * the real title, a meta row (owner, songs, length), the play key, shuffle, like and "Add all to
 * queue". The outline slides and fades in once. While the page is open the room is lit from its
 * cover. The cover keeps the shared-element key, so it morphs from the tapped card.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PosterCollectionHeader(
    modifier: Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource?,
    ownerName: String,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean,
    isJamGuest: Boolean,
    onAddToJam: (() -> Unit)?,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    isPlayLoading: Boolean,
    isShuffleLoading: Boolean,
    isAddToQueueLoading: Boolean,
    onFollowClick: () -> Unit,
    showFollowButton: Boolean,
    onEdit: (() -> Unit)?,
    sharedElementKey: String?,
    trackCount: Int?,
    totalDurationMs: Long?) {
    val colors = AppTheme.colors
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val roomLight = LocalRoomLight.current
    val wide = isWideShell()

    // Opening the page lights the room from its cover (the playing track takes it back on leave).
    DisposableEffect(imageURL, wide, roomLight) {
        val model = imageURL.ifBlank { null }
        if (wide && model != null) roomLight.preview(model)
        onDispose { if (model != null && roomLight.previewModel == model) roomLight.clearPreview() }
    }

    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) appear.animateTo(1f, tween(900, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
    }

    val outlineStyle = AppTheme.typography.displayLarge.copy(
        fontSize = 200.sp,
        lineHeight = 200.sp,
        letterSpacing = (-9).sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.14f),
        drawStyle = Stroke(width = with(density) { 1.5.dp.toPx() })
    )
    val titleStyle = when {
        title.length <= 18 -> AppTheme.typography.displayLarge.copy(fontSize = 58.sp, lineHeight = 64.sp, letterSpacing = (-2).sp)
        title.length <= 36 -> AppTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp)
        else -> AppTheme.typography.headlineLarge
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 22.dp)
            .heightIn(min = 300.dp)
    ) {
        // Decorative (the real title is read below), never wraps, clipped at the page edges.
        BasicText(
            text = title,
            style = outlineStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-10).dp, y = (-40).dp)
                .clearAndSetSemantics { }
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 20.dp.toPx()
                }
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .shadow(elevation = 40.dp, shape = RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
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
            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                TextWithShimmer(
                    text = title,
                    style = titleStyle,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                PosterMetaRow(
                    ownerName = ownerName,
                    onOwnerClick = onOwnerClick,
                    trackCount = trackCount,
                    totalDurationMs = totalDurationMs,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            size = 52.dp)
                        HeaderAction(
                            icon = Glyphs.Shuffle,
                            contentDescription = stringResource(Res.string.collection_details_shuffle_play),
                            onClick = onShufflePlay,
                            loading = isShuffleLoading,
                            size = 44.dp)
                    }
                    if (showFollowButton) {
                        HeaderAction(
                            icon = if (isFollowing) Iconsax.IconsaxHeart2 else Glyphs.Heart,
                            contentDescription = stringResource(
                                if (isFollowing) Res.string.collection_details_unfollow
                                else Res.string.collection_details_follow),
                            onClick = onFollowClick,
                            loading = isFollowLoading,
                            tint = if (isFollowing) colors.primary else null,
                            size = 44.dp)
                    }
                    if (onEdit != null) {
                        HeaderAction(
                            icon = Iconsax.IconsaxEdit,
                            contentDescription = stringResource(Res.string.collection_details_edit_playlist),
                            onClick = onEdit,
                            size = 44.dp)
                    }
                    AddAllPill(onClick = onAddToQueue, loading = isAddToQueueLoading)
                }
            }
        }
    }
}

/** Owner (bold, opens the owner) · N songs · about H h M min. */
@Composable
private fun PosterMetaRow(
    ownerName: String,
    onOwnerClick: () -> Unit,
    trackCount: Int?,
    totalDurationMs: Long?,
    modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val style = AppTheme.typography.bodyMedium
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (ownerName.isNotBlank()) {
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            TextWithShimmer(
                text = ownerName,
                style = style.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .keyboardFocusRing(RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null, onClick = onOwnerClick)
                    .then(if (hovered) Modifier.background(colors.hover) else Modifier))
        }
        if (trackCount != null && trackCount > 0) {
            Text(
                text = stringResource(Res.string.pages_collection_tracks, trackCount),
                style = style,
                color = colors.onSurfaceVariant,
                maxLines = 1)
        }
        if (totalDurationMs != null && totalDurationMs > 0L) {
            val totalMinutes = (totalDurationMs / 60_000L).toInt()
            Text(
                text = if (totalMinutes >= 60) {
                    stringResource(Res.string.pages_collection_duration_hours, totalMinutes / 60, totalMinutes % 60)
                } else {
                    stringResource(Res.string.pages_collection_duration_minutes, totalMinutes.coerceAtLeast(1))
                },
                style = style,
                color = colors.onSurfaceVariant,
                maxLines = 1)
        }
    }
}

/** "Add all to queue" glass pill. */
@Composable
private fun AddAllPill(onClick: () -> Unit, loading: Boolean) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .height(42.dp)
            .keyboardFocusRing(shape)
            .glassPlate(shape)
            .background(if (hovered) colors.hover else Color.Transparent)
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !loading,
                role = Role.Button,
                onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            MillenniumCircularProgress(size = 16.dp)
        } else {
            MillenniumIcon(imageVector = Glyphs.AddTo, contentDescription = null, tint = colors.onSurface, size = 17.dp)
        }
        Text(
            text = stringResource(Res.string.pages_collection_add_all_to_queue),
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            maxLines = 1)
    }
}

/** A quiet 40dp action next to the big play key: 24dp glyph, muted until hovered. */
@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    tint: Color? = null,
    size: Dp = 40.dp) {
    MillenniumIconButton(
        onClick = onClick,
        loading = loading,
        style = MillenniumIconButtonStyle.Quiet,
        contentDescription = contentDescription,
        size = DpSize(size, size)) {
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
