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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_expand_content_description
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_toggle_now_playing_panel
import spotube.composeapp.generated.resources.queue_title

/** Height of the desktop transport bar. */
private val BarHeight = 76.dp

/**
 * The desktop transport bar, always at the bottom of the window (Spotify / Apple Music pattern):
 *
 * - left: artwork (opens the full-window player), title, artists, like
 * - centre: transport (white play key) over a seek bar
 * - right: lyrics, queue, now-playing panel toggle, more (download, alternative source, album,
 *   sleep timer, details), volume, expand
 *
 * Quiet by design: no accent slab, no blur. The only filled control is the play key.
 */
@Composable
fun AppLargePlayer(
    modifier: Modifier = Modifier,
    onQueue: () -> Unit = {},
    onAlternativeSource: () -> Unit = {},
    onLyrics: () -> Unit = {},
    onExpand: () -> Unit = {},
    panelVisible: Boolean = false,
    onTogglePanel: (() -> Unit)? = null,
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
    val hasTrack = player.ui.currentQueueEntry != null

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    Column(
        modifier = modifier
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        MillenniumSeparator(modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Now playing
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (hasTrack) {
                    NowPlayingStand(
                        coverModel = player.ui.coverModel,
                        contentDescription = player.ui.album,
                        onClick = onExpand
                    )
                    Column(modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)) {
                        Text(
                            text = player.ui.title,
                            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PlayerArtistsText(
                            artists = player.ui.artistList,
                            onArtistClick = { id -> navigationCommands.navigateTo(Routes.Artist(id)) },
                            style = AppTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    PlayerHeartButton(
                        audioPlayerQueue = audioPlayerQueue,
                        savedTracksViewModel = savedTracksViewModel
                    )
                }
            }

            // Transport + seek
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TransportRow(player = player, large = false)
                Row(
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStyle = AppTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                    Text(
                        text = formatPlayerTime(player.ui.position),
                        style = timeStyle,
                        color = colors.onSurfaceVariant
                    )
                    MillenniumSlider(
                        value = player.seekProgress,
                        onValueChange = player.onSeekChange,
                        onValueChangeFinished = player.onSeekFinished,
                        enabled = player.canSeek,
                        label = stringResource(Res.string.player_seek_label),
                        touchTargetHeight = 24.dp,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )
                    Text(
                        text = formatPlayerTime(player.ui.displayDuration),
                        style = timeStyle,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Secondary
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuietGlyphButton(
                    icon = Glyphs.Lyrics,
                    contentDescription = stringResource(Res.string.player_lyrics),
                    enabled = hasTrack,
                    onClick = onLyrics
                )
                QuietGlyphButton(
                    icon = Glyphs.Queue,
                    contentDescription = stringResource(Res.string.queue_title),
                    onClick = onQueue
                )
                if (onTogglePanel != null) {
                    QuietGlyphButton(
                        icon = Glyphs.PanelRight,
                        contentDescription = stringResource(Res.string.player_toggle_now_playing_panel),
                        active = panelVisible,
                        onClick = onTogglePanel
                    )
                }
                PlayerMoreMenu(
                    player = player,
                    downloadsViewModel = downloadsViewModel,
                    playerOptionsViewModel = playerOptionsViewModel,
                    onAlternativeSource = onAlternativeSource,
                    onGoToAlbum = { id -> navigationCommands.navigateTo(Routes.Album(id)) }
                )
                // Volume yields first when the bar is narrow, so Expand never gets squeezed out.
                PlayerVolumeRow(
                    player = player,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 72.dp, max = 150.dp)
                )
                QuietGlyphButton(
                    icon = Glyphs.Expand,
                    contentDescription = stringResource(Res.string.player_expand_content_description),
                    enabled = hasTrack,
                    onClick = onExpand
                )
            }
        }
    }
}

/**
 * The record-shop "now playing" display in miniature: the sleeve leans on a kraft ledge, tilted a
 * few degrees as if stood up by hand. Clicking it opens the full-window player.
 */
@Composable
private fun NowPlayingStand(coverModel: Any?, contentDescription: String?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 60.dp)
            // No clip here: it would cut the sleeve's shadow and tilted corners.
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 2.dp)
                .size(48.dp)
                .graphicsLayer { rotationZ = -3f }
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(colors.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = coverModel,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.kraft)
        )
    }
}
