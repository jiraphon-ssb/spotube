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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_dock_nothing_playing
import spotube.composeapp.generated.resources.shell_dock_open_now_playing
import spotube.composeapp.generated.resources.shell_nav_home
import spotube.composeapp.generated.resources.shell_nav_library
import spotube.composeapp.generated.resources.shell_nav_search
import spotube.composeapp.generated.resources.shell_nav_stats

private val NarrowDockShape = RoundedCornerShape(22.dp)
private val TabShape = RoundedCornerShape(12.dp)
private val CoverShape = RoundedCornerShape(8.dp)

/** Plate behind the selected tab (same white as the capsule's selected pill, a step quieter). */
private val SelectedTabPlate = Color.White.copy(alpha = 0.08f)

/**
 * The narrow window's dock: one glass slab 8dp in from the bottom edges that carries both the
 * player and the navigation (the top capsule is gone at this width).
 *
 * - top edge: [DockProgressLine]
 * - track row: cover, title and artists (one line each; opens now playing), like, play/pause, next
 * - tab row: Home, Search, Library, Stats — the same destinations as the wide capsule, with
 *   Search going to the search page instead of the palette (the palette is the top-right button)
 *
 * Shuffle, previous, the waveform, queue, lyrics and sound live in now playing at this width.
 * Fill the window with [modifier]; only the dock's own rectangle takes pointer input.
 */
@Composable
internal fun NarrowDock(
    currentRoute: NavKey?,
    onNavigate: (Routes) -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val colors = AppTheme.colors
    val player = rememberPlayerController(audioPlayerQueue = audioPlayerQueue)
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(NarrowDockMargin)
                .fillMaxWidth()
                .height(NarrowDockHeight)
                .shellGlass(NarrowDockShape, fill = shellGlassFill(alpha = 0.86f), border = colors.outline, elevation = 28.dp)
                .blockPointerPassThrough()
        ) {
            DockProgressLine(player = player, modifier = Modifier.padding(horizontal = 18.dp))
            NarrowTrackRow(
                player = player,
                onOpenNowPlaying = onOpenNowPlaying,
                audioPlayerQueue = audioPlayerQueue,
                savedTracksViewModel = savedTracksViewModel
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NarrowDockTabRowHeight - 1.dp)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                NarrowTab(Glyphs.Home, stringResource(Res.string.shell_nav_home), currentRoute == Routes.Home) {
                    onNavigate(Routes.Home)
                }
                NarrowTab(Glyphs.Search, stringResource(Res.string.shell_nav_search), currentRoute == Routes.Search) {
                    onNavigate(Routes.Search)
                }
                NarrowTab(Glyphs.Library, stringResource(Res.string.shell_nav_library), currentRoute == Routes.Library) {
                    onNavigate(Routes.Library)
                }
                NarrowTab(Glyphs.Stats, stringResource(Res.string.shell_nav_stats), currentRoute == Routes.Stats) {
                    onNavigate(Routes.Stats)
                }
            }
        }
    }
}

@Composable
private fun NarrowTrackRow(
    player: PlayerController,
    onOpenNowPlaying: () -> Unit,
    audioPlayerQueue: AudioPlayerQueue,
    savedTracksViewModel: SavedTracksViewModel
) {
    val colors = AppTheme.colors
    val hasTrack = player.ui.currentQueueEntry != null
    val blockShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NarrowDockTrackRowHeight)
            .padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .keyboardFocusRing(blockShape)
                .clip(blockShape)
                .clickable(
                    enabled = hasTrack,
                    onClickLabel = stringResource(Res.string.shell_dock_open_now_playing),
                    role = Role.Button,
                    onClick = onOpenNowPlaying
                )
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CoverShape)
                    .background(colors.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = player.ui.coverModel,
                    // Decorative: the title sits right beside it.
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
                Text(
                    text = if (hasTrack) player.ui.title else stringResource(Res.string.shell_dock_nothing_playing),
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
                    color = if (hasTrack) colors.onSurface else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasTrack && player.ui.artists.isNotBlank()) {
                    Text(
                        text = player.ui.artists,
                        style = AppTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        PlayerHeartButton(audioPlayerQueue = audioPlayerQueue, savedTracksViewModel = savedTracksViewModel)
        Box(Modifier.padding(horizontal = 4.dp)) { DockPlayKey(player) }
        NextButton(player)
    }
}

/** One destination: icon over an 11sp label; the selected tab gets a quiet plate. */
@Composable
private fun RowScope.NarrowTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val content by animateColorAsState(
        targetValue = when {
            selected -> colors.onSurface
            hovered -> colors.onSurface
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(150),
        label = "narrow_tab_fg"
    )
    val plate by animateColorAsState(
        targetValue = when {
            selected -> SelectedTabPlate
            hovered -> colors.hover
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "narrow_tab_bg"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .keyboardFocusRing(TabShape)
            .clip(TabShape)
            .background(plate)
            .hoverable(interactionSource)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MillenniumIcon(icon, contentDescription = null, tint = content, size = 18.dp)
        Text(
            text = label,
            style = AppTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 15.sp
            ),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
