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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.spotube.spotube.modules.shell.player_queue.QueueItemUi
import org.spotube.spotube.resources.glyphs.Glyphs
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.overlay_queue_autoplay_body
import spotube.composeapp.generated.resources.overlay_queue_autoplay_title
import spotube.composeapp.generated.resources.overlay_queue_clear
import spotube.composeapp.generated.resources.overlay_queue_clear_description
import spotube.composeapp.generated.resources.overlay_queue_cleared
import spotube.composeapp.generated.resources.overlay_queue_empty
import spotube.composeapp.generated.resources.overlay_queue_now_playing
import spotube.composeapp.generated.resources.overlay_queue_playlist_name
import spotube.composeapp.generated.resources.overlay_queue_read_only
import spotube.composeapp.generated.resources.overlay_queue_remove
import spotube.composeapp.generated.resources.overlay_queue_reorder
import spotube.composeapp.generated.resources.overlay_queue_save
import spotube.composeapp.generated.resources.overlay_queue_save_failed
import spotube.composeapp.generated.resources.overlay_queue_save_nothing
import spotube.composeapp.generated.resources.overlay_queue_save_sign_in
import spotube.composeapp.generated.resources.overlay_queue_saved
import spotube.composeapp.generated.resources.overlay_queue_title
import spotube.composeapp.generated.resources.overlay_queue_up_next
import spotube.composeapp.generated.resources.overlay_queue_up_next_empty
import spotube.composeapp.generated.resources.overlay_queue_up_next_plain

/** Plugin playlist APIs (Spotify-like) take at most this many tracks per call. */
private const val PLAYLIST_TRACKS_PER_CALL = 100

/**
 * Work that must outlive the drawer: saving a playlist, and the toast's Undo after Clear (the
 * drawer may be closed by then, which would cancel a composition scope).
 */
private object QueueDrawerJobs {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/**
 * The queue drawer (mockup `#drawer`): what's playing, the reorderable "up next" list (drag the
 * grip, or Alt+Up/Down on a focused row; Delete removes), autoplay, save as playlist, clear.
 * Queue logic is [PlayerQueueContentViewModel]'s, shared with the compact queue sheet.
 */
@Composable
internal fun QueueDrawer(
    onClose: () -> Unit,
    queueViewModel: PlayerQueueContentViewModel,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    libraryRepository: LibraryRepository = koinInject()
) {
    val colors = AppTheme.colors
    val state by queueViewModel.queueContentUiState.collectAsState()
    val userSettings by settingsRepository.userSettings.collectAsState()
    val shellActions = LocalShellActions.current
    val title = stringResource(Res.string.overlay_queue_title)

    // The compact sheet's filter would hide rows here and block reordering.
    LaunchedEffect(Unit) { queueViewModel.setQueueFilter("") }

    val items = state.displayItems
    val currentIndex = items.indexOfFirst { it.isCurrent }
    val nowPlaying = items.getOrNull(currentIndex)
    // Rows after the current one. Their LazyColumn index + [upNextOffset] = index in the queue.
    val upNextOffset = if (currentIndex >= 0) currentIndex + 1 else 0
    val upNext = items.drop(upNextOffset)
    val canEdit = !state.isReadOnly && !state.isFiltered

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (!canEdit) return@rememberReorderableLazyListState
        queueViewModel.onMove(from.index + upNextOffset, to.index + upNextOffset)
    }

    val clearedMessage = stringResource(Res.string.overlay_queue_cleared)

    fun clearQueue() {
        val snapshot = audioPlayerQueue.queueFlow.value
        if (snapshot.isEmpty()) return
        val current = audioPlayerQueue.currentQueueEntryFlow.value
        val collection = audioPlayerQueue.currentCollectionEntryFlow.value
        val restoreIndex = snapshot.indexOf(current).coerceAtLeast(0)
        queueViewModel.clearQueue()
        shellActions.showToast(
            message = clearedMessage,
            coverModel = current?.artworkModel,
            undo = {
                QueueDrawerJobs.scope.launch {
                    audioPlayerQueue.load(
                        entries = snapshot,
                        autoPlay = false,
                        startPosition = restoreIndex,
                        collectionEntry = collection
                    )
                }
            }
        )
    }

    fun saveAsPlaylist() {
        val entries = audioPlayerQueue.queueFlow.value
        val trackIds = entries.filterIsInstance<QueueEntry.StreamingTrack>().map { it.track.id }
        val nameSeed = nowPlaying?.title ?: items.firstOrNull()?.title.orEmpty()
        val cover = nowPlaying?.artwork
        QueueDrawerJobs.scope.launch {
            val message = if (trackIds.isEmpty()) {
                getString(Res.string.overlay_queue_save_nothing)
            } else {
                val name = getString(Res.string.overlay_queue_playlist_name, nameSeed)
                runCatching {
                    val chunks = trackIds.chunked(PLAYLIST_TRACKS_PER_CALL)
                    val created = libraryRepository.createPlaylist(
                        name = name,
                        description = null,
                        isPublic = false,
                        isCollaborating = false,
                        imageBase64 = "",
                        trackIds = chunks.first()
                    )
                    if (created != null) {
                        chunks.drop(1).forEach { libraryRepository.addTracksToPlaylist(created.id, it) }
                    }
                    created
                }.fold(
                    onSuccess = { created ->
                        if (created == null) {
                            getString(Res.string.overlay_queue_save_sign_in)
                        } else {
                            getString(Res.string.overlay_queue_saved, created.title)
                        }
                    },
                    onFailure = { getString(Res.string.overlay_queue_save_failed) }
                )
            }
            shellActions.showToast(message, cover)
        }
    }

    RightOverlayPanel(title = title, onClose = onClose, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 18.dp)) {
            OverlayHeader(
                title = title,
                onClose = onClose,
                modifier = Modifier.padding(horizontal = 6.dp),
                actions = {
                    if (canEdit && items.isNotEmpty()) {
                        OverlaySelectBox(
                            label = stringResource(Res.string.overlay_queue_clear),
                            onClick = ::clearQueue,
                            contentDescription = stringResource(Res.string.overlay_queue_clear_description)
                        )
                    }
                }
            )

            if (items.isEmpty()) {
                DrawerLabel(stringResource(Res.string.overlay_queue_empty), modifier = Modifier.weight(1f))
            } else {
                if (nowPlaying != null) {
                    DrawerLabel(stringResource(Res.string.overlay_queue_now_playing))
                    QueueDrawerRow(item = nowPlaying, highlighted = true)
                }
                DrawerLabel(
                    stringResource(
                        if (canEdit && upNext.size > 1) Res.string.overlay_queue_up_next
                        else Res.string.overlay_queue_up_next_plain
                    )
                )
                if (state.isReadOnly) {
                    Text(
                        text = stringResource(Res.string.overlay_queue_read_only),
                        style = AppTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                    )
                }
                if (upNext.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.overlay_queue_up_next_empty),
                        style = AppTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(upNext, key = { it.id }) { item ->
                            ReorderableItem(reorderState, key = item.id) { isDragging ->
                                val handle = if (canEdit) {
                                    Modifier.draggableHandle(
                                        onDragStarted = { queueViewModel.onDragStart() },
                                        onDragStopped = { queueViewModel.onDragStop() }
                                    )
                                } else {
                                    null
                                }
                                QueueDrawerRow(
                                    item = item,
                                    highlighted = false,
                                    dragging = isDragging,
                                    onClick = if (state.isReadOnly) null else {
                                        { queueViewModel.playQueueItem(item.originalIndex) }
                                    },
                                    onRemove = if (canEdit) {
                                        { queueViewModel.removeQueueItem(item.originalIndex) }
                                    } else {
                                        null
                                    },
                                    onMoveBy = if (canEdit) {
                                        { delta: Int ->
                                            val target = item.originalIndex + delta
                                            if (target >= upNextOffset && target < items.size) {
                                                queueViewModel.moveQueueItem(item.originalIndex, target)
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    dragHandle = handle
                                )
                            }
                        }
                    }
                }
            }

            // Footer (mockup `.dfoot`)
            OverlayDivider()
            OverlayToggleRow(
                title = stringResource(Res.string.overlay_queue_autoplay_title),
                body = stringResource(Res.string.overlay_queue_autoplay_body),
                checked = userSettings.enableEndlessPlayback,
                onCheckedChange = { enabled ->
                    QueueDrawerJobs.scope.launch {
                        val latest = settingsRepository.userSettings.value
                        settingsRepository.updateSettings(latest.copy(enableEndlessPlayback = enabled))
                    }
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GlassPillButton(
                    label = stringResource(Res.string.overlay_queue_save),
                    onClick = ::saveAsPlaylist,
                    enabled = items.isNotEmpty()
                )
            }
        }
    }
}

@Composable
private fun DrawerLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)
    )
}

/**
 * Mockup `.qi`: cover, title (accent when [highlighted]), artists; on hover or focus a remove
 * button, and the grip when [dragHandle] is set.
 */
@Composable
private fun QueueDrawerRow(
    item: QueueItemUi,
    highlighted: Boolean,
    dragging: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onMoveBy: ((Int) -> Unit)? = null,
    dragHandle: Modifier? = null
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val showActions = hovered || focusWithin || dragging

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusWithin = it.hasFocus }
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(
                when {
                    dragging -> Color.White.copy(alpha = 0.12f)
                    hovered && onClick != null -> colors.hover
                    else -> Color.Transparent
                }
            )
            .hoverable(interaction)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    onMoveBy != null && event.isAltPressed && event.key == Key.DirectionUp -> {
                        onMoveBy(-1); true
                    }
                    onMoveBy != null && event.isAltPressed && event.key == Key.DirectionDown -> {
                        onMoveBy(1); true
                    }
                    onRemove != null && (event.key == Key.Delete || event.key == Key.Backspace) -> {
                        onRemove(); true
                    }
                    else -> false
                }
            }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (item.artwork != null) {
                AsyncImage(
                    model = item.artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                MillenniumIcon(Glyphs.Albums, contentDescription = null, tint = colors.onSurfaceVariant, size = 18.dp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (highlighted) colors.primary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onRemove != null) {
            // Kept in place (only faded) so keyboard users can still Tab to it.
            Box(modifier = Modifier.alpha(if (showActions) 1f else 0f)) {
                QuietGlyphButton(
                    icon = Glyphs.Close,
                    contentDescription = stringResource(Res.string.overlay_queue_remove, item.title),
                    onClick = onRemove,
                    size = 28.dp,
                    iconSize = 16.dp
                )
            }
        }
        if (dragHandle != null) {
            MillenniumIcon(
                OverlayGripGlyph,
                contentDescription = stringResource(Res.string.overlay_queue_reorder, item.title),
                tint = colors.onSurfaceVariant.copy(alpha = if (showActions) 1f else 0.6f),
                size = 18.dp,
                modifier = dragHandle
            )
        } else if (!highlighted) {
            Text(
                text = item.durationLabel,
                style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** Mockup `.pill.glass` (36dp): a quiet glass button with a label. */
@Composable
private fun GlassPillButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .height(36.dp)
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(Color.White.copy(alpha = if (hovered && enabled) 0.12f else 0.06f))
            .border(1.dp, OverlayLine, shape)
            .hoverable(interaction, enabled = enabled)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(Glyphs.Playlists, contentDescription = null, tint = colors.onSurface, size = 16.dp)
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = colors.onSurface,
            maxLines = 1
        )
    }
}
