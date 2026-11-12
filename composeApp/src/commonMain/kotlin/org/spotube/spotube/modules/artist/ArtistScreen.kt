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

package org.spotube.spotube.modules.artist


import org.spotube.spotube.core.ui.component.WindowChromeBand
import org.spotube.spotube.core.ui.component.cards.isWideShell
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumTooltip
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.spotube.spotube.core.ui.component.BigPlayButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import com.composeunstyled.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.koin.compose.koinInject
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackList
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.modules.library.playlist.AddToPlaylistPicker
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.resources.glyphs.Glyphs
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.spotube.spotube.resources.iconsax.User
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun ArtistScreen(
    viewModel: ArtistViewModel,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playbackUiState by viewModel.playbackUiState.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val currentQueueEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val currentCollection by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val isTopTracksQueue by viewModel.isTopTracksQueue.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val savedArtistIds by viewModel.savedArtistIds.collectAsStateWithLifecycle()
    val blacklistedArtistIds by viewModel.blacklistedArtistIds.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()

    // Wide glass shell: no opaque canvas (the room light shows through) and no title bar (the
    // shell's floating back button replaces it); the content starts under the floating chrome.
    val wide = isWideShell()
    val shellTopInset = LocalAppShellTopInset.current
    val shellBottomInset = LocalAppShellBottomInset.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (wide) Modifier else Modifier.background(MillenniumTheme.colors.background))
    ) {
        if (!wide) ApplicationMainBar()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val currentState = state) {
                is ArtistScreenState.Loading -> {
                    ArtistLoadingContent()
                }

                is ArtistScreenState.Error -> {
                    ArtistErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is ArtistScreenState.Loaded -> {
                    val listState = rememberLazyListState()
                    // Top tracks load with no collection entry (there is no artist entry type), so
                    // the VM tracks whether the queue is this artist's top tracks; the artist is
                    // current when that queue is on one of them.
                    val currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id
                    val isArtistCurrent = isTopTracksQueue && currentCollection == null &&
                        currentTrackId != null && currentState.topTracks.any { it.id == currentTrackId }
                    val isArtistPlaying = isArtistCurrent && playerState == PlayerState.PLAYING

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = shellTopInset,
                            bottom = if (wide) shellBottomInset + 16.dp else 0.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            ArtistBanner(
                                artist = currentState.artist,
                                isSaved = savedArtistIds.contains(currentState.artist.id),
                                isSaving = currentState.isSaving,
                                onFollowClick = viewModel::toggleSavedArtist,
                                isBlacklisted = currentState.artist.id in blacklistedArtistIds,
                                onBlacklistClick = { viewModel.toggleArtistBlacklist(currentState.artist) },
                                isPlaying = isArtistPlaying,
                                onPlay = {
                                    if (isArtistCurrent) {
                                        scope.launch {
                                            if (isArtistPlaying) audioPlayer.pause() else audioPlayer.play()
                                        }
                                    } else {
                                        viewModel.playTopTracks()
                                    }
                                },
                                isPlayLoading = playbackUiState.isPlayLoading,
                                onAddToQueue = viewModel::addTopTracksToQueue,
                                isAddToQueueLoading = playbackUiState.isAddToQueueLoading)
                        }

                        item { TopTracksHeader() }

                        item {
                            TrackList(
                                tracks = currentState.topTracks,
                                error = null,
                                hasMore = false,
                                isLoading = false,
                                isLoadingNextPage = false,
                                currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id,
                                isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
                                onTrackClick = viewModel::playTopTracksFromTrack,
                                onTrackOptionsAction = viewModel::handleTrackOptionsAction,
                                trackOptionsState = trackOptionsContext::stateFor,
                                onArtistClick = { trackArtist ->
                                    navigationCommands.navigateTo(
                                        Routes.Artist(
                                            trackArtist.id
                                        )
                                    )
                                },
                                onAlbumClick = { album ->
                                    navigationCommands.navigateTo(
                                        Routes.Album(
                                            album.id
                                        )
                                    )
                                },
                                onLoadNextPage = {},
                                simplified = true,
                                scrollable = false,
                                onBulkDownload = viewModel::downloadTracks,
                                onBulkAddToQueue = viewModel::addTracksToQueue,
                                onBulkPlayNext = viewModel::playTracksNext,
                                onBulkAddToPlaylist = viewModel::showAddToPlaylistPicker,
                                onBulkAddToJam = viewModel::addTracksToJam,
                                isInJam = jamActive,
                                isJamGuest = isJamGuest
                            )
                        }

                        if (currentState.albums.isNotEmpty() || currentState.albumsNextPagination != null) {
                            item {
                                AlbumsSection(
                                    albums = currentState.albums,
                                    onAlbumClick = { album ->
                                        navigationCommands.navigateTo(
                                            Routes.Album(
                                                album.id
                                            )
                                        )
                                    },
                                    onLoadMore = { viewModel.loadMoreAlbums() })
                            }
                        }

                        if (currentState.relatedArtists.isNotEmpty() || currentState.relatedArtistsNextPagination != null) {
                            item {
                                RelatedArtistsSection(
                                    artists = currentState.relatedArtists,
                                    onArtistClick = { artist ->
                                        navigationCommands.navigateTo(
                                            Routes.Artist(
                                                artist.id
                                            )
                                        )
                                    },
                                    onLoadMore = { viewModel.loadMoreRelatedArtists() })
                            }
                        }

                        if (currentState.featuredPlaylists.isNotEmpty() || currentState.featuredPlaylistsNextPagination != null) {
                            item {
                                FeaturedPlaylistsSection(
                                    playlists = currentState.featuredPlaylists,
                                    onPlaylistClick = { playlist ->
                                        navigationCommands.navigateTo(
                                            Routes.Playlist(
                                                playlist.id
                                            )
                                        )
                                    },
                                    onLoadMore = { viewModel.loadMoreFeaturedPlaylists() })
                            }
                        }
                    }
                }
            }
            // Keeps the window draggable (and its buttons) under the floating chrome.
            WindowChromeBand(Modifier.align(Alignment.TopCenter))
        }

        AddToPlaylistPicker(
            visible = showAddToPlaylistPicker,
            currentUserId = currentUserId,
            onDismiss = viewModel::dismissAddToPlaylistPicker,
            onPlaylistSelected = viewModel::addTracksToPlaylist
        )
    }
}

@Composable
private fun ArtistLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SkeletonTree(true) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceContainerHighest)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(4) {
                            PlayableCard(
                                title = "Item Title",
                                subtitle = "Subtitle",
                                imageURL = "https://placehold.co/600x400"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Same error surface as the rest of the app (summary, copyable details, retry) instead
        // of dumping the raw exception text across the page.
        ErrorDisplay(errorMessage = message, onRetry = onRetry)
    }
}

/**
 * Full-bleed artist banner: the artist's photo fills the width and fades into the canvas, with the
 * name set large on top of it as the page's hero. Play (top tracks) and Follow sit right under it.
 */
@Composable
private fun ArtistBanner(
    artist: MetadataArtist.Detailed,
    isSaved: Boolean,
    isSaving: Boolean,
    onFollowClick: () -> Unit,
    isBlacklisted: Boolean,
    onBlacklistClick: () -> Unit,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    isPlayLoading: Boolean,
    onAddToQueue: () -> Unit,
    isAddToQueueLoading: Boolean
) {
    val colors = AppTheme.colors
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val imageUrl = artist.thumbnails.firstOrNull()?.url.orEmpty()
    val tint by rememberArtworkTint(imageUrl.ifBlank { null }, fallback = colors.surfaceContainerHigh)
    val sidePadding = artistGutter()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 280.dp else 340.dp)
                // Read in the draw phase so the tint animation doesn't recompose the banner.
                .drawBehind { drawRect(tint) }
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // Faces sit in the upper part of most artist photos.
                    alignment = BiasAlignment(0f, -0.45f)
                )
            }
            // Fade into the canvas so the name reads on any photo and the page continues seamlessly.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.background.copy(alpha = 0.1f),
                            0.45f to colors.background.copy(alpha = 0.35f),
                            1f to colors.background
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = sidePadding, end = sidePadding, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = artist.name,
                    style = artistNameStyle(artist.name, isCompact),
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(formatFollowers(artist.followersCount))
                        if (artist.genres.isNotEmpty()) {
                            append(", ")
                            append(artist.genres.take(3).joinToString(", "))
                        }
                    },
                    style = AppTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = sidePadding, end = sidePadding, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BigPlayButton(isPlaying = isPlaying, loading = isPlayLoading, onClick = onPlay)
            MillenniumButton(
                onClick = onFollowClick,
                loading = isSaving,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(
                    text = stringResource(
                        if (isSaved) Res.string.artist_following else Res.string.collection_details_follow
                    ),
                    maxLines = 1
                )
            }
            MillenniumIconButton(
                onClick = onAddToQueue,
                loading = isAddToQueueLoading,
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    imageVector = Glyphs.AddTo,
                    contentDescription = stringResource(Res.string.artist_add_top_tracks_to_queue),
                    tint = colors.onSurfaceVariant,
                    size = 24.dp
                )
            }
            MillenniumTooltip(
                text = stringResource(if (isBlacklisted) Res.string.blacklist_remove else Res.string.blacklist_add)
            ) {
                MillenniumIconButton(
                    onClick = onBlacklistClick,
                    style = MillenniumIconButtonStyle.Quiet
                ) {
                    MillenniumIcon(
                        imageVector = Glyphs.UserBlock,
                        contentDescription = stringResource(
                            if (isBlacklisted) Res.string.blacklist_remove else Res.string.blacklist_add
                        ),
                        tint = if (isBlacklisted) colors.error else colors.onSurfaceVariant,
                        size = 24.dp
                    )
                }
            }
        }

        artist.biography?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = sidePadding, end = sidePadding, top = 16.dp)
                    .widthIn(max = 720.dp)
            )
        }
    }
}

/** Short names get the poster size; long ones step down so they still fit on two lines. */
@Composable
private fun artistNameStyle(name: String, isCompact: Boolean): TextStyle {
    val type = AppTheme.typography
    return when {
        isCompact -> type.displaySmall
        name.length <= 14 -> type.displayLarge.copy(fontSize = 88.sp, lineHeight = 106.sp, letterSpacing = (-2.4).sp)
        name.length <= 28 -> type.displayLarge
        else -> type.displaySmall
    }
}

/** The page's one horizontal gutter: banner, section headers and shelf artwork all align to it. */
@Composable
private fun artistGutter(): Dp =
    if (currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium) 16.dp else 28.dp

/** Shelf contentPadding: cards carry their own 8dp hover-plate inset, so artwork lands on the gutter. */
@Composable
private fun artistShelfPadding(): PaddingValues = PaddingValues(horizontal = artistGutter() - 8.dp)

@Composable
private fun TopTracksHeader() {
    SectionHeader(title = stringResource(Res.string.artist_top_tracks))
}

@Composable
private fun AlbumsSection(
    albums: List<MetadataAlbum.Detailed>,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()
    LaunchedEffect(rowState) {
        // Key on (last visible, total) so a page that still fits on screen asks again.
        snapshotFlow {
            val info = rowState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.distinctUntilChanged().collect { (last, total) ->
            if (total > 0 && last >= total - 2) onLoadMore()
        }
    }

    SectionHeader(title = stringResource(Res.string.library_tab_albums))
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = artistShelfPadding(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsWithUniqueKeys(albums, id = { it.id }) { album ->
            AlbumCard(album = album)
        }
    }
}

@Composable
private fun RelatedArtistsSection(
    artists: List<MetadataArtist.Basic>,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = stringResource(Res.string.artist_related_artists))
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = artistShelfPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsWithUniqueKeys(artists, id = { it.id }) { artist ->
            ArtistCard(artist = artist)
        }
    }
}

@Composable
private fun FeaturedPlaylistsSection(
    playlists: List<MetadataPlaylist>,
    onPlaylistClick: (MetadataPlaylist) -> Unit,
    onLoadMore: () -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = stringResource(Res.string.artist_featured_playlists))
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = artistShelfPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsWithUniqueKeys(playlists, id = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        // 20/28 Bold, tightened: same as Home shelves and Search sections.
        style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        color = AppTheme.colors.onSurface,
        modifier = Modifier.padding(horizontal = artistGutter())
    )
}

@Composable
private fun formatFollowers(count: Int?): String {
    if (count == null) return stringResource(Res.string.artist_followers_unavailable)
    return when {
        count >= 1_000_000_000 -> stringResource(Res.string.artist_followers_billions, formatAbbreviatedCount(count, 1_000_000_000))
        count >= 1_000_000 -> stringResource(Res.string.artist_followers_millions, formatAbbreviatedCount(count, 1_000_000))
        count >= 1_000 -> stringResource(Res.string.artist_followers_thousands, formatAbbreviatedCount(count, 1_000))
        else -> pluralStringResource(Res.plurals.artist_followers_count, count, count)
    }
}

private fun formatAbbreviatedCount(count: Int, divisor: Int): String {
    val scaled = count / divisor.toDouble()
    val rounded = (scaled * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
