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

package org.spotube.spotube.modules.lyrics


import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.ui.component.cards.isWideShell
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import org.spotube.millennium.composables.MillenniumBarsLoader
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.ApplicationBackButton
import org.spotube.spotube.resources.glyphs.Glyphs
import org.koin.compose.koinInject
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error
import spotube.composeapp.generated.resources.lyrics_mode_plain
import spotube.composeapp.generated.resources.lyrics_mode_synced
import spotube.composeapp.generated.resources.lyrics_no_plain
import spotube.composeapp.generated.resources.lyrics_no_synced
import spotube.composeapp.generated.resources.common_close
import spotube.composeapp.generated.resources.common_retry
import spotube.composeapp.generated.resources.lyrics_action_browse_plugins
import spotube.composeapp.generated.resources.lyrics_action_show_plain
import spotube.composeapp.generated.resources.lyrics_error_title
import spotube.composeapp.generated.resources.lyrics_local_body
import spotube.composeapp.generated.resources.lyrics_local_title
import spotube.composeapp.generated.resources.lyrics_no_plugin_body
import spotube.composeapp.generated.resources.lyrics_no_plugin_title
import spotube.composeapp.generated.resources.lyrics_nothing_playing_body
import spotube.composeapp.generated.resources.lyrics_nothing_playing_title

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    /** Hosted inside another surface (the full-window player): no back/close button. */
    embedded: Boolean = false) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MillenniumTheme.colors

    // Wide glass shell: transparent (the room light shows through) and clear of the floating
    // chrome. Embedded in the full player it is transparent too.
    val wide = isWideShell()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Flat: the accent wash is gone with the rest of the app's gradients.
            .background(if (embedded || wide) Color.Transparent else colors.background)
            .padding(top = if (embedded) 0.dp else LocalAppShellTopInset.current)
    ) {
        val isWide = maxWidth >= 840.dp

        // In the full player the mode switch only matters when both kinds of lyrics exist.
        val showModeSwitch = !embedded ||
            (!uiState.syncedLyrics.isNullOrEmpty() && !uiState.plainLyrics.isNullOrBlank())
        Column(Modifier.fillMaxSize()) {
            if (showModeSwitch) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
                    if (embedded) {
                        // Nothing to go back to from inside the player.
                    } else if (onClose != null) {
                        MillenniumIconButton(
                            onClick = onClose,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxArrowDown4,
                                contentDescription = stringResource(Res.string.common_close)
                            )
                        }
                    } else if (!wide) {
                        // The wide shell has its own floating back button.
                        ApplicationBackButton()
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MillenniumSegmentedButtonRow(Modifier.widthIn(max = 280.dp)) {
                        MillenniumSegmentedButton(
                            selected = uiState.mode == LyricType.SYNCED,
                            onClick = { viewModel.setMode(LyricType.SYNCED) },
                            shape = MillenniumSegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(Res.string.lyrics_mode_synced)) }
                        MillenniumSegmentedButton(
                            selected = uiState.mode == LyricType.STATIC,
                            onClick = { viewModel.setMode(LyricType.STATIC) },
                            shape = MillenniumSegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(Res.string.lyrics_mode_plain)) }
                    }
                }
                Spacer(Modifier.width(48.dp))
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Embedded in the full player, which already shows the cover and title.
                    if (!embedded) TrackPane(uiState, Modifier.weight(0.38f))
                    Column(Modifier.weight(if (embedded) 1f else 0.62f).fillMaxHeight()) {
                        LyricsBody(uiState, viewModel, embedded)
                    }
                }
            } else {
                if (!embedded) {
                    CompactTrackHeader(
                        uiState,
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    LyricsBody(uiState, viewModel, embedded)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LyricsBody(uiState: LyricsUiState, viewModel: LyricsViewModel, embedded: Boolean) {
    val retry = { viewModel.loadLyrics(forceReload = true) }
    when {
        uiState.unavailable == LyricsUnavailableReason.NothingPlaying -> LyricsStateMessage(
            icon = Glyphs.Play,
            title = stringResource(Res.string.lyrics_nothing_playing_title),
            body = stringResource(Res.string.lyrics_nothing_playing_body)
        )

        uiState.unavailable == LyricsUnavailableReason.LocalFile -> LyricsStateMessage(
            icon = Glyphs.LocalFiles,
            title = stringResource(Res.string.lyrics_local_title),
            body = stringResource(Res.string.lyrics_local_body)
        )

        uiState.unavailable == LyricsUnavailableReason.NoPlugin -> {
            val navigationCommands: NavigationCommands = koinInject()
            LyricsStateMessage(
                icon = Glyphs.Lyrics,
                title = stringResource(Res.string.lyrics_no_plugin_title),
                body = stringResource(Res.string.lyrics_no_plugin_body),
                // From inside the full player the Plugins page would open underneath it.
                actionLabel = if (embedded) null else stringResource(Res.string.lyrics_action_browse_plugins),
                onAction = { navigationCommands.navigateTo(Routes.Plugins) }
            )
        }

        uiState.isLoading -> CenteredMessage {
            MillenniumBarsLoader(size = 48.dp)
        }

        uiState.error != null -> LyricsStateMessage(
            icon = Glyphs.Lyrics,
            title = stringResource(Res.string.lyrics_error_title),
            body = uiState.error?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.common_unknown_error),
            actionLabel = stringResource(Res.string.common_retry),
            onAction = retry
        )

        uiState.mode == LyricType.SYNCED -> SyncedLyricsContent(
            syncedLyrics = uiState.syncedLyrics ?: emptyList(),
            currentIndex = uiState.currentIndex,
            onLineClick = viewModel::seekToLine,
            hasPlain = !uiState.plainLyrics.isNullOrBlank(),
            onShowPlain = { viewModel.setMode(LyricType.STATIC) },
            onRetry = retry
        )

        uiState.mode == LyricType.STATIC -> PlainLyricsContent(
            plainLyrics = uiState.plainLyrics ?: "",
            onRetry = retry
        )
    }
}

/** Icon, short title, muted body and an optional single action, centred in the lyrics area. */
@Composable
private fun ColumnScope.LyricsStateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}) {
    CenteredMessage {
        Column(
            modifier = Modifier.widthIn(max = 360.dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MillenniumIcon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(8.dp))
                MillenniumButton(onClick = onAction, style = MillenniumButtonStyle.Secondary) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun CoverArt(url: String, size: Dp, radius: Dp, elevation: Dp, modifier: Modifier = Modifier) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .millenniumSurface(
                background = millenniumPanelBrush(colors),
                shape = RoundedCornerShape(radius),
                borderColor = colors.border,
                borderWidth = 1.dp,
                shadowElevation = elevation
            )
    ) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun TrackPane(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoverArt(
            url = uiState.coverUrl,
            size = 280.dp,
            radius = 24.dp,
            elevation = 0.dp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = uiState.trackTitle,
            style = AppTheme.typography.headlineSmall,
            color = MillenniumTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiState.trackArtists,
            style = AppTheme.typography.bodyLarge,
            color = MillenniumTheme.colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTrackHeader(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = uiState.coverUrl, size = 48.dp, radius = 12.dp, elevation = 0.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = uiState.trackTitle,
                style = AppTheme.typography.titleSmall,
                color = MillenniumTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.trackArtists,
                style = AppTheme.typography.bodySmall,
                color = MillenniumTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColumnScope.CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ColumnScope.SyncedLyricsContent(
    syncedLyrics: List<LyricsLine>,
    currentIndex: Int,
    onLineClick: (LyricsLine) -> Unit,
    hasPlain: Boolean,
    onShowPlain: () -> Unit,
    onRetry: () -> Unit) {
    if (syncedLyrics.isEmpty()) {
        EmptyLyricsMessage(
            text = stringResource(Res.string.lyrics_no_synced),
            actionLabel = stringResource(
                if (hasPlain) Res.string.lyrics_action_show_plain else Res.string.common_retry),
            onAction = if (hasPlain) onShowPlain else onRetry
        )
        return
    }

    val position = rememberSmoothPositionMillis()
    val positionMillis = remember(position) { { position.longValue } }
    SyncedLyricsList(
        lines = syncedLyrics,
        currentIndex = currentIndex,
        onLineClick = onLineClick,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        positionMillis = positionMillis
    )
}

@Composable
private fun ColumnScope.PlainLyricsContent(
    plainLyrics: String,
    onRetry: () -> Unit) {
    if (plainLyrics.isBlank()) {
        EmptyLyricsMessage(
            text = stringResource(Res.string.lyrics_no_plain),
            actionLabel = stringResource(Res.string.common_retry),
            onAction = onRetry
        )
        return
    }

    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
    ) {
        item {
            Text(
                text = plainLyrics,
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.typography.titleMedium.copy(lineHeight = 34.sp),
                color = MillenniumTheme.colors.text,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ColumnScope.EmptyLyricsMessage(text: String, actionLabel: String, onAction: () -> Unit) {
    CenteredMessage {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            MillenniumButton(onClick = onAction, style = MillenniumButtonStyle.Quiet) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Playback position for the karaoke fill, advanced every frame between the player's position
 * updates while playing. Only the draw phase reads it, so nothing recomposes per frame.
 */
@Composable
private fun rememberSmoothPositionMillis(
    audioPlayer: AudioPlayerInterface = koinInject()
): MutableLongState {
    val position = remember(audioPlayer) {
        mutableLongStateOf(audioPlayer.positionFlow.value.inWholeMilliseconds)
    }
    LaunchedEffect(audioPlayer) {
        combine(audioPlayer.positionFlow, audioPlayer.playerStateFlow) { at, state -> at to state }
            .collectLatest { (at, state) ->
                val base = at.inWholeMilliseconds
                position.longValue = base
                if (state == PlayerState.PLAYING) {
                    val start = withFrameMillis { it }
                    while (true) {
                        withFrameMillis { now -> position.longValue = base + (now - start) }
                    }
                }
            }
    }
    return position
}
