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

package org.spotube.spotube.modules.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.audioplayer.SleepTimerChoice
import org.spotube.spotube.core.audioplayer.SoundSettings
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.search.SearchRepository
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.overlay_cmd_downloads
import spotube.composeapp.generated.resources.overlay_cmd_library
import spotube.composeapp.generated.resources.overlay_cmd_mini
import spotube.composeapp.generated.resources.overlay_cmd_queue
import spotube.composeapp.generated.resources.overlay_cmd_repeat
import spotube.composeapp.generated.resources.overlay_cmd_search
import spotube.composeapp.generated.resources.overlay_cmd_settings
import spotube.composeapp.generated.resources.overlay_cmd_shuffle
import spotube.composeapp.generated.resources.overlay_cmd_sleep_30
import spotube.composeapp.generated.resources.overlay_cmd_sound
import spotube.composeapp.generated.resources.overlay_cmd_stats
import spotube.composeapp.generated.resources.overlay_palette_empty
import spotube.composeapp.generated.resources.overlay_palette_group_albums
import spotube.composeapp.generated.resources.overlay_palette_group_artists
import spotube.composeapp.generated.resources.overlay_palette_group_commands
import spotube.composeapp.generated.resources.overlay_palette_group_playlists
import spotube.composeapp.generated.resources.overlay_palette_group_songs
import spotube.composeapp.generated.resources.overlay_palette_hint_close
import spotube.composeapp.generated.resources.overlay_palette_hint_move
import spotube.composeapp.generated.resources.overlay_palette_hint_play
import spotube.composeapp.generated.resources.overlay_palette_hint_run
import spotube.composeapp.generated.resources.overlay_palette_input_label
import spotube.composeapp.generated.resources.overlay_palette_key_open
import spotube.composeapp.generated.resources.overlay_palette_key_play
import spotube.composeapp.generated.resources.overlay_palette_key_run
import spotube.composeapp.generated.resources.overlay_palette_kind_album
import spotube.composeapp.generated.resources.overlay_palette_kind_artist
import spotube.composeapp.generated.resources.overlay_palette_kind_playlist
import spotube.composeapp.generated.resources.overlay_palette_kind_playlist_by
import spotube.composeapp.generated.resources.overlay_palette_placeholder
import spotube.composeapp.generated.resources.overlay_palette_searching
import spotube.composeapp.generated.resources.overlay_sleep_30
import spotube.composeapp.generated.resources.overlay_sleep_toast_minutes
import spotube.composeapp.generated.resources.overlay_toast_playing
import spotube.composeapp.generated.resources.overlay_toast_repeat_all
import spotube.composeapp.generated.resources.overlay_toast_repeat_off
import spotube.composeapp.generated.resources.overlay_toast_repeat_one
import spotube.composeapp.generated.resources.overlay_toast_shuffle_off
import spotube.composeapp.generated.resources.overlay_toast_shuffle_on

private const val SEARCH_DEBOUNCE_MS = 250L
private const val MAX_TRACKS = 4
private const val MAX_OTHERS = 2

private val PaletteShape = RoundedCornerShape(22.dp)
private val RowShape = RoundedCornerShape(12.dp)

/** Playing a track must outlive the palette, which closes as soon as an item runs. */
private object PaletteJobs {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

private enum class PaletteCommand(val icon: ImageVector, val keywords: String) {
    Shuffle(Glyphs.Shuffle, "shuffle random"),
    Repeat(Glyphs.Repeat, "repeat loop"),
    Sleep30(Glyphs.Clock, "sleep timer stop"),
    Queue(Glyphs.Queue, "queue up next"),
    Sound(Glyphs.Equalizer, "sound eq equalizer volume speed"),
    Mini(Glyphs.MiniPlayer, "mini player compact"),
    Library(Glyphs.Library, "library collection"),
    Stats(Glyphs.Stats, "stats statistics history"),
    Settings(Glyphs.Settings, "settings preferences options"),
    Downloads(Glyphs.Download, "downloads offline"),
    Search(Glyphs.Search, "search find")
}

private sealed interface PaletteItem {
    data class Track(val track: MetadataTrack) : PaletteItem
    data class Artist(val artist: MetadataArtist.Basic) : PaletteItem
    data class Album(val album: MetadataAlbum.Basic) : PaletteItem
    data class Playlist(val playlist: MetadataPlaylist) : PaletteItem
    data class Command(val command: PaletteCommand, val label: String) : PaletteItem
}

/** One line of the result list: a group header, or a selectable item ([flatIndex] in run order). */
private sealed interface PaletteRow {
    data class Header(val text: String) : PaletteRow
    data class Entry(val item: PaletteItem, val flatIndex: Int) : PaletteRow
}

private data class PaletteResults(
    val tracks: List<MetadataTrack> = emptyList(),
    val artists: List<MetadataArtist.Basic> = emptyList(),
    val albums: List<MetadataAlbum.Basic> = emptyList(),
    val playlists: List<MetadataPlaylist> = emptyList()
)

/**
 * The ⌘K palette (mockup `#cmdk`): a centred glass box over a dimmed page. Typing searches the
 * metadata plugin (debounced) and filters commands. Up/Down move the selection, Enter opens or
 * runs it, Shift+Enter plays a song, Escape closes (handled by the shell).
 */
@Composable
internal fun CommandPalette(
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
    searchRepository: SearchRepository = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    libraryState: LibraryState = koinInject()
) {
    val colors = AppTheme.colors
    val shellActions = LocalShellActions.current
    val player = rememberPlayerController(audioPlayerQueue = audioPlayerQueue)
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { inputFocus.requestFocus() } }

    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    var results by remember { mutableStateOf(PaletteResults()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(trimmed) {
        if (trimmed.isEmpty()) {
            results = PaletteResults()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(SEARCH_DEBOUNCE_MS)
        results = withContext(Dispatchers.Default) { searchTop(searchRepository, trimmed) }
        loading = false
    }

    // Commands, filtered by label or keywords.
    val commandLabels = mapOf(
        PaletteCommand.Shuffle to stringResource(Res.string.overlay_cmd_shuffle),
        PaletteCommand.Repeat to stringResource(Res.string.overlay_cmd_repeat),
        PaletteCommand.Sleep30 to stringResource(Res.string.overlay_cmd_sleep_30),
        PaletteCommand.Queue to stringResource(Res.string.overlay_cmd_queue),
        PaletteCommand.Sound to stringResource(Res.string.overlay_cmd_sound),
        PaletteCommand.Mini to stringResource(Res.string.overlay_cmd_mini),
        PaletteCommand.Library to stringResource(Res.string.overlay_cmd_library),
        PaletteCommand.Stats to stringResource(Res.string.overlay_cmd_stats),
        PaletteCommand.Settings to stringResource(Res.string.overlay_cmd_settings),
        PaletteCommand.Downloads to stringResource(Res.string.overlay_cmd_downloads),
        PaletteCommand.Search to stringResource(Res.string.overlay_cmd_search)
    )
    val needle = trimmed.lowercase()
    val commands = PaletteCommand.entries
        .map { PaletteItem.Command(it, commandLabels.getValue(it)) }
        .filter { needle.isEmpty() || it.label.lowercase().contains(needle) || it.command.keywords.contains(needle) }

    val songsHeader = stringResource(Res.string.overlay_palette_group_songs)
    val artistsHeader = stringResource(Res.string.overlay_palette_group_artists)
    val albumsHeader = stringResource(Res.string.overlay_palette_group_albums)
    val playlistsHeader = stringResource(Res.string.overlay_palette_group_playlists)
    val commandsHeader = stringResource(Res.string.overlay_palette_group_commands)

    val rows = remember(results, commands, songsHeader, artistsHeader, albumsHeader, playlistsHeader, commandsHeader) {
        val list = mutableListOf<PaletteRow>()
        var flat = 0
        fun group(header: String, items: List<PaletteItem>) {
            if (items.isEmpty()) return
            list += PaletteRow.Header(header)
            items.forEach { list += PaletteRow.Entry(it, flat++) }
        }
        group(songsHeader, results.tracks.map { PaletteItem.Track(it) })
        group(artistsHeader, results.artists.map { PaletteItem.Artist(it) })
        group(albumsHeader, results.albums.map { PaletteItem.Album(it) })
        group(playlistsHeader, results.playlists.map { PaletteItem.Playlist(it) })
        group(commandsHeader, commands)
        list.toList()
    }
    val entries = rows.filterIsInstance<PaletteRow.Entry>()

    var selected by remember { mutableIntStateOf(0) }
    LaunchedEffect(trimmed) { selected = 0 }
    val selection = if (entries.isEmpty()) -1 else selected.coerceIn(0, entries.lastIndex)

    // Toast texts resolved up front; commands run from key/click handlers.
    val shuffleOn = stringResource(Res.string.overlay_toast_shuffle_on)
    val shuffleOff = stringResource(Res.string.overlay_toast_shuffle_off)
    val repeatOff = stringResource(Res.string.overlay_toast_repeat_off)
    val repeatOne = stringResource(Res.string.overlay_toast_repeat_one)
    val repeatAll = stringResource(Res.string.overlay_toast_repeat_all)
    val sleepToast = stringResource(Res.string.overlay_sleep_toast_minutes, stringResource(Res.string.overlay_sleep_30))

    fun playTrack(track: MetadataTrack) {
        val cover = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url
        PaletteJobs.scope.launch {
            audioPlayerQueue.load(
                entries = listOf(QueueEntry.StreamingTrack(track = track, url = "")),
                autoPlay = true,
                startPosition = 0,
                collectionEntry = null
            )
            shellActions.showToast(getString(Res.string.overlay_toast_playing, track.title), cover)
        }
    }

    fun runCommand(command: PaletteCommand) {
        val cover = player.ui.coverModel
        when (command) {
            PaletteCommand.Shuffle -> {
                player.onShuffle()
                if (!player.isJamGuest) shellActions.showToast(if (player.ui.isShuffling) shuffleOff else shuffleOn, cover)
            }
            PaletteCommand.Repeat -> {
                player.onLoop()
                if (!player.isJamGuest) {
                    val message = when (player.ui.loopState.next()) {
                        LoopState.NONE -> repeatOff
                        LoopState.ONE -> repeatOne
                        LoopState.ALL -> repeatAll
                    }
                    shellActions.showToast(message, cover)
                }
            }
            PaletteCommand.Sleep30 -> {
                SoundSettings.setSleepTimer(SleepTimerChoice.Minutes30)
                shellActions.showToast(sleepToast, cover)
            }
            PaletteCommand.Queue -> shellActions.openQueue()
            PaletteCommand.Sound -> shellActions.openSound()
            PaletteCommand.Mini -> shellActions.toggleMini()
            PaletteCommand.Library -> onNavigate(Routes.Library)
            PaletteCommand.Stats -> onNavigate(Routes.Stats)
            PaletteCommand.Settings -> onNavigate(Routes.Settings)
            PaletteCommand.Downloads -> {
                libraryState.currentTab.value = LibraryTab.Downloads
                onNavigate(Routes.Library)
            }
            PaletteCommand.Search -> onOpenSearch()
        }
    }

    fun runItem(item: PaletteItem, play: Boolean) {
        onClose()
        when (item) {
            is PaletteItem.Track -> {
                val albumId = item.track.album?.id
                if (play || albumId == null) playTrack(item.track) else onNavigate(Routes.Album(albumId))
            }
            is PaletteItem.Artist -> onNavigate(Routes.Artist(item.artist.id))
            is PaletteItem.Album -> onNavigate(Routes.Album(item.album.id))
            is PaletteItem.Playlist -> onNavigate(Routes.Playlist(item.playlist.id))
            is PaletteItem.Command -> runCommand(item.command)
        }
    }

    // Keep the selected row in view.
    val listState = rememberLazyListState()
    LaunchedEffect(selection, rows) {
        if (selection < 0) return@LaunchedEffect
        val lazyIndex = rows.indexOfFirst { it is PaletteRow.Entry && it.flatIndex == selection }
        if (lazyIndex < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
        when {
            visible == null -> listState.scrollToItem((lazyIndex - 1).coerceAtLeast(0))
            visible.offset < info.viewportStartOffset ->
                listState.scrollBy((visible.offset - info.viewportStartOffset).toFloat())
            visible.offset + visible.size > info.viewportEndOffset ->
                listState.scrollBy((visible.offset + visible.size - info.viewportEndOffset).toFloat())
        }
    }

    val inputLabel = stringResource(Res.string.overlay_palette_input_label)

    Box(modifier = modifier.fillMaxSize()) {
        OverlayDismissLayer(onDismiss = onClose, scrim = Color(0xFF030508).copy(alpha = 0.45f))
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .width(660.dp)
                .shellGlass(
                    PaletteShape,
                    fill = Color(0xFF121821).copy(alpha = 0.92f),
                    border = colors.outline,
                    elevation = 32.dp
                )
                .blockPointerPassThrough()
                .semantics { paneTitle = inputLabel }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (entries.isNotEmpty()) selected = (selection + 1).coerceAtMost(entries.lastIndex)
                            true
                        }
                        Key.DirectionUp -> {
                            if (entries.isNotEmpty()) selected = (selection - 1).coerceAtLeast(0)
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            entries.getOrNull(selection)?.let { runItem(it.item, event.isShiftPressed) }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // Input (mockup `.in`)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MillenniumIcon(Glyphs.Search, contentDescription = null, tint = colors.onSurfaceVariant, size = 20.dp)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    val inputStyle = AppTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.overlay_palette_placeholder),
                            style = inputStyle,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = inputStyle,
                        cursorBrush = SolidColor(colors.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocus)
                            .semantics { contentDescription = inputLabel }
                    )
                }
                if (loading) {
                    Text(
                        text = stringResource(Res.string.overlay_palette_searching),
                        style = AppTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            OverlayDivider()

            if (entries.isEmpty()) {
                Text(
                    text = if (loading) {
                        stringResource(Res.string.overlay_palette_searching)
                    } else {
                        stringResource(Res.string.overlay_palette_empty, trimmed)
                    },
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        rows,
                        key = { index, row ->
                            when (row) {
                                is PaletteRow.Header -> "h:$index:${row.text}"
                                is PaletteRow.Entry -> "e:${row.flatIndex}:${row.item.stableKey()}"
                            }
                        }
                    ) { _, row ->
                        when (row) {
                            is PaletteRow.Header -> PaletteGroupHeader(row.text)
                            is PaletteRow.Entry -> PaletteResultRow(
                                item = row.item,
                                selected = row.flatIndex == selection,
                                onSelect = { selected = row.flatIndex },
                                onRun = { runItem(row.item, false) }
                            )
                        }
                    }
                }
            }

            // Footer hints (mockup `.ft`)
            OverlayDivider(modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    stringResource(Res.string.overlay_palette_hint_move),
                    stringResource(Res.string.overlay_palette_hint_run),
                    stringResource(Res.string.overlay_palette_hint_play),
                    stringResource(Res.string.overlay_palette_hint_close)
                ).forEach { hint ->
                    Text(
                        text = hint,
                        style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun PaletteItem.stableKey(): String = when (this) {
    is PaletteItem.Track -> "t:${track.id}"
    is PaletteItem.Artist -> "ar:${artist.id}"
    is PaletteItem.Album -> "al:${album.id}"
    is PaletteItem.Playlist -> "p:${playlist.id}"
    is PaletteItem.Command -> "c:${command.name}"
}

/** Top hits per kind; uses the plugin's combined search when it has one. Never throws. */
private suspend fun searchTop(repository: SearchRepository, query: String): PaletteResults = coroutineScope {
    val types = runCatching { repository.loadSupportedSearchTypes() }.getOrDefault(emptyList())
    if (MetadataSupportedSearchType.ALL in types) {
        val all = runCatching { repository.searchAll(query) }.getOrDefault(emptyList())
        PaletteResults(
            tracks = all.filterIsInstance<MetadataSearchResult.Track>().map { it.data }.take(MAX_TRACKS),
            artists = all.filterIsInstance<MetadataSearchResult.Artist>().map { it.data }.take(MAX_OTHERS),
            albums = all.filterIsInstance<MetadataSearchResult.Album>().map { it.data }.take(MAX_OTHERS),
            playlists = all.filterIsInstance<MetadataSearchResult.Playlist>().map { it.data }.take(MAX_OTHERS)
        )
    } else {
        val tracks = async {
            if (MetadataSupportedSearchType.TRACK !in types) emptyList<MetadataTrack>()
            else runCatching { repository.searchTracks(query).items.map { it.data } }.getOrDefault(emptyList())
        }
        val artists = async {
            if (MetadataSupportedSearchType.ARTIST !in types) emptyList<MetadataArtist.Basic>()
            else runCatching { repository.searchArtists(query).items.map { it.data } }.getOrDefault(emptyList())
        }
        val albums = async {
            if (MetadataSupportedSearchType.ALBUM !in types) emptyList<MetadataAlbum.Basic>()
            else runCatching { repository.searchAlbums(query).items.map { it.data } }.getOrDefault(emptyList())
        }
        val playlists = async {
            if (MetadataSupportedSearchType.PLAYLIST !in types) emptyList<MetadataPlaylist>()
            else runCatching { repository.searchPlaylists(query).items.map { it.data } }.getOrDefault(emptyList())
        }
        PaletteResults(
            tracks = tracks.await().take(MAX_TRACKS),
            artists = artists.await().take(MAX_OTHERS),
            albums = albums.await().take(MAX_OTHERS),
            playlists = playlists.await().take(MAX_OTHERS)
        )
    }
}

@Composable
private fun PaletteGroupHeader(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        color = AppTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

/** Mockup `.res`: artwork or icon, title + subtitle, key hint. */
@Composable
private fun PaletteResultRow(
    item: PaletteItem,
    selected: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    LaunchedEffect(hovered) { if (hovered) onSelect() }

    val title: String
    val subtitle: String
    val image: String?
    val imageShape: Shape
    when (item) {
        is PaletteItem.Track -> {
            title = item.track.title
            subtitle = item.track.artists.joinToString(", ") { it.name }
            image = (item.track.album?.thumbnails ?: item.track.thumbnails)?.firstOrNull()?.url
            imageShape = RoundedCornerShape(8.dp)
        }
        is PaletteItem.Artist -> {
            title = item.artist.name
            subtitle = stringResource(Res.string.overlay_palette_kind_artist)
            image = item.artist.thumbnails.firstOrNull()?.url
            imageShape = CircleShape
        }
        is PaletteItem.Album -> {
            title = item.album.title
            subtitle = stringResource(
                Res.string.overlay_palette_kind_album,
                item.album.artists.joinToString(", ") { it.name }
            )
            image = item.album.thumbnails.firstOrNull()?.url
            imageShape = RoundedCornerShape(8.dp)
        }
        is PaletteItem.Playlist -> {
            title = item.playlist.title
            val owner = item.playlist.owner?.let { it.displayName ?: it.username }
            subtitle = if (owner.isNullOrBlank()) {
                stringResource(Res.string.overlay_palette_kind_playlist)
            } else {
                stringResource(Res.string.overlay_palette_kind_playlist_by, owner)
            }
            image = item.playlist.thumbnails.firstOrNull()?.url
            imageShape = RoundedCornerShape(8.dp)
        }
        is PaletteItem.Command -> {
            title = item.label
            subtitle = ""
            image = null
            imageShape = RoundedCornerShape(10.dp)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .onFocusChanged { if (it.isFocused) onSelect() }
            .keyboardFocusRing(RowShape)
            .clip(RowShape)
            .background(if (selected) colors.hover else Color.Transparent)
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onRun
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(imageShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (item is PaletteItem.Command) {
                MillenniumIcon(item.command.icon, contentDescription = null, tint = colors.primary, size = 18.dp)
            } else if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                MillenniumIcon(
                    when (item) {
                        is PaletteItem.Artist -> Glyphs.Artists
                        is PaletteItem.Playlist -> Glyphs.Playlists
                        else -> Glyphs.Albums
                    },
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    size = 18.dp
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item is PaletteItem.Track && selected) {
            KeyHint(stringResource(Res.string.overlay_palette_key_play))
        }
        KeyHint(
            stringResource(
                if (item is PaletteItem.Command) Res.string.overlay_palette_key_run
                else Res.string.overlay_palette_key_open
            )
        )
    }
}

/** Mockup `kbd`. */
@Composable
private fun KeyHint(text: String) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        color = AppTheme.colors.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, OverlayLine, shape)
            .padding(horizontal = 6.dp)
    )
}
