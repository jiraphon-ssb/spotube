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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.lyrics.LyricsScreen
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContent
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_collapse_content_description
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.queue_title

private enum class FullPlayerPane { Lyrics, Queue }

/** Warm lamp light over the shop counter. */
private val CounterLamp = Color(0xFF4A3420)

/**
 * Desktop "now playing" that takes over the whole window, in the spirit of Apple Music's full
 * screen player: a record sliding out of its sleeve, title and the same control deck on the left; lyrics or the queue on the
 * right. Close with the collapse button or Escape. It reuses [rememberPlayerController] and the
 * side panel's building blocks, so behaviour is identical.
 */
@Composable
fun AppFullPlayer(
    onCollapse: () -> Unit,
    onAlternativeSource: () -> Unit,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    downloadsViewModel: DownloadsViewModel = koinViewModel(),
    playerOptionsViewModel: PlayerOptionsViewModel = koinViewModel(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val player = rememberPlayerController(audioPlayerQueue = audioPlayerQueue)
    val navigationCommands: NavigationCommands = koinInject()
    val colors = AppTheme.colors
    var pane by remember { mutableStateOf(FullPlayerPane.Lyrics) }
    val focusRequester = remember { FocusRequester() }
    // macOS draws its window buttons over the top-left corner of the content.
    val topInset = remember { if (getPlatform().type == PlatformType.MacOS) 28.dp else 0.dp }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Nothing left to show: fall back to the normal shell.
    LaunchedEffect(player.ui.currentQueueEntry == null) {
        if (player.ui.currentQueueEntry == null) onCollapse()
    }

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    // Backdrop tinted from the artwork (Apple Music style) instead of the app accent.
    val tint by rememberArtworkTint(player.ui.coverModel, fallback = colors.surfaceContainerHigh)
    // The counter lamp: the cover's colour warmed toward the shop's brown.
    // Read inside the draw lambda so the colour fade redraws instead of recomposing.
    fun glow() = lerp(tint, CounterLamp, 0.45f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            // Warm accent bloom behind the record, fading out across the window.
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow().copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.45f),
                        radius = size.minDimension * 0.85f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow().copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.minDimension * 0.7f
                    )
                )
            }
            // Swallow clicks so nothing underneath reacts.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onCollapse()
                    true
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 24.dp, top = 16.dp + topInset, bottom = 24.dp)
        ) {
            // Header: pane switch + collapse.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                PaneSwitch(selected = pane, onSelect = { pane = it })
                Spacer(Modifier.width(8.dp))
                QuietGlyphButton(
                    icon = Glyphs.Collapse,
                    contentDescription = stringResource(Res.string.player_collapse_content_description),
                    onClick = onCollapse
                )
            }

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // The left column never gets wider than the artwork reads well at.
                // Room for the sleeve plus the record sliding out of it.
                val leftWidth = (maxWidth * 0.5f).coerceIn(340.dp, 580.dp)
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(leftWidth)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        VinylArtwork(
                            player = player,
                            onOpenAlbum = { id ->
                                onCollapse()
                                navigationCommands.navigateTo(Routes.Album(id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        PlayerTitleBlock(
                            player = player,
                            titleStyle = AppTheme.typography.headlineMedium,
                            audioPlayerQueue = audioPlayerQueue,
                            savedTracksViewModel = savedTracksViewModel,
                            onArtistClick = { id ->
                                onCollapse()
                                navigationCommands.navigateTo(Routes.Artist(id))
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        PlayerSeekBar(player = player)
                        Spacer(Modifier.height(16.dp))
                        TransportRow(player = player, large = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerSecondaryActions(
                                player = player,
                                downloadsViewModel = downloadsViewModel,
                                playerOptionsViewModel = playerOptionsViewModel,
                                onLyrics = { pane = FullPlayerPane.Lyrics },
                                onAlternativeSource = onAlternativeSource,
                                onGoToAlbum = { id ->
                                    onCollapse()
                                    navigationCommands.navigateTo(Routes.Album(id))
                                },
                                showLyrics = false
                            )
                            Spacer(Modifier.width(12.dp))
                            PlayerVolumeRow(player = player, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.width(40.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(AppTheme.shapes.large)
                            .background(colors.surface)
                    ) {
                        when (pane) {
                            FullPlayerPane.Lyrics -> LyricsScreen(modifier = Modifier.fillMaxSize(), embedded = true)
                            FullPlayerPane.Queue -> PlayerQueueContent(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

/** Two-option segmented switch for the right-hand pane. */
@Composable
private fun PaneSwitch(selected: FullPlayerPane, onSelect: (FullPlayerPane) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FullPlayerPane.entries.forEach { option ->
            val isSelected = option == selected
            Text(
                text = stringResource(
                    when (option) {
                        FullPlayerPane.Lyrics -> Res.string.player_lyrics
                        FullPlayerPane.Queue -> Res.string.queue_title
                    }
                ),
                style = AppTheme.typography.labelLarge,
                color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.surfaceContainerHighest else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
