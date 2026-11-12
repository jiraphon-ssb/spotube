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

package org.spotube.spotube.modules.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.Grid as FeatherGrid
import compose.icons.feathericons.List as FeatherList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.history.ListeningHistoryRepository
import org.spotube.spotube.core.history.RecentType
import org.spotube.spotube.core.history.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.CollectionPlaybackKey
import org.spotube.spotube.core.remote.PlaybackDestinationAction
import org.spotube.spotube.core.remote.RemoteCollectionType
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveMenuItem
import org.spotube.spotube.core.ui.component.BigPlayButton
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.WindowChromeBand
import org.spotube.spotube.core.ui.component.cards.CollectionCoverTile
import org.spotube.spotube.core.ui.component.cards.CoverTile
import org.spotube.spotube.core.ui.component.cards.glassPlate
import org.spotube.spotube.core.ui.component.cards.pageGlassFill
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.misc.uniqueLazyKeys
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadsScreen
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.library.album.LibraryAlbumsState
import org.spotube.spotube.modules.library.album.LibraryAlbumsViewModel
import org.spotube.spotube.modules.library.artist.LibraryArtistsState
import org.spotube.spotube.modules.library.artist.LibraryArtistsViewModel
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksScreen
import org.spotube.spotube.modules.library.playlist.CreatePlaylistDialog
import org.spotube.spotube.modules.library.playlist.LibraryPlaylistsState
import org.spotube.spotube.modules.library.playlist.LibraryPlaylistsViewModel
import org.spotube.spotube.modules.plugin.guard.MetadataPluginGuard
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import org.spotube.spotube.modules.shell.roomLightPreview
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_action_create_playlist
import spotube.composeapp.generated.resources.library_action_grid_view
import spotube.composeapp.generated.resources.library_action_list_view
import spotube.composeapp.generated.resources.library_tab_albums
import spotube.composeapp.generated.resources.library_tab_artists
import spotube.composeapp.generated.resources.library_tab_downloads
import spotube.composeapp.generated.resources.library_tab_local_tracks
import spotube.composeapp.generated.resources.library_tab_playlists
import spotube.composeapp.generated.resources.media_album
import spotube.composeapp.generated.resources.media_artist
import spotube.composeapp.generated.resources.media_liked_tracks
import spotube.composeapp.generated.resources.media_playlist
import spotube.composeapp.generated.resources.pages_library_col_owner
import spotube.composeapp.generated.resources.pages_library_col_title
import spotube.composeapp.generated.resources.pages_library_col_tracks
import spotube.composeapp.generated.resources.pages_library_col_type
import spotube.composeapp.generated.resources.pages_library_downloads_empty
import spotube.composeapp.generated.resources.pages_library_downloads_summary
import spotube.composeapp.generated.resources.pages_library_empty
import spotube.composeapp.generated.resources.pages_library_filter_all
import spotube.composeapp.generated.resources.pages_library_liked_count
import spotube.composeapp.generated.resources.pages_library_no_match
import spotube.composeapp.generated.resources.pages_library_no_match_hint
import spotube.composeapp.generated.resources.pages_library_recent
import spotube.composeapp.generated.resources.pages_library_recent_empty
import spotube.composeapp.generated.resources.pages_library_recent_summary
import spotube.composeapp.generated.resources.pages_library_search_placeholder
import spotube.composeapp.generated.resources.pages_library_sort_creator
import spotube.composeapp.generated.resources.pages_library_sort_label
import spotube.composeapp.generated.resources.pages_library_sort_name
import spotube.composeapp.generated.resources.pages_library_sort_recent_added
import spotube.composeapp.generated.resources.pages_library_sort_recent_played
import spotube.composeapp.generated.resources.pages_library_summary
import spotube.composeapp.generated.resources.pages_library_title
import spotube.composeapp.generated.resources.pages_playing

/** What the library shows. [Recent] is reached from the "Recently played" card, not the segment. */
private enum class LibraryFilter(val label: StringResource?) {
    All(Res.string.pages_library_filter_all),
    Playlists(Res.string.library_tab_playlists),
    Albums(Res.string.library_tab_albums),
    Artists(Res.string.library_tab_artists),
    Downloads(Res.string.library_tab_downloads),
    Local(Res.string.library_tab_local_tracks),
    Recent(null);

    /** The shell-wide tab this filter maps to (other screens and the Other menu set it). */
    val tab: LibraryTab
        get() = when (this) {
            Albums -> LibraryTab.Albums
            Artists -> LibraryTab.Artists
            Downloads -> LibraryTab.Downloads
            Local -> LibraryTab.LocalTracks
            else -> LibraryTab.Playlists
        }

    val showsCollections: Boolean get() = this != Downloads && this != Local
}

private enum class LibrarySort(val label: StringResource) {
    RecentlyAdded(Res.string.pages_library_sort_recent_added),
    Name(Res.string.pages_library_sort_name),
    Creator(Res.string.pages_library_sort_creator),
    RecentlyPlayed(Res.string.pages_library_sort_recent_played)
}

private enum class EntryKind { Playlist, Album, Artist, LikedSongs }

/** One collection in the library, flattened from the playlist / album / artist lists. */
private data class LibraryEntry(
    val key: String,
    val id: String,
    val kind: EntryKind,
    val title: String,
    val owner: String?,
    val imageUrl: String?,
    val trackCount: Int?
)

private val PageSide = 48.dp
private val TileGap = 16.dp

/**
 * The wide-layout Library: header with counts and "Create playlist", a top row (Liked songs card,
 * pinned Downloads and Recently played cards that filter the list), a toolbar (type segment,
 * in-library search, sort, grid/list) and the collections as cover tiles or table rows. Downloads
 * and Local files keep their own screens under the same header.
 *
 * Only fields the plugins give are shown: there is no "added" date, pin or per-collection download
 * state, so there are no such columns or badges.
 */
@Composable
internal fun LibraryOverview(
    libraryState: LibraryState,
    history: ListeningHistoryRepository = koinInject()
) {
    val currentTab by libraryState.currentTab.collectAsStateWithLifecycle()
    val toolbarState by libraryState.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.All) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.RecentlyAdded) }
    var viewMode by rememberSaveable {
        mutableStateOf(libraryState.uiState.value.viewModeOverride ?: LibraryViewMode.Grid)
    }
    var createOpen by remember { mutableStateOf(false) }
    val query = rememberTextFieldState()

    // The Other menu / plugin guard point the library at Downloads or Local files through the
    // shared tab; follow them.
    LaunchedEffect(currentTab) {
        when (currentTab) {
            LibraryTab.Downloads -> filter = LibraryFilter.Downloads
            LibraryTab.LocalTracks -> filter = LibraryFilter.Local
            else -> if (!filter.showsCollections) filter = LibraryFilter.All
        }
    }

    fun select(next: LibraryFilter) {
        filter = next
        if (libraryState.currentTab.value != next.tab) libraryState.onTabSelected(next.tab)
    }

    val recent by history.recentCollections.collectAsStateWithLifecycle(initialValue = emptyList())
    val topInset = LocalAppShellTopInset.current
    val bottomInset = LocalAppShellBottomInset.current

    Box(Modifier.fillMaxSize()) {
        if (filter.showsCollections) {
            MetadataPluginGuard(showTopBar = false, requireLogin = true) {
                CollectionsBody(
                    filter = filter,
                    sort = sort,
                    viewMode = viewMode,
                    queryText = query.text.toString(),
                    recent = recent,
                    contentPadding = PaddingValues(
                        start = PageSide,
                        end = PageSide,
                        top = topInset + 6.dp,
                        bottom = bottomInset + 16.dp
                    ),
                    header = { counts ->
                        LibraryHeader(summary = counts, onCreate = { createOpen = true })
                    },
                    topRow = {
                        LibraryTopRow(
                            recentCovers = recent.mapNotNull { it.coverUrl }.distinct().take(4),
                            recentCount = recent.size,
                            filter = filter,
                            onFilter = ::select
                        )
                    },
                    toolbar = {
                        LibraryToolbar(
                            filter = filter,
                            onFilter = ::select,
                            searchState = query,
                            sort = sort,
                            onSort = { sort = it },
                            viewMode = viewMode,
                            onViewMode = { viewMode = it }
                        )
                    }
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = topInset + 6.dp)
            ) {
                Column(Modifier.padding(horizontal = PageSide)) {
                    LibraryHeader(summary = null, onCreate = { createOpen = true })
                    LibraryToolbar(
                        filter = filter,
                        onFilter = ::select,
                        // Local files keep their own filter (shared with the compact layout).
                        searchState = if (filter == LibraryFilter.Local) toolbarState.searchState else null,
                        sort = null,
                        onSort = {},
                        viewMode = null,
                        onViewMode = {}
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = PageSide - 12.dp)) {
                    if (filter == LibraryFilter.Downloads) {
                        DownloadsScreen(Modifier.fillMaxSize())
                    } else {
                        LibraryLocalTracksScreen(Modifier.fillMaxSize())
                    }
                }
            }
        }
        WindowChromeBand(Modifier.align(Alignment.TopCenter))
    }

    CreatePlaylistDialog(visible = createOpen, onDismiss = { createOpen = false })
}

@Composable
private fun CollectionsBody(
    filter: LibraryFilter,
    sort: LibrarySort,
    viewMode: LibraryViewMode,
    queryText: String,
    recent: List<org.spotube.spotube.core.history.RecentCollection>,
    contentPadding: PaddingValues,
    header: @Composable (summary: String?) -> Unit,
    topRow: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
    playlistsViewModel: LibraryPlaylistsViewModel = koinViewModel(),
    albumsViewModel: LibraryAlbumsViewModel = koinViewModel(),
    artistsViewModel: LibraryArtistsViewModel = koinViewModel(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    navigationCommands: NavigationCommands = koinInject()
) {
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
    val artistsState by artistsViewModel.uiState.collectAsStateWithLifecycle()
    val currentCollection by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    val playlistLabel = stringResource(Res.string.media_playlist)
    val albumLabel = stringResource(Res.string.media_album)
    val artistLabel = stringResource(Res.string.media_artist)
    val likedLabel = stringResource(Res.string.media_liked_tracks)

    val playlists = (playlistsState as? LibraryPlaylistsState.Data)?.allItems.orEmpty()
    val albums = (albumsState as? LibraryAlbumsState.Data)?.allItems.orEmpty()
    val artists = (artistsState as? LibraryArtistsState.Data)?.allItems.orEmpty()

    val summary = if (playlistsState is LibraryPlaylistsState.Data ||
        albumsState is LibraryAlbumsState.Data || artistsState is LibraryArtistsState.Data
    ) {
        fun count(size: Int, more: Boolean) = if (more) "$size+" else size.toString()
        stringResource(
            Res.string.pages_library_summary,
            count(playlists.size, (playlistsState as? LibraryPlaylistsState.Data)?.nextPagination != null),
            count(albums.size, (albumsState as? LibraryAlbumsState.Data)?.nextPagination != null),
            count(artists.size, (artistsState as? LibraryArtistsState.Data)?.nextPagination != null)
        )
    } else null

    val lastPlayed = remember(recent) { recent.associate { it.id to it.lastPlayedAtMillis } }

    val entries = remember(playlists, albums, artists, recent, filter, sort, queryText, likedLabel) {
        val base: List<LibraryEntry> = when (filter) {
            LibraryFilter.Recent -> recent.mapNotNull { item ->
                val kind = when {
                    item.type == RecentType.Playlist && item.id == SAVED_TRACKS_COLLECTION_ID -> EntryKind.LikedSongs
                    item.type == RecentType.Playlist -> EntryKind.Playlist
                    item.type == RecentType.Album -> EntryKind.Album
                    item.type == RecentType.Artist -> EntryKind.Artist
                    else -> null
                } ?: return@mapNotNull null
                LibraryEntry(
                    key = "recent:${item.type}:${item.id}",
                    id = item.id,
                    kind = kind,
                    title = if (kind == EntryKind.LikedSongs) likedLabel else item.title,
                    owner = null,
                    imageUrl = item.coverUrl,
                    trackCount = null
                )
            }
            else -> buildList {
                if (filter == LibraryFilter.All || filter == LibraryFilter.Playlists) {
                    playlists.forEach {
                        add(
                            LibraryEntry(
                                key = "playlist:${it.id}", id = it.id, kind = EntryKind.Playlist,
                                title = it.title,
                                owner = it.owner?.displayName ?: it.owner?.username,
                                imageUrl = it.thumbnails.firstOrNull()?.url,
                                trackCount = it.trackCount
                            )
                        )
                    }
                }
                if (filter == LibraryFilter.All || filter == LibraryFilter.Albums) {
                    albums.forEach {
                        add(
                            LibraryEntry(
                                key = "album:${it.id}", id = it.id, kind = EntryKind.Album,
                                title = it.title,
                                owner = it.artists.joinToString { artist -> artist.name }.ifBlank { null },
                                imageUrl = it.thumbnails.firstOrNull()?.url,
                                trackCount = it.trackCount
                            )
                        )
                    }
                }
                if (filter == LibraryFilter.All || filter == LibraryFilter.Artists) {
                    artists.forEach {
                        add(
                            LibraryEntry(
                                key = "artist:${it.id}", id = it.id, kind = EntryKind.Artist,
                                title = it.name, owner = null,
                                imageUrl = it.thumbnails.firstOrNull()?.url,
                                trackCount = null
                            )
                        )
                    }
                }
            }
        }
        val needle = queryText.trim()
        val matched = if (needle.isEmpty()) base else base.filter {
            it.title.contains(needle, ignoreCase = true) || it.owner?.contains(needle, ignoreCase = true) == true
        }
        when (sort) {
            LibrarySort.RecentlyAdded -> matched
            LibrarySort.Name -> matched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            LibrarySort.Creator -> matched.sortedWith(
                compareBy<LibraryEntry> { it.owner == null }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.owner.orEmpty() }
            )
            // Stable: never-played items keep their order at the end.
            LibrarySort.RecentlyPlayed -> matched.sortedByDescending { lastPlayed[it.id] ?: Long.MIN_VALUE }
        }
    }
    val keys = remember(entries) { uniqueLazyKeys(entries) { it.key } }

    val loading = when (filter) {
        LibraryFilter.Playlists -> playlistsState is LibraryPlaylistsState.Loading
        LibraryFilter.Albums -> albumsState is LibraryAlbumsState.Loading
        LibraryFilter.Artists -> artistsState is LibraryArtistsState.Loading
        LibraryFilter.All -> playlistsState is LibraryPlaylistsState.Loading &&
            albumsState is LibraryAlbumsState.Loading && artistsState is LibraryArtistsState.Loading
        else -> false
    }
    val errorMessage: String? = when (filter) {
        LibraryFilter.Playlists -> (playlistsState as? LibraryPlaylistsState.Error)?.message
        LibraryFilter.Albums -> (albumsState as? LibraryAlbumsState.Error)?.message
        LibraryFilter.Artists -> (artistsState as? LibraryArtistsState.Error)?.message
        LibraryFilter.All -> {
            val p = playlistsState as? LibraryPlaylistsState.Error
            val a = albumsState as? LibraryAlbumsState.Error
            val r = artistsState as? LibraryArtistsState.Error
            if (p != null && a != null && r != null) p.message else null
        }
        else -> null
    }

    // Near the end of what's loaded: ask the lists on screen for their next page.
    LaunchedEffect(gridState, filter) {
        snapshotFlow {
            gridState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: -1) to it.totalItemsCount }
        }
            .distinctUntilChanged()
            .collect { (last, total) ->
                if (total > 0 && last >= total - 6) {
                    // onItemsVisible(1, 1) = "the end is visible": each VM loads its next page if it has one.
                    if (filter == LibraryFilter.All || filter == LibraryFilter.Playlists) playlistsViewModel.onItemsVisible(1, 1)
                    if (filter == LibraryFilter.All || filter == LibraryFilter.Albums) albumsViewModel.onItemsVisible(1, 1)
                    if (filter == LibraryFilter.All || filter == LibraryFilter.Artists) artistsViewModel.onItemsVisible(1, 1)
                }
            }
    }

    val playingLabel = stringResource(Res.string.pages_playing)
    fun isCurrent(entry: LibraryEntry): Boolean = when (entry.kind) {
        EntryKind.Playlist -> (currentCollection as? QueueCollectionEntry.Playlist)?.id == entry.id
        EntryKind.Album -> (currentCollection as? QueueCollectionEntry.Album)?.id == entry.id
        EntryKind.LikedSongs -> currentCollection is QueueCollectionEntry.SavedTracks
        EntryKind.Artist -> false
    }
    fun open(entry: LibraryEntry) {
        when (entry.kind) {
            EntryKind.Playlist -> navigationCommands.navigateTo(Routes.Playlist(entry.id))
            EntryKind.Album -> navigationCommands.navigateTo(Routes.Album(entry.id))
            EntryKind.Artist -> navigationCommands.navigateTo(Routes.Artist(entry.id))
            EntryKind.LikedSongs -> navigationCommands.navigateTo(Routes.SavedTracks)
        }
    }
    fun typeLabel(entry: LibraryEntry) = when (entry.kind) {
        EntryKind.Playlist, EntryKind.LikedSongs -> playlistLabel
        EntryKind.Album -> albumLabel
        EntryKind.Artist -> artistLabel
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(TileGap)
    ) {
        item(key = "library_header", span = { GridItemSpan(maxLineSpan) }) { header(summary) }
        item(key = "library_top_row", span = { GridItemSpan(maxLineSpan) }) { topRow() }
        item(key = "library_toolbar", span = { GridItemSpan(maxLineSpan) }) { toolbar() }

        when {
            errorMessage != null && entries.isEmpty() -> item(key = "library_error", span = { GridItemSpan(maxLineSpan) }) {
                ErrorDisplay(
                    errorMessage = errorMessage,
                    onRetry = {
                        playlistsViewModel.refresh()
                        albumsViewModel.refresh()
                        artistsViewModel.refresh()
                    }
                )
            }

            loading && entries.isEmpty() -> items(count = 8, key = { "library_placeholder_$it" }) {
                Column(Modifier.padding(bottom = 20.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surfaceContainerHigh)
                    )
                    Box(
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppTheme.colors.surfaceContainerHigh)
                    )
                }
            }

            entries.isEmpty() -> item(key = "library_empty", span = { GridItemSpan(maxLineSpan) }) {
                LibraryEmpty(query = queryText.trim(), filter = filter)
            }

            viewMode == LibraryViewMode.Grid -> items(count = entries.size, key = { keys[it] }) { index ->
                val entry = entries[index]
                val subtitle = if (entry.owner != null) "${typeLabel(entry)} · ${entry.owner}" else typeLabel(entry)
                val tileModifier = Modifier.padding(bottom = 20.dp)
                when (entry.kind) {
                    EntryKind.Playlist -> CollectionCoverTile(
                        type = RemoteCollectionType.Playlist, id = entry.id, title = entry.title,
                        subtitle = subtitle, imageUrl = entry.imageUrl, modifier = tileModifier
                    )
                    EntryKind.Album -> CollectionCoverTile(
                        type = RemoteCollectionType.Album, id = entry.id, title = entry.title,
                        subtitle = subtitle, imageUrl = entry.imageUrl, modifier = tileModifier
                    )
                    EntryKind.Artist -> CoverTile(
                        title = entry.title, subtitle = subtitle, imageModel = entry.imageUrl,
                        onClick = { open(entry) }, modifier = tileModifier, circle = true
                    )
                    EntryKind.LikedSongs -> CoverTile(
                        title = entry.title, subtitle = subtitle, imageModel = entry.imageUrl,
                        onClick = { open(entry) }, modifier = tileModifier,
                        isCurrent = isCurrent(entry), playingLabel = playingLabel
                    )
                }
            }

            else -> {
                item(key = "library_table_head", span = { GridItemSpan(maxLineSpan) }) { LibraryTableHead() }
                items(count = entries.size, key = { keys[it] }, span = { GridItemSpan(maxLineSpan) }) { index ->
                    val entry = entries[index]
                    LibraryTableRow(
                        title = entry.title,
                        type = typeLabel(entry),
                        owner = entry.owner,
                        trackCount = entry.trackCount,
                        imageUrl = entry.imageUrl,
                        circle = entry.kind == EntryKind.Artist,
                        isCurrent = isCurrent(entry),
                        last = index == entries.lastIndex,
                        onClick = { open(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(summary: String?, onCreate: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.pages_library_title),
                style = AppTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.8).sp
                ),
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        MillenniumButton(onClick = onCreate, style = MillenniumButtonStyle.Primary) {
            MillenniumIcon(imageVector = Glyphs.AddTo, contentDescription = null, size = 16.dp)
            Text(stringResource(Res.string.library_action_create_playlist), maxLines = 1)
        }
    }
}

/** Liked songs + the two pinned cards. */
@Composable
private fun LibraryTopRow(
    recentCovers: List<String>,
    recentCount: Int,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    downloadsViewModel: DownloadsViewModel = koinViewModel()
) {
    val downloads by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp).height(178.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LikedSongsCard(Modifier.weight(1.35f).fillMaxHeight())
        PinCard(
            icon = Glyphs.Download,
            title = stringResource(Res.string.library_tab_downloads),
            subtitle = if (downloads.items.isNotEmpty()) {
                stringResource(Res.string.pages_library_downloads_summary, downloads.items.size)
            } else {
                stringResource(Res.string.pages_library_downloads_empty)
            },
            covers = downloads.items.mapNotNull { it.artworkUrl }.distinct().take(4),
            selected = filter == LibraryFilter.Downloads,
            onClick = { onFilter(LibraryFilter.Downloads) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        PinCard(
            icon = Glyphs.Clock,
            title = stringResource(Res.string.pages_library_recent),
            subtitle = if (recentCount > 0) {
                stringResource(Res.string.pages_library_recent_summary, recentCount)
            } else {
                stringResource(Res.string.pages_library_recent_empty)
            },
            covers = recentCovers,
            selected = filter == LibraryFilter.Recent,
            onClick = { onFilter(if (filter == LibraryFilter.Recent) LibraryFilter.All else LibraryFilter.Recent) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

private data class LikedSummary(val count: Int?, val covers: List<String>)

/**
 * The big "Liked songs" card: accent gradient, the four most recent liked covers stacked in the
 * corner, the song count and a play key. The whole card opens Liked songs.
 */
@Composable
private fun LikedSongsCard(
    modifier: Modifier = Modifier,
    savedTracksRepository: SavedTracksRepository = koinInject(),
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject(),
    navigationCommands: NavigationCommands = koinInject()
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val changes by savedTracksRepository.libraryChanges.collectAsStateWithLifecycle()
    val summary by produceState<LikedSummary?>(initialValue = null, changes) {
        value = try {
            savedTracksRepository.getSavedTracks()?.let { page ->
                LikedSummary(
                    count = page.totalCount.takeIf { it > 0 } ?: page.items.size,
                    covers = page.items
                        .mapNotNull { (it.album?.thumbnails ?: it.thumbnails)?.firstOrNull()?.url }
                        .distinct()
                        .take(4)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    val currentCollection by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val busy by remotePlaybackController.busyCollections.collectAsStateWithLifecycle()
    val isCurrent = currentCollection is QueueCollectionEntry.SavedTracks
    val isPlaying = isCurrent && playerState == PlayerState.PLAYING
    val title = stringResource(Res.string.media_liked_tracks)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(22.dp)
    val start = lerp(Color(0xFF1A2A44), colors.primary, if (hovered) 0.62f else 0.55f)

    Box(
        modifier = modifier
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to start,
                    0.75f to Color(0xFF121C2C),
                    1f to Color(0xFF121C2C)
                )
            )
            .border(1.dp, colors.outline, shape)
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = title
            ) { navigationCommands.navigateTo(Routes.SavedTracks) }
            .padding(22.dp)
    ) {
        val covers = summary?.covers.orEmpty()
        if (covers.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd)) {
                covers.forEachIndexed { index, url ->
                    Box(
                        Modifier
                            .padding(start = (40 * index).dp)
                            .size(58.dp)
                            .shadow(8.dp, RoundedCornerShape(10.dp), clip = false)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceContainerHighest)
                            .border(2.dp, Color(0xFF0A0E14).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(end = 70.dp)) {
            Text(
                text = title,
                style = AppTheme.typography.headlineMedium.copy(letterSpacing = (-0.6).sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val count = summary?.count
            if (count != null) {
                Text(
                    text = stringResource(Res.string.pages_library_liked_count, count),
                    style = AppTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
        BigPlayButton(
            isPlaying = isPlaying,
            loading = CollectionPlaybackKey(
                RemoteCollectionType.SavedTracks, SAVED_TRACKS_COLLECTION_ID, PlaybackDestinationAction.Play
            ) in busy,
            size = 52.dp,
            onClick = {
                if (isCurrent) {
                    scope.launch { if (isPlaying) audioPlayer.pause() else audioPlayer.play() }
                } else {
                    remotePlaybackController.requestCollectionPlay(
                        RemoteCollectionType.SavedTracks, SAVED_TRACKS_COLLECTION_ID, title
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/** A pinned glass card (Downloads, Recently played): tapping filters the list. */
@Composable
private fun PinCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    covers: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .keyboardFocusRing(shape)
            .glassPlate(shape)
            .background(if (hovered || selected) colors.hover else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, colors.primary.copy(alpha = 0.6f), shape) else Modifier)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(imageVector = icon, contentDescription = null, tint = colors.primary, size = 20.dp)
        }
        Column {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = colors.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = AppTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (covers.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                covers.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.surfaceContainerHighest)
                            .roomLightPreview(url),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Type segment, search, sort and grid/list. [searchState], [sort] and [viewMode] are optional: Downloads
 * and Local files only take what applies to them.
 */
@Composable
private fun LibraryToolbar(
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    searchState: TextFieldState?,
    sort: LibrarySort?,
    onSort: (LibrarySort) -> Unit,
    viewMode: LibraryViewMode?,
    onViewMode: (LibraryViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Segmented(
            options = LibraryFilter.entries.filter { it.label != null },
            selected = filter,
            label = { stringResource(it.label!!) },
            onSelect = onFilter
        )
        Spacer(Modifier.weight(1f))
        if (searchState != null) {
            MillenniumTextField(
                state = searchState,
                placeholder = stringResource(Res.string.pages_library_search_placeholder),
                minimumHeight = 38.dp,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.width(240.dp)
            )
        }
        if (sort != null) SortMenu(sort, onSort)
        if (viewMode != null) ViewToggle(viewMode, onViewMode)
    }
}

@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .glassPlate(RoundedCornerShape(12.dp), fill = Color.White.copy(alpha = 0.05f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val shape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .keyboardFocusRing(shape)
                    .clip(shape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = if (isSelected || hovered) colors.onSurface else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SortMenu(sort: LibrarySort, onSort: (LibrarySort) -> Unit) {
    val colors = AppTheme.colors
    val sortLabel = stringResource(Res.string.pages_library_sort_label)
    AdaptiveDropdownBottomSheet(
        items = LibrarySort.entries.map { option ->
            AdaptiveMenuItem(
                label = stringResource(option.label),
                onClick = { onSort(option) },
                selected = option == sort
            )
        },
        trigger = { onClick ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .keyboardFocusRing(shape)
                    .glassPlate(shape)
                    .background(if (hovered) colors.hover else Color.Transparent)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.DropdownList,
                        onClickLabel = sortLabel,
                        onClick = onClick
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MillenniumIcon(imageVector = Glyphs.Sort, contentDescription = null, tint = colors.onSurface, size = 15.dp)
                Text(
                    text = stringResource(sort.label),
                    style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                    maxLines = 1
                )
            }
        }
    )
}

@Composable
private fun ViewToggle(viewMode: LibraryViewMode, onViewMode: (LibraryViewMode) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .glassPlate(RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(
            Triple(LibraryViewMode.Grid, FeatherIcons.FeatherGrid, Res.string.library_action_grid_view),
            Triple(LibraryViewMode.List, FeatherIcons.FeatherList, Res.string.library_action_list_view)
        ).forEach { (mode, icon, label) ->
            val isSelected = mode == viewMode
            val shape = RoundedCornerShape(9.dp)
            val text = stringResource(label)
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 32.dp)
                    .keyboardFocusRing(shape)
                    .clip(shape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = text) { onViewMode(mode) }
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                    size = 16.dp
                )
            }
        }
    }
}

// ---- List view: one glass sheet, each row a slice of it --------------------------------------

private val ColCover = 52.dp
private val ColType = 110.dp
private val ColCount = 90.dp

@Composable
private fun LibraryTableHead() {
    val colors = AppTheme.colors
    val style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(pageGlassFill())
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(ColCover))
            Text(stringResource(Res.string.pages_library_col_title), style = style, color = colors.onSurfaceVariant, modifier = Modifier.weight(1.6f))
            Text(stringResource(Res.string.pages_library_col_type), style = style, color = colors.onSurfaceVariant, modifier = Modifier.width(ColType))
            Text(stringResource(Res.string.pages_library_col_owner), style = style, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                stringResource(Res.string.pages_library_col_tracks), style = style, color = colors.onSurfaceVariant,
                textAlign = TextAlign.End, modifier = Modifier.width(ColCount)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LibraryTableRow(
    title: String,
    type: String,
    owner: String?,
    trackCount: Int?,
    imageUrl: String?,
    circle: Boolean,
    isCurrent: Boolean,
    last: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val sheetShape = if (last) RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp) else RoundedCornerShape(0.dp)
    val rowShape = RoundedCornerShape(12.dp)
    val coverShape = if (circle) CircleShape else RoundedCornerShape(8.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(pageGlassFill())
            .padding(start = 8.dp, end = 8.dp, bottom = if (last) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .keyboardFocusRing(rowShape)
                .clip(rowShape)
                .background(if (hovered) colors.hover else Color.Transparent)
                .roomLightPreview(imageUrl)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = title, onClick = onClick)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(ColCover)) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(coverShape)
                        .background(colors.surfaceContainerHighest)
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Text(
                text = title,
                style = AppTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCurrent) colors.primary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.6f)
            )
            Text(type, style = AppTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.width(ColType))
            Text(
                text = owner ?: "—",
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = trackCount?.toString() ?: "—",
                style = AppTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(ColCount)
            )
        }
    }
}

@Composable
private fun LibraryEmpty(query: String, filter: LibraryFilter) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (query.isNotEmpty()) {
                stringResource(Res.string.pages_library_no_match, query)
            } else if (filter == LibraryFilter.Recent) {
                stringResource(Res.string.pages_library_recent_empty)
            } else {
                stringResource(Res.string.pages_library_empty)
            },
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
        if (query.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.pages_library_no_match_hint),
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
