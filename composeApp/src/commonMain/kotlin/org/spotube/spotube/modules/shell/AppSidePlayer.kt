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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadProgressIcon
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxMoon
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxVolumeCross
import org.spotube.spotube.resources.iconsax.IconsaxVolumeHigh
import org.spotube.spotube.resources.iconsax.IconsaxVolumeLow
import org.spotube.spotube.resources.iconsax.SwapHorizontal2
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_alternative_source_content_description
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_download
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
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

/**
 * The desktop "now playing" panel docked to the right of the content.
 *
 * Top to bottom: header (+ queue), large artwork (opens the album), title / artists / like, seek bar,
 * the accent control deck (shuffle, previous, play-pause, next, repeat), secondary actions
 * (lyrics, download, alternative source, more), the next three queue entries (click to jump), and a
 * volume row pinned to the bottom. Everything a Jam guest isn't allowed to change is disabled.
 *
 * It replaces [AppLargePlayer] at wide widths and offers the same actions, so nothing is lost when
 * the shell switches between the two.
 */
@Composable
fun AppSidePlayer(
    modifier: Modifier = Modifier,
    onQueue: () -> Unit = {},
    onAlternativeSource: () -> Unit = {},
    onLyrics: () -> Unit = {},
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    downloadsViewModel: DownloadsViewModel = koinViewModel(),
    playerOptionsViewModel: PlayerOptionsViewModel = koinViewModel(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val jamRoomService: JamRoomService = koinInject()
    val isJamGuest by jamRoomService.role
        .map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val currentEntry = playerUiState.currentQueueEntry
    val currentTrack = (currentEntry as? QueueEntry.StreamingTrack)?.track
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val navigationCommands: NavigationCommands = koinInject()
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }
    var lastNonZeroVolume by remember {
        mutableFloatStateOf(if (playerUiState.volume > 0f) playerUiState.volume else 0.6f)
    }
    val colors = AppTheme.colors

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    LaunchedEffect(playerUiState.progress, isSeeking) {
        if (!isSeeking) seekProgress = playerUiState.progress
    }
    LaunchedEffect(playerUiState.volume) {
        if (playerUiState.volume > 0f) lastNonZeroVolume = playerUiState.volume
    }

    fun onPlayPause() {
        scope.launch { if (playerUiState.isPlaying) audioPlayer.pause() else audioPlayer.play() }
    }

    fun onSeekFinished() {
        val durationMillis = playerUiState.seekDuration.inWholeMilliseconds
        if (durationMillis <= 0L) return
        scope.launch {
            audioPlayer.seekTo((durationMillis * seekProgress.coerceIn(0f, 1f)).toLong().milliseconds)
        }
    }

    fun onToggleMute() {
        scope.launch {
            if (playerUiState.volume <= 0f) {
                audioPlayer.setVolume(lastNonZeroVolume)
            } else {
                lastNonZeroVolume = playerUiState.volume
                audioPlayer.setVolume(0f)
            }
        }
    }

    Column(
        modifier = modifier
            .width(AppSidePlayerWidth)
            .fillMaxHeight()
            .background(colors.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.player_now_playing),
                style = AppTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            MillenniumIconButton(style = MillenniumIconButtonStyle.Quiet, onClick = onQueue) {
                MillenniumIcon(
                    Iconsax.IconsaxMusicFilter,
                    contentDescription = stringResource(Res.string.queue_title),
                    tint = colors.onSurfaceVariant
                )
            }
        }

        if (currentEntry == null) {
            SidePlayerEmptyState(modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Artwork: opens the album when there is one.
                val albumId = currentTrack?.album?.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(AppTheme.shapes.large)
                        .background(colors.surfaceContainerHigh)
                        .clickable(enabled = albumId != null) {
                            albumId?.let { navigationCommands.navigateTo(Routes.Album(it)) }
                        }
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxMusic,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        size = 48.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    AsyncImage(
                        model = playerUiState.coverModel,
                        contentDescription = playerUiState.album,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Title, artists, like
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playerUiState.title,
                            style = AppTheme.typography.titleLarge,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        PlayerArtistsText(
                            artists = playerUiState.artistList,
                            onArtistClick = { artistId ->
                                navigationCommands.navigateTo(Routes.Artist(artistId))
                            },
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

                Spacer(Modifier.height(14.dp))

                // Seek
                MillenniumSlider(
                    value = seekProgress,
                    onValueChange = {
                        seekProgress = it
                        isSeeking = true
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onSeekFinished()
                    },
                    enabled = playerUiState.seekDuration.inWholeMilliseconds > 0L && !isJamGuest,
                    label = stringResource(Res.string.player_seek_label),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    val timeStyle = AppTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                    Text(
                        text = formatPlayerTime(playerUiState.position),
                        style = timeStyle,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatPlayerTime(playerUiState.displayDuration),
                        style = timeStyle,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(14.dp))

                ControlDeck(
                    isPlaying = playerUiState.isPlaying,
                    isShuffling = playerUiState.isShuffling,
                    loopState = playerUiState.loopState,
                    controlsEnabled = !isJamGuest,
                    onPlayPause = ::onPlayPause,
                    onPrevious = { scope.launch { audioPlayer.skipToPrevious() } },
                    onNext = { scope.launch { audioPlayer.skipToNext() } },
                    onShuffle = { scope.launch { audioPlayer.shuffle(!playerUiState.isShuffling) } },
                    onLoop = { scope.launch { audioPlayer.loop(playerUiState.loopState.next()) } }
                )

                Spacer(Modifier.height(8.dp))

                // Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MillenniumIconButton(style = MillenniumIconButtonStyle.Quiet, onClick = onLyrics) {
                        MillenniumIcon(
                            Iconsax.IconsaxMusic,
                            contentDescription = stringResource(Res.string.player_lyrics),
                            tint = colors.onSurfaceVariant
                        )
                    }
                    MillenniumIconButton(
                        style = MillenniumIconButtonStyle.Quiet,
                        enabled = currentTrack != null,
                        onClick = { currentTrack?.let { downloadsViewModel.downloadTrack(it) } }
                    ) {
                        DownloadProgressIcon(
                            track = currentTrack,
                            icon = Iconsax.IconsaxDirectboxReceive,
                            contentDescription = stringResource(Res.string.player_download),
                            size = 20.dp,
                            tint = colors.onSurfaceVariant
                        )
                    }
                    MillenniumIconButton(
                        style = MillenniumIconButtonStyle.Quiet,
                        onClick = onAlternativeSource
                    ) {
                        MillenniumIcon(
                            Iconsax.SwapHorizontal2,
                            contentDescription = stringResource(Res.string.player_alternative_source_content_description),
                            tint = colors.onSurfaceVariant
                        )
                    }
                    MillenniumDropdownMenu(
                        expanded = playerOptionsUiState.isMoreOptionsMenuOpen,
                        onExpandedChange = { expanded ->
                            if (!expanded) playerOptionsViewModel.dismissMoreOptionsMenu()
                        },
                        side = AnchorSide.Top,
                        anchor = {
                            MillenniumIconButton(
                                style = MillenniumIconButtonStyle.Quiet,
                                onClick = { playerOptionsViewModel.showMoreOptionsMenu() }
                            ) {
                                MillenniumIcon(
                                    Iconsax.Iconsax3DotsMore,
                                    contentDescription = stringResource(Res.string.player_more_options_content_description),
                                    tint = colors.onSurfaceVariant
                                )
                            }
                        }
                    ) {
                        MillenniumDropdownMenuItem(
                            label = stringResource(Res.string.player_go_to_album),
                            icon = Iconsax.IconsaxCd,
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsMenu()
                                currentTrack?.album?.id?.let { albumId ->
                                    navigationCommands.navigateTo(Routes.Album(albumId))
                                }
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

                Spacer(Modifier.height(16.dp))

                UpNextSection(
                    items = playerUiState.upNext(limit = 3),
                    enabled = !isJamGuest,
                    onJumpTo = { index -> scope.launch { audioPlayerQueue.jumpTo(index) } },
                    onViewQueue = onQueue
                )
            }
        }

        // Volume, pinned to the bottom of the panel.
        MillenniumSeparator(modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MillenniumIconButton(style = MillenniumIconButtonStyle.Quiet, onClick = ::onToggleMute) {
                val volumeIcon = when {
                    playerUiState.volume <= 0f -> Iconsax.IconsaxVolumeCross
                    playerUiState.volume < 0.5f -> Iconsax.IconsaxVolumeLow
                    else -> Iconsax.IconsaxVolumeHigh
                }
                MillenniumIcon(
                    imageVector = volumeIcon,
                    contentDescription = stringResource(
                        if (playerUiState.volume <= 0f) Res.string.player_unmute_content_description
                        else Res.string.player_mute_content_description
                    ),
                    tint = colors.onSurfaceVariant
                )
            }
            MillenniumSlider(
                value = playerUiState.volume,
                onValueChange = {
                    lastNonZeroVolume = it
                    scope.launch { audioPlayer.setVolume(it) }
                },
                label = stringResource(Res.string.player_volume_label),
                activeTrackBackground = SolidColor(colors.onSurfaceVariant),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The one loud element of the shell: an accent slab holding the transport controls, with a white
 * play/pause key so the primary action reads first.
 */
@Composable
private fun ControlDeck(
    isPlaying: Boolean,
    isShuffling: Boolean,
    loopState: LoopState,
    controlsEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onLoop: () -> Unit
) {
    val colors = AppTheme.colors
    val onFill = colors.onAccentFill

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(AppTheme.shapes.large)
            .background(colors.accentFill)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeckButton(
            icon = Iconsax.IconsaxShuffle,
            contentDescription = stringResource(
                if (isShuffling) Res.string.player_disable_shuffle_content_description
                else Res.string.player_enable_shuffle_content_description
            ),
            active = isShuffling,
            enabled = controlsEnabled,
            onClick = onShuffle
        )
        DeckButton(
            icon = Iconsax.IconsaxPrevious,
            contentDescription = stringResource(Res.string.player_previous_content_description),
            enabled = controlsEnabled,
            onClick = onPrevious
        )
        MillenniumIconButton(
            onClick = onPlayPause,
            style = MillenniumIconButtonStyle.Quiet,
            background = SolidColor(onFill),
            foreground = colors.accentFill,
            shape = RoundedCornerShape(14.dp),
            size = DpSize(52.dp, 52.dp)
        ) {
            MillenniumIcon(
                if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.player_pause_content_description
                    else Res.string.player_play_or_pause_content_description
                ),
                tint = colors.accentFill,
                size = 24.dp
            )
        }
        DeckButton(
            icon = Iconsax.IconsaxNext,
            contentDescription = stringResource(Res.string.player_next_content_description),
            enabled = controlsEnabled,
            onClick = onNext
        )
        DeckButton(
            icon = when (loopState) {
                LoopState.NONE -> Iconsax.IconsaxRepeateMusic
                LoopState.ONE -> Iconsax.IconsaxRepeateOne
                LoopState.ALL -> Iconsax.IconsaxRepeatMusic
            },
            contentDescription = stringResource(
                when (loopState) {
                    LoopState.NONE -> Res.string.player_loop_mode_none
                    LoopState.ONE -> Res.string.player_loop_mode_one
                    LoopState.ALL -> Res.string.player_loop_mode_all
                }
            ),
            active = loopState != LoopState.NONE,
            enabled = controlsEnabled,
            onClick = onLoop
        )
    }
}

/** A transport button on the accent deck. "Active" toggles get a translucent plate. */
@Composable
private fun DeckButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true
) {
    val onFill = AppTheme.colors.onAccentFill
    MillenniumIconButton(
        onClick = onClick,
        enabled = enabled,
        style = MillenniumIconButtonStyle.Quiet,
        background = SolidColor(if (active) onFill.copy(alpha = 0.18f) else Color.Transparent),
        foreground = onFill,
        shape = RoundedCornerShape(10.dp),
        size = DpSize(40.dp, 40.dp)
    ) {
        MillenniumIcon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) onFill else onFill.copy(alpha = 0.82f),
            size = 20.dp
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
                    .clip(AppTheme.shapes.small)
                    .clickable(onClick = onViewQueue)
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
                Iconsax.IconsaxMusic,
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
