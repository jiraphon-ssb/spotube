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
import spotube.composeapp.generated.resources.featured_crate_hint
import spotube.composeapp.generated.resources.featured_crate_position
import spotube.composeapp.generated.resources.featured_show_item
import spotube.composeapp.generated.resources.media_album
import spotube.composeapp.generated.resources.media_playlist
import spotube.composeapp.generated.resources.media_song
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description

/**
 * Home "featured" crate: the featured items stand in a record crate and flip forward under the
 * mouse, the facing one described by a shelf-talker card with its play key.
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
    // Start the crate fresh (first sleeve facing) when the data changes.
    key(featured) { FeaturedCrate(items = featured, modifier = modifier) }
}

/** The bits of a browse item the spotlight shows, resolved once. */
private data class FeaturedItem(
    val source: MetadataBrowseItem,
    val key: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val kind: FeaturedKind,
    /** What the shelf-talker says about it (a playlist's own description), if anything. */
    val note: String? = null
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
        kind = FeaturedKind.Playlist,
        note = data.description?.let(::plainText)?.takeIf { it.isNotBlank() }
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


/** Plugin descriptions can carry HTML (links, entities); the talker shows plain words. */
private fun plainText(html: String): String = html
    .replace(Regex("<[^>]*>"), "")
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&lt;", "<").replace("&gt;", ">")
    .trim()

/**
 * Home's featured crate: sleeves standing in a crate, the facing one described by a shelf-talker
 * (title, the playlist's own note, play). On narrow windows the talker gives way to a caption.
 */
@Composable
private fun FeaturedCrate(items: List<FeaturedItem>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val navigationCommands = koinInject<NavigationCommands>()
    var facingIndex by remember { mutableIntStateOf(0) }
    val facing = items[facingIndex.coerceIn(0, items.lastIndex)]

    fun open(item: FeaturedItem) {
        when (val src = item.source) {
            is MetadataBrowseItem.Album -> navigationCommands.navigateTo(Routes.Album(src.data.id))
            is MetadataBrowseItem.Playlist -> navigationCommands.navigateTo(Routes.Playlist(src.data.id))
            is MetadataBrowseItem.Track ->
                src.data.album?.id?.let { navigationCommands.navigateTo(Routes.Album(it)) }
            else -> Unit
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val wide = maxWidth >= 860.dp
        val compact = maxWidth < 600.dp
        CrateFrame {
            Box(modifier = Modifier.fillMaxWidth()) {
                CrateRow(
                    items = items,
                    key = { it.key },
                    cover = { it.artworkUrl },
                    label = { it.title },
                    facingIndex = facingIndex,
                    onFacingChange = { facingIndex = it },
                    onOpen = ::open,
                    faceSize = if (compact) 170.dp else 230.dp,
                    modifier = Modifier.fillMaxWidth().padding(end = if (wide) 330.dp else 0.dp)
                )
                if (wide) {
                    AnimatedContent(
                        targetState = facing,
                        transitionSpec = { fadeIn(tween(160, delayMillis = 60)) togetherWith fadeOut(tween(90)) },
                        contentKey = { it.key },
                        label = "shelf_talker",
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp)
                    ) { item ->
                        ShelfTalker(
                            title = item.title,
                            note = item.note,
                            meta = listOfNotNull(
                                stringResource(
                                    when (item.kind) {
                                        FeaturedKind.Album -> Res.string.media_album
                                        FeaturedKind.Playlist -> Res.string.media_playlist
                                        FeaturedKind.Song -> Res.string.media_song
                                    }
                                ),
                                item.subtitle?.takeIf { it.isNotBlank() }
                            ).joinToString(", "),
                            action = { FeaturedPlayButton(item = item) }
                        )
                    }
                }
            }
            // Where you are in the crate, and the facing sleeve's name when there's no talker.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!wide) {
                    FeaturedPlayButton(item = facing)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = facing.title,
                            style = AppTheme.typography.titleMedium,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        facing.subtitle?.let {
                            Text(it, style = AppTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.featured_crate_position, facingIndex + 1, items.size),
                        style = AppTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.featured_crate_hint),
                        style = AppTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Round accent play key. If this item is what's already playing it toggles pause/resume instead
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
    BigPlayButton(
        isPlaying = isPlaying,
        loading = loading,
        size = 40.dp,
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
        }
    )
}
