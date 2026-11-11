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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.CollectionPlaybackKey
import org.spotube.spotube.core.remote.PlaybackDestinationAction
import org.spotube.spotube.core.remote.RemoteCollectionType
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.featured_open
import spotube.composeapp.generated.resources.featured_show_item
import spotube.composeapp.generated.resources.media_album
import spotube.composeapp.generated.resources.media_playlist
import spotube.composeapp.generated.resources.media_song
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description

/**
 * Home "featured" spotlight.
 *
 * Replaces the old 3D coverflow (dimmed side cards, black gradient scrims, a small blurred play
 * button) with one flat card: full-strength artwork, a large title as the focal point, explicit
 * Play/Open actions and a thumbnail strip to pick another item. No dimming, gradients or blur.
 */
@Composable
fun FeaturedCarousel(
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val featured = remember(items) {
        items.mapNotNull { it.toFeatured() }
    }
    if (featured.isEmpty()) return
    FeaturedSpotlight(items = featured, modifier = modifier)
}

/** The bits of a browse item the spotlight shows, resolved once. */
private data class FeaturedItem(
    val source: MetadataBrowseItem,
    val key: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val kind: FeaturedKind
)

private enum class FeaturedKind { Album, Playlist, Song }

private fun MetadataBrowseItem.toFeatured(): FeaturedItem? = when (this) {
    is MetadataBrowseItem.Album -> FeaturedItem(
        source = this,
        key = "album:${data.id}",
        title = data.title,
        subtitle = data.artists.joinToString { it.name },
        artworkUrl = data.thumbnails.firstOrNull()?.url,
        kind = FeaturedKind.Album
    )
    is MetadataBrowseItem.Playlist -> FeaturedItem(
        source = this,
        key = "playlist:${data.id}",
        title = data.title,
        subtitle = data.owner?.displayName ?: data.owner?.username,
        artworkUrl = data.thumbnails.firstOrNull()?.url,
        kind = FeaturedKind.Playlist
    )
    is MetadataBrowseItem.Track -> FeaturedItem(
        source = this,
        key = "track:${data.id}",
        title = data.title,
        subtitle = data.artists.joinToString { it.name },
        artworkUrl = (data.album?.thumbnails ?: data.thumbnails)?.firstOrNull()?.url,
        kind = FeaturedKind.Song
    )
    else -> null
}

@Composable
private fun FeaturedSpotlight(items: List<FeaturedItem>, modifier: Modifier = Modifier) {
    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    val selected = items[selectedIndex.coerceIn(0, items.lastIndex)]
    val colors = AppTheme.colors
    val navigationCommands = koinInject<NavigationCommands>()

    fun open(item: FeaturedItem) {
        when (val src = item.source) {
            is MetadataBrowseItem.Album -> navigationCommands.navigateTo(Routes.Album(src.data.id))
            is MetadataBrowseItem.Playlist -> navigationCommands.navigateTo(Routes.Playlist(src.data.id))
            is MetadataBrowseItem.Track ->
                src.data.album?.id?.let { navigationCommands.navigateTo(Routes.Album(it)) }
            else -> Unit
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val compact = maxWidth < 600.dp
        val artSize = if (compact) 120.dp else 256.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
                .padding(if (compact) 14.dp else 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                contentKey = { it.key },
                label = "featured_artwork"
            ) { item ->
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceContainerHigh)
                        .clickable { open(item) }
                ) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.width(if (compact) 16.dp else 28.dp))

            // Desktop: the text column matches the artwork height so the strip sits on its
            // baseline. Compact: let it grow instead of clipping two-line titles.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (compact) Modifier else Modifier.height(artSize))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            when (selected.kind) {
                                FeaturedKind.Album -> Res.string.media_album
                                FeaturedKind.Playlist -> Res.string.media_playlist
                                FeaturedKind.Song -> Res.string.media_song
                            }
                        ),
                        style = AppTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (items.size > 1) {
                        Text(
                            text = "${selectedIndex + 1} / ${items.size}",
                            style = AppTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        SpotlightArrow(
                            forward = false,
                            enabled = selectedIndex > 0,
                            onClick = { selectedIndex -= 1 }
                        )
                        SpotlightArrow(
                            forward = true,
                            enabled = selectedIndex < items.lastIndex,
                            onClick = { selectedIndex += 1 }
                        )
                    }
                }

                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    contentKey = { it.key },
                    label = "featured_text"
                ) { item ->
                    Column {
                        Text(
                            text = item.title,
                            style = if (compact) AppTheme.typography.titleLarge
                            else AppTheme.typography.headlineLarge,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = AppTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeaturedPlayButton(item = selected)
                    if (selected.kind != FeaturedKind.Song ||
                        (selected.source as? MetadataBrowseItem.Track)?.data?.album != null
                    ) {
                        MillenniumButton(
                            onClick = { open(selected) },
                            style = MillenniumButtonStyle.Secondary
                        ) {
                            Text(stringResource(Res.string.featured_open))
                        }
                    }
                }

                if (items.size > 1 && !compact) {
                    Spacer(Modifier.weight(1f))
                    ThumbnailStrip(
                        items = items,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotlightArrow(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    MillenniumIconButton(
        onClick = onClick,
        enabled = enabled,
        style = MillenniumIconButtonStyle.Quiet,
        shape = RoundedCornerShape(8.dp),
        size = androidx.compose.ui.unit.DpSize(32.dp, 32.dp)
    ) {
        MillenniumIcon(
            if (forward) Glyphs.ChevronRight else Glyphs.ChevronLeft,
            contentDescription = stringResource(
                if (forward) Res.string.player_next_content_description
                else Res.string.player_previous_content_description
            ),
            tint = AppTheme.colors.onSurfaceVariant,
            size = 18.dp
        )
    }
}

/** Small artwork strip; the current item gets an accent ring, the rest sit at reduced opacity. */
@Composable
private fun ThumbnailStrip(
    items: List<FeaturedItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val isSelected = index == selectedIndex
            val description = stringResource(Res.string.featured_show_item, item.title)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .alpha(if (isSelected || hovered) 1f else 0.55f)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) colors.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(3.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.surfaceContainerHigh)
                    .hoverable(interaction)
                    .clickable(onClickLabel = description) { onSelect(index) }
            ) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = description,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Accent "Play" button. If this item is what's already playing it toggles pause/resume instead
 * of restarting it (the old card showed a pause icon but navigated away when tapped).
 */
@Composable
private fun FeaturedPlayButton(
    item: FeaturedItem,
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject()
) {
    val scope = rememberCoroutineScope()
    val currentCollection by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val busyCollections by remotePlaybackController.busyCollections.collectAsStateWithLifecycle()

    val src = item.source
    val isCurrent = when (src) {
        is MetadataBrowseItem.Album ->
            (currentCollection as? QueueCollectionEntry.Album)?.id == src.data.id
        is MetadataBrowseItem.Playlist ->
            (currentCollection as? QueueCollectionEntry.Playlist)?.id == src.data.id
        is MetadataBrowseItem.Track ->
            (currentEntry as? QueueEntry.StreamingTrack)?.track?.id == src.data.id
        else -> false
    }
    val isPlaying = isCurrent && playerState == PlayerState.PLAYING
    val loading = when (src) {
        is MetadataBrowseItem.Album -> CollectionPlaybackKey(
            RemoteCollectionType.Album, src.data.id, PlaybackDestinationAction.Play
        ) in busyCollections
        is MetadataBrowseItem.Playlist -> CollectionPlaybackKey(
            RemoteCollectionType.Playlist, src.data.id, PlaybackDestinationAction.Play
        ) in busyCollections
        else -> false
    }
    val label = stringResource(
        if (isPlaying) Res.string.player_pause_content_description
        else Res.string.player_play_content_description
    )

    MillenniumButton(
        onClick = {
            if (isCurrent) {
                scope.launch { if (isPlaying) audioPlayer.pause() else audioPlayer.play() }
            } else when (src) {
                is MetadataBrowseItem.Album -> remotePlaybackController.requestCollectionPlay(
                    RemoteCollectionType.Album, src.data.id, src.data.title
                )
                is MetadataBrowseItem.Playlist -> remotePlaybackController.requestCollectionPlay(
                    RemoteCollectionType.Playlist, src.data.id, src.data.title
                )
                is MetadataBrowseItem.Track -> scope.launch {
                    audioPlayerQueue.load(
                        entries = listOf(QueueEntry.StreamingTrack(track = src.data, url = "")),
                        autoPlay = true,
                        startPosition = 0,
                        collectionEntry = null
                    )
                }
                else -> Unit
            }
        },
        loading = loading,
        style = MillenniumButtonStyle.Primary
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MillenniumIcon(
                if (isPlaying) Glyphs.Pause else Glyphs.Play,
                contentDescription = null,
                tint = AppTheme.colors.onAccentFill,
                size = 18.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}
