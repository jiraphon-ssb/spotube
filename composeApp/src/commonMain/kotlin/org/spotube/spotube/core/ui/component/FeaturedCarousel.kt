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
import spotube.composeapp.generated.resources.home_more_picks
import spotube.composeapp.generated.resources.featured_next
import spotube.composeapp.generated.resources.featured_previous
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import org.spotube.spotube.core.ui.theming.coolGlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import spotube.composeapp.generated.resources.a11y_play_item
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import spotube.composeapp.generated.resources.featured_show_item
import spotube.composeapp.generated.resources.media_album
import spotube.composeapp.generated.resources.media_playlist
import spotube.composeapp.generated.resources.media_song
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description

/**
 * Home "featured" area: a hero card for one featured item and quick tiles for the next few.
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
    // Start fresh (first item as the hero) when the data changes.
    key(featured) { FeaturedHero(items = featured, modifier = modifier) }
}

/** The bits of a browse item the spotlight shows, resolved once. */
internal data class FeaturedItem(
    val source: MetadataBrowseItem,
    val key: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val kind: FeaturedKind,
    /** What the shelf-talker says about it (a playlist's own description), if anything. */
    val note: String? = null
)

internal enum class FeaturedKind { Album, Playlist, Song }

internal fun MetadataBrowseItem.toFeatured(): FeaturedItem? = when (this) {
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
internal fun plainText(html: String): String = html
    .replace(Regex("<[^>]*>"), "")
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&lt;", "<").replace("&gt;", ">")
    .trim()

/** Opens a featured item's page (a song opens its album). */
internal fun FeaturedItem.open(navigationCommands: NavigationCommands) {
    when (val src = source) {
        is MetadataBrowseItem.Album -> navigationCommands.navigateTo(Routes.Album(src.data.id))
        is MetadataBrowseItem.Playlist -> navigationCommands.navigateTo(Routes.Playlist(src.data.id))
        is MetadataBrowseItem.Track ->
            src.data.album?.id?.let { navigationCommands.navigateTo(Routes.Album(it)) }
        else -> Unit
    }
}

/**
 * Adds a featured item to the queue through the same path the cards use (the destination picker
 * decides between this device and a remote one). Returns false for items that can't be queued.
 */
internal fun FeaturedItem.addToQueue(remotePlaybackController: RemotePlaybackController): Boolean =
    when (val src = source) {
        is MetadataBrowseItem.Album -> {
            remotePlaybackController.requestCollectionAddToQueue(RemoteCollectionType.Album, src.data.id, src.data.title)
            true
        }
        is MetadataBrowseItem.Playlist -> {
            remotePlaybackController.requestCollectionAddToQueue(RemoteCollectionType.Playlist, src.data.id, src.data.title)
            true
        }
        is MetadataBrowseItem.Track -> {
            remotePlaybackController.requestTrackAddToQueue(src.data)
            true
        }
        else -> false
    }

/**
 * Home's featured area: one large hero card lit by its own cover (title, note, play, open, and
 * arrows to step through the featured items), and beside it a grid of quick tiles for the next
 * few. On narrow windows the tiles move under the hero.
 */
@Composable
private fun FeaturedHero(items: List<FeaturedItem>, modifier: Modifier = Modifier) {
    val navigationCommands = koinInject<NavigationCommands>()
    var heroIndex by remember { mutableIntStateOf(0) }
    val hero = items[heroIndex.coerceIn(0, items.lastIndex)]
    val picks = remember(items, heroIndex) {
        (items.drop(heroIndex + 1) + items.take(heroIndex)).take(6)
    }

    fun open(item: FeaturedItem) = item.open(navigationCommands)

    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val wide = maxWidth >= 900.dp
        val heroCard = @Composable { mod: Modifier ->
            HeroCard(
                item = hero,
                index = heroIndex,
                count = items.size,
                onStep = { heroIndex = (heroIndex + it).mod(items.size) },
                onOpen = { open(hero) },
                compact = maxWidth < 600.dp,
                modifier = mod
            )
        }
        val quickPicks = @Composable { mod: Modifier ->
            if (picks.isNotEmpty()) QuickPicks(items = picks, onOpen = ::open, modifier = mod)
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                heroCard(Modifier.weight(1.25f))
                quickPicks(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                heroCard(Modifier.fillMaxWidth())
                quickPicks(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HeroCard(
    item: FeaturedItem,
    index: Int,
    count: Int,
    onStep: (Int) -> Unit,
    onOpen: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val tint by rememberArtworkTint(item.artworkUrl, fallback = colors.ambient)
    val shape = RoundedCornerShape(14.dp)
    val pad = if (compact) 16.dp else 32.dp
    BoxWithConstraints(
        modifier = modifier
            .height(if (compact) 224.dp else 300.dp)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            // The cover's light pools in the corner behind it.
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.coolGlow(tint, 0.3f).copy(alpha = 0.75f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.35f),
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
            .border(1.dp, colors.outline, shape)
    ) {
        // The art scales with the card (a ~470dp card at 900–1100dp windows would otherwise
        // leave the title ~140dp); the title steps down when the text column gets narrow.
        val artSize = if (compact) 120.dp else (maxWidth * 0.36f).coerceIn(120.dp, 236.dp)
        val textWidth = maxWidth - artSize - pad * 3
        val titleStyle = if (!compact && textWidth >= 280.dp) {
            AppTheme.typography.displaySmall
        } else {
            AppTheme.typography.titleLarge
        }
        AnimatedContent(
            targetState = item,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            contentKey = { it.key },
            label = "hero_art",
            modifier = Modifier.align(Alignment.TopEnd).padding(pad)
        ) { shown ->
            Box(
                modifier = Modifier
                    .size(artSize)
                    .shadow(24.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerHighest)
                    // A pointer shortcut only: the "Open" button is the keyboard/reader path, so
                    // the cover is not a second tab stop saying the same thing.
                    .focusProperties { canFocus = false }
                    .clickable(onClickLabel = shown.title, onClick = onOpen)
            ) {
                AsyncImage(
                    model = shown.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(
            // The text takes whatever the cover leaves, so the two never overlap.
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = pad, bottom = pad, end = artSize + pad * 2)
        ) {
            Text(
                text = item.title,
                style = titleStyle,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val line = item.note ?: item.subtitle
            if (!line.isNullOrBlank()) {
                Text(
                    text = line,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturedPlayButton(
                    item = item,
                    size = 48.dp,
                    contentDescription = stringResource(Res.string.a11y_play_item, item.title)
                )
                MillenniumButton(onClick = onOpen, style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.featured_open), maxLines = 1)
                }
            }
        }
        if (count > 1) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverflowArrow(forward = false, enabled = true, onClick = { onStep(-1) })
                CoverflowArrow(forward = true, enabled = true, onClick = { onStep(1) })
                Text(
                    text = "${index + 1} / $count",
                    style = AppTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPicks(items: List<FeaturedItem>, onOpen: (FeaturedItem) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.home_more_picks),
            style = AppTheme.typography.titleLarge,
            color = colors.onSurface
        )
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> QuickTile(item = item, onClick = { onOpen(item) }, modifier = Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(item: FeaturedItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(64.dp)
            // Before clip/clickable so the ring sits outside and tracks the tile's focus.
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(if (hovered) colors.surfaceContainerHighest else colors.surfaceContainerHigh)
            .border(1.dp, colors.outline, shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClickLabel = item.title, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(64.dp).background(colors.surfaceContainerHighest),
            contentScale = ContentScale.Crop
        )
        Text(
            text = item.title,
            style = AppTheme.typography.titleSmall,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * Small round step arrow on the hero card.
 */
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
            background = SolidColor(colors.background.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(50),
            size = DpSize(32.dp, 32.dp)
        ) {
            MillenniumIcon(
                if (forward) Glyphs.ChevronRight else Glyphs.ChevronLeft,
                contentDescription = stringResource(
                    if (forward) Res.string.featured_next
                    else Res.string.featured_previous
                ),
                tint = colors.onSurface,
                size = 18.dp
            )
        }
    }
}

/**
 * Round accent play key. If this item is what's already playing it toggles pause/resume instead
 * of restarting it (the old card showed a pause icon but navigated away when tapped).
 */
@Composable
internal fun FeaturedPlayButton(
    item: FeaturedItem,
    size: Dp = 40.dp,
    contentDescription: String? = null,
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
        size = size,
        // "Play <title>" only fits while it would start playback; playing falls back to "Pause".
        contentDescription = if (isPlaying) null else contentDescription,
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
