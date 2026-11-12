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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import org.spotube.spotube.core.audioplayer.PlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.AnchorSide
import com.composeunstyled.Text
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadProgressIcon
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxMoon
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.a11y_off
import spotube.composeapp.generated.resources.a11y_on
import spotube.composeapp.generated.resources.player_alternative_source
import spotube.composeapp.generated.resources.common_close
import spotube.composeapp.generated.resources.player_alternative_source_content_description
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_download
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_expand_content_description
import spotube.composeapp.generated.resources.player_go_to_album
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.player_more_options_content_description
import spotube.composeapp.generated.resources.player_mute_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_nothing_playing
import spotube.composeapp.generated.resources.player_nothing_playing_hint
import spotube.composeapp.generated.resources.player_now_playing
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_or_pause_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_sleep_timer
import spotube.composeapp.generated.resources.player_track_details
import spotube.composeapp.generated.resources.player_unmute_content_description
import spotube.composeapp.generated.resources.player_up_next
import spotube.composeapp.generated.resources.player_up_next_empty
import spotube.composeapp.generated.resources.player_view_queue
import spotube.composeapp.generated.resources.player_volume_label
import spotube.composeapp.generated.resources.queue_title
import kotlin.time.Duration.Companion.milliseconds

/** Width of the docked player panel. The shell only docks it when the window can afford it. */
val AppSidePlayerWidth = 320.dp

/** Stop showing "loading" if playback never starts (resolution failed, nothing to play). */
private const val PENDING_PLAY_TIMEOUT_MS = 20_000L

// ---------------------------------------------------------------------------------------------
// Shared player state + actions (side panel and full-window view)
// ---------------------------------------------------------------------------------------------

/**
 * Everything the desktop players render and do, resolved once so the side panel and the
 * full-window view stay identical in behaviour. Jam guests get no-op transport/seek actions.
 */
@Stable
internal class PlayerController(
    val ui: PlayerUiState,
    val isJamGuest: Boolean,
    /** Play was pressed (or the player is buffering) and audio hasn't started yet. */
    val isLoading: Boolean,
    val seekProgress: Float,
    val currentTrack: MetadataTrack?,
    val onSeekChange: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onShuffle: () -> Unit,
    val onLoop: () -> Unit,
    val onToggleMute: () -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onJumpTo: (Int) -> Unit
) {
    val canSeek: Boolean get() = ui.seekDuration.inWholeMilliseconds > 0L && !isJamGuest
}

@Composable
internal fun rememberPlayerController(
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject()
): PlayerController {
    val ui = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val jamRoomService: JamRoomService = koinInject()
    val isJamGuest by jamRoomService.role
        .map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(ui.progress) }
    var lastNonZeroVolume by remember {
        mutableFloatStateOf(if (ui.volume > 0f) ui.volume else 0.6f)
    }
    // Resolving a streamed track takes seconds; show it from the moment play is pressed rather
    // than leaving a still play icon that reads as "didn't register".
    var pendingPlay by remember { mutableStateOf(false) }
    LaunchedEffect(ui.playerState) {
        if (ui.playerState == PlayerState.PLAYING) pendingPlay = false
    }
    LaunchedEffect(pendingPlay) {
        if (pendingPlay) {
            delay(PENDING_PLAY_TIMEOUT_MS)
            pendingPlay = false
        }
    }

    LaunchedEffect(ui.progress, isSeeking) {
        if (!isSeeking) seekProgress = ui.progress
    }
    LaunchedEffect(ui.volume) {
        if (ui.volume > 0f) lastNonZeroVolume = ui.volume
    }

    return PlayerController(
        ui = ui,
        isJamGuest = isJamGuest,
        isLoading = pendingPlay || ui.playerState == PlayerState.BUFFERING,
        seekProgress = seekProgress,
        currentTrack = (ui.currentQueueEntry as? QueueEntry.StreamingTrack)?.track,
        onSeekChange = {
            seekProgress = it
            isSeeking = true
        },
        onSeekFinished = {
            isSeeking = false
            val durationMillis = ui.seekDuration.inWholeMilliseconds
            if (durationMillis > 0L && !isJamGuest) {
                scope.launch {
                    audioPlayer.seekTo(
                        (durationMillis * seekProgress.coerceIn(0f, 1f)).toLong().milliseconds
                    )
                }
            }
        },
        onPlayPause = {
            // A second press while still loading cancels the start instead of re-requesting it.
            if (ui.isPlaying || pendingPlay) {
                pendingPlay = false
                scope.launch { audioPlayer.pause() }
            } else {
                pendingPlay = true
                scope.launch { audioPlayer.play() }
            }
        },
        onPrevious = { if (!isJamGuest) scope.launch { audioPlayer.skipToPrevious() } },
        onNext = { if (!isJamGuest) scope.launch { audioPlayer.skipToNext() } },
        onShuffle = { if (!isJamGuest) scope.launch { audioPlayer.shuffle(!ui.isShuffling) } },
        onLoop = { if (!isJamGuest) scope.launch { audioPlayer.loop(ui.loopState.next()) } },
        onToggleMute = {
            scope.launch {
                if (ui.volume <= 0f) {
                    audioPlayer.setVolume(lastNonZeroVolume)
                } else {
                    lastNonZeroVolume = ui.volume
                    audioPlayer.setVolume(0f)
                }
            }
        },
        onVolumeChange = {
            lastNonZeroVolume = it
            scope.launch { audioPlayer.setVolume(it) }
        },
        onJumpTo = { index -> if (!isJamGuest) scope.launch { audioPlayerQueue.jumpTo(index) } }
    )
}

// ---------------------------------------------------------------------------------------------
// Side panel
// ---------------------------------------------------------------------------------------------

/**
 * Optional now-playing *info* panel docked to the right on wide windows (Spotify's "Now playing
 * view"). Transport lives in the bottom bar; this panel shows the artwork, title/artists/like and
 * what's up next, and can be closed.
 */
@Composable
fun AppSidePlayer(
    modifier: Modifier = Modifier,
    onQueue: () -> Unit = {},
    onExpand: () -> Unit = {},
    onClose: () -> Unit = {},
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val player = rememberPlayerController(audioPlayerQueue = audioPlayerQueue)
    val navigationCommands: NavigationCommands = koinInject()
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .width(AppSidePlayerWidth)
            .fillMaxHeight()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.player_now_playing),
                style = AppTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            QuietGlyphButton(
                icon = Glyphs.Expand,
                contentDescription = stringResource(Res.string.player_expand_content_description),
                enabled = player.ui.currentQueueEntry != null,
                onClick = onExpand,
                size = 32.dp,
                iconSize = 18.dp
            )
            QuietGlyphButton(
                icon = Glyphs.Close,
                contentDescription = stringResource(Res.string.common_close),
                onClick = onClose,
                size = 32.dp,
                iconSize = 18.dp
            )
        }

        if (player.ui.currentQueueEntry == null) {
            SidePlayerEmptyState(modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                PlayerArtwork(
                    player = player,
                    onOpenAlbum = { id -> navigationCommands.navigateTo(Routes.Album(id)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                PlayerTitleBlock(
                    player = player,
                    titleStyle = AppTheme.typography.titleLarge,
                    audioPlayerQueue = audioPlayerQueue,
                    savedTracksViewModel = savedTracksViewModel,
                    onArtistClick = { id -> navigationCommands.navigateTo(Routes.Artist(id)) }
                )
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.medium)
                        .background(colors.surface)
                        .padding(12.dp)
                ) {
                    UpNextSection(
                        items = player.ui.upNext(limit = 4),
                        enabled = !player.isJamGuest,
                        onJumpTo = player.onJumpTo,
                        onViewQueue = onQueue
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Building blocks (shared with AppFullPlayer)
// ---------------------------------------------------------------------------------------------

/**
 * Low-emphasis icon button: muted glyph, neutral hover plate, no border.
 *
 * [active] makes it a toggle (`null` = plain button). A toggle that is on shows an accent glyph
 * *and* a dot under it, and reports on/off to screen readers, so state isn't colour alone.
 */
@Composable
internal fun QuietGlyphButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean? = null,
    tint: Color? = null,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp
) {
    val colors = AppTheme.colors
    val toggleState = when (active) {
        null -> null
        true -> stringResource(Res.string.a11y_on)
        false -> stringResource(Res.string.a11y_off)
    }
    MillenniumIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = if (toggleState != null) {
            Modifier.semantics { stateDescription = toggleState }
        } else {
            Modifier
        },
        style = MillenniumIconButtonStyle.Quiet,
        shape = RoundedCornerShape(10.dp),
        size = DpSize(size, size)
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            MillenniumIcon(
                icon,
                contentDescription = contentDescription,
                tint = when {
                    active == true -> colors.primary
                    tint != null -> tint
                    else -> colors.onSurfaceVariant
                },
                size = iconSize
            )
            if (active == true) {
                // Centred in the gap between the glyph and the button's bottom edge.
                val gap = (size - iconSize) / 2
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ((gap - 4.dp) / 2).coerceAtLeast(1.dp))
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }
    }
}

@Composable
internal fun PlayerArtwork(
    player: PlayerController,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val albumId = player.currentTrack?.album?.id
    val shape = AppTheme.shapes.large
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .clickable(
                enabled = albumId != null,
                // The click opens the album, so that's what it announces.
                onClickLabel = stringResource(Res.string.player_go_to_album),
                role = Role.Button
            ) { albumId?.let(onOpenAlbum) }
    ) {
        MillenniumIcon(
            Iconsax.IconsaxMusic,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            size = 48.dp,
            modifier = Modifier.align(Alignment.Center)
        )
        AsyncImage(
            model = player.ui.coverModel,
            // Decorative here: the title block names the track and album.
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun PlayerTitleBlock(
    player: PlayerController,
    titleStyle: androidx.compose.ui.text.TextStyle,
    audioPlayerQueue: AudioPlayerQueue,
    savedTracksViewModel: SavedTracksViewModel,
    onArtistClick: (String) -> Unit
) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.ui.title,
                style = titleStyle,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            PlayerArtistsText(
                artists = player.ui.artistList,
                onArtistClick = onArtistClick,
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        PlayerHeartButton(
            audioPlayerQueue = audioPlayerQueue,
            savedTracksViewModel = savedTracksViewModel
        )
    }
}

@Composable
internal fun PlayerSeekBar(player: PlayerController, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        MillenniumSlider(
            value = player.seekProgress,
            onValueChange = player.onSeekChange,
            onValueChangeFinished = player.onSeekFinished,
            enabled = player.canSeek,
            label = stringResource(Res.string.player_seek_label),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            val timeStyle = AppTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
            Text(
                text = formatPlayerTime(player.ui.position),
                style = timeStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatPlayerTime(player.ui.displayDuration),
                style = timeStyle,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/**
 * Transport: shuffle, previous, play key, next, repeat. No slab: the controls are plain glyphs and
 * the play key is the only filled element (white in the bar, the yellow accent gradient in the
 * full-window player). Toggles show state with the accent colour *and* a dot-free full-strength
 * glyph vs. muted, so it isn't colour alone.
 */
@Composable
internal fun TransportRow(player: PlayerController, large: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val ui = player.ui
    // Nothing loaded: transport has nothing to act on (play would just spin until the timeout).
    val hasTrack = ui.currentQueueEntry != null
    val enabled = !player.isJamGuest && hasTrack
    val button = if (large) 48.dp else 32.dp
    val glyph = if (large) 24.dp else 18.dp
    Row(
        modifier = modifier,
        horizontalArrangement = if (large) Arrangement.SpaceBetween else Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietGlyphButton(
            icon = Glyphs.Shuffle,
            contentDescription = stringResource(
                if (ui.isShuffling) Res.string.player_disable_shuffle_content_description
                else Res.string.player_enable_shuffle_content_description
            ),
            onClick = player.onShuffle,
            enabled = enabled,
            active = ui.isShuffling,
            size = button,
            iconSize = glyph
        )
        QuietGlyphButton(
            icon = Glyphs.SkipPrevious,
            contentDescription = stringResource(Res.string.player_previous_content_description),
            onClick = player.onPrevious,
            enabled = enabled,
            tint = colors.onSurface,
            size = button,
            iconSize = glyph
        )
        PlayKey(player = player, large = large)
        QuietGlyphButton(
            icon = Glyphs.SkipNext,
            contentDescription = stringResource(Res.string.player_next_content_description),
            onClick = player.onNext,
            enabled = enabled,
            tint = colors.onSurface,
            size = button,
            iconSize = glyph
        )
        QuietGlyphButton(
            icon = if (ui.loopState == LoopState.ONE) Glyphs.RepeatOne else Glyphs.Repeat,
            contentDescription = stringResource(
                when (ui.loopState) {
                    LoopState.NONE -> Res.string.player_loop_mode_none
                    LoopState.ONE -> Res.string.player_loop_mode_one
                    LoopState.ALL -> Res.string.player_loop_mode_all
                }
            ),
            onClick = player.onLoop,
            enabled = enabled,
            active = ui.loopState != LoopState.NONE,
            size = button,
            iconSize = glyph
        )
    }
}

/** The play/pause key: 36dp white circle in the bar, 64dp accent-gradient circle full-window. */
@Composable
internal fun PlayKey(player: PlayerController, large: Boolean) {
    val colors = AppTheme.colors
    val ui = player.ui
    val keySize = if (large) 64.dp else 36.dp
    val ink = if (large) colors.onAccentFill else colors.background
    MillenniumIconButton(
        onClick = player.onPlayPause,
        // With no track, play has nothing to start and would spin until the 20s timeout. While
        // loading (e.g. a collection into an empty queue) it stays pressable so it can cancel.
        enabled = ui.currentQueueEntry != null || player.isLoading,
        // The Quiet style's accent ring is invisible on an accent/white key; draw our own.
        modifier = Modifier.keyboardFocusRing(CircleShape),
        style = MillenniumIconButtonStyle.Quiet,
        // The ring has no label of its own; while loading, the key acts as "pause".
        contentDescription = if (player.isLoading) {
            stringResource(Res.string.player_pause_content_description)
        } else {
            null
        },
        // Transport bar: a quiet white key; full player: the accent.
        background = if (large) colors.accentBrush else SolidColor(colors.onSurface),
        foreground = ink,
        shape = CircleShape,
        size = DpSize(keySize, keySize)
    ) {
        if (player.isLoading) {
            // Still clickable while loading: a second press cancels the start.
            LoadingRing(color = ink, size = if (large) 26.dp else 16.dp)
        } else {
            MillenniumIcon(
                if (ui.isPlaying) Glyphs.Pause else Glyphs.Play,
                contentDescription = stringResource(
                    if (ui.isPlaying) Res.string.player_pause_content_description
                    else Res.string.player_play_or_pause_content_description
                ),
                tint = ink,
                size = if (large) 26.dp else 16.dp
            )
        }
    }
}

/**
 * The "more" menu for the bar: everything that isn't transport. Download and alternative source
 * live here so the bar stays quiet.
 */
@Composable
internal fun PlayerMoreMenu(
    player: PlayerController,
    downloadsViewModel: DownloadsViewModel,
    playerOptionsViewModel: PlayerOptionsViewModel,
    onAlternativeSource: () -> Unit,
    onGoToAlbum: (String) -> Unit
) {
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack = player.currentTrack
    MillenniumDropdownMenu(
        expanded = playerOptionsUiState.isMoreOptionsMenuOpen,
        onExpandedChange = { expanded ->
            if (!expanded) playerOptionsViewModel.dismissMoreOptionsMenu()
        },
        side = AnchorSide.Top,
        anchor = {
            QuietGlyphButton(
                icon = Glyphs.More,
                contentDescription = stringResource(Res.string.player_more_options_content_description),
                enabled = player.ui.currentQueueEntry != null,
                onClick = { playerOptionsViewModel.showMoreOptionsMenu() }
            )
        }
    ) {
        MillenniumDropdownMenuItem(
            label = stringResource(Res.string.player_download),
            icon = Glyphs.Download,
            onClick = {
                playerOptionsViewModel.dismissMoreOptionsMenu()
                currentTrack?.let { downloadsViewModel.downloadTrack(it) }
            })
        MillenniumDropdownMenuItem(
            label = stringResource(Res.string.player_alternative_source),
            icon = Glyphs.SwapSource,
            onClick = {
                playerOptionsViewModel.dismissMoreOptionsMenu()
                onAlternativeSource()
            })
        MillenniumDropdownMenuItem(
            label = stringResource(Res.string.player_go_to_album),
            icon = Iconsax.IconsaxCd,
            onClick = {
                playerOptionsViewModel.dismissMoreOptionsMenu()
                currentTrack?.album?.id?.let(onGoToAlbum)
            })
        MillenniumDropdownMenuItem(
            label = stringResource(Res.string.player_sleep_timer),
            icon = Iconsax.IconsaxMoon,
            onClick = {
                playerOptionsViewModel.dismissMoreOptionsMenu()
                playerOptionsViewModel.showSleepTimerDialog()
            })
        MillenniumDropdownMenuItem(
            label = stringResource(Res.string.player_track_details),
            icon = Iconsax.IconsaxInformation,
            onClick = {
                playerOptionsViewModel.dismissMoreOptionsMenu()
                playerOptionsViewModel.showTrackDetails()
            })
    }
}

@Composable
internal fun PlayerSecondaryActions(
    player: PlayerController,
    downloadsViewModel: DownloadsViewModel,
    playerOptionsViewModel: PlayerOptionsViewModel,
    onLyrics: () -> Unit,
    onAlternativeSource: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    showLyrics: Boolean = true
) {
    val colors = AppTheme.colors
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack = player.currentTrack

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLyrics) {
            QuietGlyphButton(
                icon = Glyphs.Lyrics,
                contentDescription = stringResource(Res.string.player_lyrics),
                onClick = onLyrics
            )
        }
        MillenniumIconButton(
            style = MillenniumIconButtonStyle.Quiet,
            enabled = currentTrack != null,
            shape = RoundedCornerShape(10.dp),
            size = DpSize(36.dp, 36.dp),
            onClick = { currentTrack?.let { downloadsViewModel.downloadTrack(it) } }
        ) {
            DownloadProgressIcon(
                track = currentTrack,
                icon = Glyphs.Download,
                contentDescription = stringResource(Res.string.player_download),
                size = 20.dp,
                tint = colors.onSurfaceVariant
            )
        }
        QuietGlyphButton(
            icon = Glyphs.SwapSource,
            contentDescription = stringResource(Res.string.player_alternative_source_content_description),
            onClick = onAlternativeSource
        )
        MillenniumDropdownMenu(
            expanded = playerOptionsUiState.isMoreOptionsMenuOpen,
            onExpandedChange = { expanded ->
                if (!expanded) playerOptionsViewModel.dismissMoreOptionsMenu()
            },
            side = AnchorSide.Top,
            anchor = {
                QuietGlyphButton(
                    icon = Glyphs.More,
                    contentDescription = stringResource(Res.string.player_more_options_content_description),
                    onClick = { playerOptionsViewModel.showMoreOptionsMenu() }
                )
            }
        ) {
            MillenniumDropdownMenuItem(
                label = stringResource(Res.string.player_go_to_album),
                icon = Iconsax.IconsaxCd,
                onClick = {
                    playerOptionsViewModel.dismissMoreOptionsMenu()
                    currentTrack?.album?.id?.let(onGoToAlbum)
                })
            MillenniumDropdownMenuItem(
                label = stringResource(Res.string.player_sleep_timer),
                icon = Iconsax.IconsaxMoon,
                onClick = {
                    playerOptionsViewModel.dismissMoreOptionsMenu()
                    playerOptionsViewModel.showSleepTimerDialog()
                })
            MillenniumDropdownMenuItem(
                label = stringResource(Res.string.player_track_details),
                icon = Iconsax.IconsaxInformation,
                onClick = {
                    playerOptionsViewModel.dismissMoreOptionsMenu()
                    playerOptionsViewModel.showTrackDetails()
                })
        }
    }
}

@Composable
internal fun PlayerVolumeRow(player: PlayerController, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val volume = player.ui.volume
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        QuietGlyphButton(
            icon = when {
                volume <= 0f -> Glyphs.VolumeMute
                volume < 0.5f -> Glyphs.VolumeLow
                else -> Glyphs.VolumeHigh
            },
            contentDescription = stringResource(
                if (volume <= 0f) Res.string.player_unmute_content_description
                else Res.string.player_mute_content_description
            ),
            onClick = player.onToggleMute
        )
        MillenniumSlider(
            value = volume,
            onValueChange = player.onVolumeChange,
            label = stringResource(Res.string.player_volume_label),
            activeTrackBackground = SolidColor(colors.onSurfaceVariant),
            thumbBackground = SolidColor(colors.onSurface),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UpNextSection(
    items: List<UpNextItem>,
    enabled: Boolean,
    onJumpTo: (Int) -> Unit,
    onViewQueue: () -> Unit
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.player_up_next),
                style = AppTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.player_view_queue),
                style = AppTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier
                    .keyboardFocusRing(AppTheme.shapes.small)
                    .clip(AppTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onViewQueue)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        if (items.isEmpty()) {
            Text(
                text = stringResource(Res.string.player_up_next_empty),
                style = AppTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            items.forEach { item ->
                UpNextRow(item = item, enabled = enabled, onClick = { onJumpTo(item.index) })
            }
        }
    }
}

@Composable
private fun UpNextRow(item: UpNextItem, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .keyboardFocusRing(AppTheme.shapes.small)
            .clip(AppTheme.shapes.small)
            .background(if (hovered && enabled) colors.hover else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = item.entry.artworkModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = item.entry.displayTitle(),
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.entry.displayArtists(),
                style = AppTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SidePlayerEmptyState(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(AppTheme.shapes.large)
                .background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                Glyphs.Play,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                size = 28.dp
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.player_nothing_playing),
            style = AppTheme.typography.titleMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.player_nothing_playing_hint),
            style = AppTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Small indeterminate ring: a 270° arc turning once a second. Flat colour, no gradient. */
@Composable
internal fun LoadingRing(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading_ring")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "loading_ring_angle"
    )
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.12f
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - stroke,
                    this.size.height - stroke
                ),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
