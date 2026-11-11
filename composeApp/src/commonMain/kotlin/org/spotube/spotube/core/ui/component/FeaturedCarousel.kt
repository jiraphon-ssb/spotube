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
import org.spotube.spotube.core.ui.coverflow.rememberCoverflowState
import org.spotube.spotube.core.ui.coverflow.CoverflowParams
import org.spotube.spotube.core.ui.coverflow.Coverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.key
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
 * Home "featured" coverflow.
 *
 * Keeps the 3D coverflow but takes everything off the artwork: no black gradient scrim, no small
 * blurred play button, no text printed over covers. The centred item's title, artist and explicit
 * Play/Open actions sit underneath it instead, with visible arrows and a position counter.
 */
@Composable
fun FeaturedCarousel(
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    // Lazy keys must be unique; browse data can repeat an album/track across sections.
    val featured = remember(items) {
        items.mapNotNull { it.toFeatured() }.distinctBy { it.key }
    }
    if (featured.isEmpty()) return
    // The coverflow state captures its select handler once, so start fresh when the data changes.
    key(featured) { FeaturedCoverflow(items = featured, modifier = modifier) }
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
private fun FeaturedCoverflow(items: List<FeaturedItem>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val navigationCommands = koinInject<NavigationCommands>()
    val startIndex = remember { items.size / 2 }
    var selectedIndex by remember { mutableIntStateOf(startIndex) }
    // The coverflow reports -1 while nothing is exactly centred (mid-scroll); keep the last item.
    val state = rememberCoverflowState(startIndex) { index -> if (index >= 0) selectedIndex = index }
    val selected = items[selectedIndex.coerceIn(0, items.lastIndex)]

    fun open(item: FeaturedItem) {
        when (val src = item.source) {
            is MetadataBrowseItem.Album -> navigationCommands.navigateTo(Routes.Album(src.data.id))
            is MetadataBrowseItem.Playlist -> navigationCommands.navigateTo(Routes.Playlist(src.data.id))
            is MetadataBrowseItem.Track ->
                src.data.album?.id?.let { navigationCommands.navigateTo(Routes.Album(it)) }
            else -> Unit
        }
    }

    fun go(index: Int) {
        if (index in items.indices) state.scrollToItem(index)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        val compact = maxWidth < 600.dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 200.dp else 280.dp),
                contentAlignment = Alignment.Center
            ) {
                Coverflow(
                    state = state,
                    params = CoverflowParams(
                        size = 1f,
                        offset = 0.38f,
                        angle = 12f,
                        shift = 0.255f,
                        zoom = 0.7864f,
                        mirror = false
                    )
                ) {
                    items(
                        count = items.size,
                        key = { index -> items[index].key }
                    ) { index ->
                        CoverflowCard(
                            item = items[index],
                            isCentered = index == selectedIndex,
                            onClick = {
                                if (index == selectedIndex) open(items[index]) else go(index)
                            }
                        )
                    }
                }

                if (!compact && items.size > 1) {
                    CoverflowArrow(
                        forward = false,
                        enabled = selectedIndex > 0,
                        onClick = { go(selectedIndex - 1) },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                    )
                    CoverflowArrow(
                        forward = true,
                        enabled = selectedIndex < items.lastIndex,
                        onClick = { go(selectedIndex + 1) },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

            // Details of the centred item, under it rather than printed over the artwork.
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                contentKey = { it.key },
                label = "featured_details"
            ) { item ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            when (item.kind) {
                                FeaturedKind.Album -> Res.string.media_album
                                FeaturedKind.Playlist -> Res.string.media_playlist
                                FeaturedKind.Song -> Res.string.media_song
                            }
                        ) + if (items.size > 1) "  ${selectedIndex + 1} / ${items.size}" else "",
                        style = AppTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = item.title,
                        style = if (compact) AppTheme.typography.titleLarge
                        else AppTheme.typography.headlineMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (!item.subtitle.isNullOrBlank()) {
                        Text(
                            text = item.subtitle,
                            style = AppTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One cover in the coverflow: artwork only. Off-centre covers are dimmed a little (less than the
 * old 50%) so depth reads without turning them into black tiles.
 */
@Composable
private fun CoverflowCard(item: FeaturedItem, isCentered: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val dim by animateFloatAsState(if (isCentered) 0f else 0.35f, tween(220), label = "cover_dim")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .clickable(onClickLabel = item.title, onClick = onClick)
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (dim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
    }
}

@Composable
private fun CoverflowArrow(
    forward: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Box(modifier = modifier) {
        MillenniumIconButton(
            onClick = onClick,
            enabled = enabled,
            style = MillenniumIconButtonStyle.Quiet,
            background = SolidColor(colors.surface.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(50),
            size = DpSize(40.dp, 40.dp)
        ) {
            MillenniumIcon(
                if (forward) Glyphs.ChevronRight else Glyphs.ChevronLeft,
                contentDescription = stringResource(
                    if (forward) Res.string.player_next_content_description
                    else Res.string.player_previous_content_description
                ),
                tint = colors.onSurface,
                size = 20.dp
            )
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
