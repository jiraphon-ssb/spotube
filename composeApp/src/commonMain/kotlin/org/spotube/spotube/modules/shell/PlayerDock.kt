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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.modules.lyrics.LyricsViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_or_pause_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.shell_dock_exit_mini
import spotube.composeapp.generated.resources.shell_dock_lyrics
import spotube.composeapp.generated.resources.shell_dock_mini
import spotube.composeapp.generated.resources.shell_dock_nothing_playing
import spotube.composeapp.generated.resources.shell_dock_open_now_playing
import spotube.composeapp.generated.resources.shell_dock_queue
import spotube.composeapp.generated.resources.shell_dock_queue_count
import spotube.composeapp.generated.resources.shell_dock_seek
import spotube.composeapp.generated.resources.shell_dock_sound
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// Geometry from the approved mockup (v8): an 80dp capsule centred 18dp above the bottom edge, or
// a 360×154 card 20dp in from the bottom-right corner in mini mode.
private val DockMaxWidth = 900.dp
private val DockSideClearance = 96.dp
private val DockHeight = 80.dp
private val DockBottomMargin = 18.dp
private val MiniWidth = 360.dp
private val MiniHeight = 154.dp
private val MiniMargin = 20.dp
private const val DOCK_RADIUS = 30f
private const val MINI_RADIUS = 24f

/** One turn of the cover disc while playing. */
private const val DISC_TURN_MILLIS = 9_000

/** Bars in the waveform seek bar. */
private const val WAVE_BARS = 72

/** Arrow-key seek step on the waveform. */
private const val WAVE_KEY_STEP = 0.02f

/** Space the dock takes above the bottom edge; pages pad their last item by this much. */
internal val PlayerDockClearance = DockBottomMargin + DockHeight + 18.dp

/**
 * The wide layout's floating player: a glass capsule at the bottom centre of the window.
 *
 * - left: spinning cover disc, "Title · Artist" and the live lyric line; opens now playing
 * - centre: shuffle, previous, play/pause, next, like, over a waveform seek bar
 * - right: sound & EQ, queue (with the up-next count), lyrics, mini player
 *
 * With [mini] it morphs (spring, no bounce) into a card in the bottom-right corner: cover, title
 * and lyric on top, previous/play/next in the middle, waveform at the bottom, and a button that
 * returns to the full window.
 *
 * Fill the window with [modifier]: the dock positions itself, and only its own rectangle takes
 * pointer input.
 */
@Composable
fun PlayerDock(
    mini: Boolean,
    onOpenNowPlaying: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleMini: () -> Unit,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    lyricsViewModel: LyricsViewModel = koinViewModel(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val colors = AppTheme.colors
    val player = rememberPlayerController(audioPlayerQueue = audioPlayerQueue)
    val lyrics by lyricsViewModel.uiState.collectAsStateWithLifecycle()
    val hasTrack = player.ui.currentQueueEntry != null

    // The current synced line; before the first timestamp (or without synced lyrics) the line
    // falls back to the album, in the muted colour.
    val lyricLine = if (hasTrack) {
        lyrics.syncedLyrics?.getOrNull(lyrics.currentIndex)?.text?.trim()?.takeIf { it.isNotEmpty() }
    } else {
        null
    }
    val subLine = DockSubLine(
        text = lyricLine ?: player.ui.album.takeIf { hasTrack && it.isNotBlank() }.orEmpty(),
        isLyric = lyricLine != null
    )

    val upcoming = remember(player.ui.queue, player.ui.currentQueueEntry) {
        val current = player.ui.currentQueueEntry
        val index = if (current == null) -1 else player.ui.queue.indexOfFirst { it.url == current.url }
        if (index < 0) 0 else player.ui.queue.size - index - 1
    }
    val waveSeed = when (val entry = player.ui.currentQueueEntry) {
        is QueueEntry.StreamingTrack -> entry.track.id
        is QueueEntry.LocalTrack -> entry.url
        null -> null
    }

    val progress = animateFloatAsState(
        targetValue = if (mini) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "dock_mini"
    )
    val showMiniContent by remember { derivedStateOf { progress.value > 0.5f } }
    val shape = remember(progress) { DockShape(progress) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .dockPlacement(progress)
                .shellGlass(shape, fill = shellGlassFill(alpha = 0.82f), border = colors.outline, elevation = 28.dp)
                .blockPointerPassThrough()
        ) {
            // Cross-fade the two arrangements at the midpoint of the morph.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress.value
                        alpha = if (showMiniContent) ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                        else ((0.5f - p) * 2f).coerceIn(0f, 1f)
                    }
            ) {
                if (showMiniContent) {
                    MiniDockContent(
                        player = player,
                        subLine = subLine,
                        waveSeed = waveSeed,
                        onOpenNowPlaying = onOpenNowPlaying,
                        onExitMini = onToggleMini
                    )
                } else {
                    FullDockContent(
                        player = player,
                        subLine = subLine,
                        waveSeed = waveSeed,
                        upcoming = upcoming,
                        audioPlayerQueue = audioPlayerQueue,
                        savedTracksViewModel = savedTracksViewModel,
                        onOpenNowPlaying = onOpenNowPlaying,
                        onOpenSound = onOpenSound,
                        onOpenQueue = onOpenQueue,
                        onToggleMini = onToggleMini
                    )
                }
            }
        }
    }
}

/** The line under the title: the current lyric (accent) or the album (muted). */
private data class DockSubLine(val text: String, val isLyric: Boolean)

/**
 * Sizes and places the dock between its capsule and mini-card rectangles. Reads [progress] in
 * the layout phase only, so the morph relayouts without recomposing.
 */
private fun Modifier.dockPlacement(progress: State<Float>): Modifier = layout { measurable, constraints ->
    val p = progress.value
    val fullW = constraints.maxWidth
    val fullH = constraints.maxHeight
    val normalW = min(DockMaxWidth.roundToPx(), fullW - DockSideClearance.roundToPx()).coerceAtLeast(0)
    val normalH = DockHeight.roundToPx()
    val miniW = MiniWidth.roundToPx()
    val miniH = MiniHeight.roundToPx()
    val w = lerpInt(normalW, miniW, p).coerceIn(0, fullW)
    val h = lerpInt(normalH, miniH, p).coerceIn(0, fullH)
    val x = lerpInt((fullW - normalW) / 2, fullW - MiniMargin.roundToPx() - miniW, p)
    val y = lerpInt(
        fullH - DockBottomMargin.roundToPx() - normalH,
        fullH - MiniMargin.roundToPx() - miniH,
        p
    )
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(fullW, fullH) { placeable.place(x, y) }
}

private fun lerpInt(start: Int, stop: Int, fraction: Float): Int =
    (start + (stop - start) * fraction).roundToInt()

/** Rounded rectangle whose radius follows the mini morph (30dp capsule → 24dp card). */
private class DockShape(private val progress: State<Float>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radiusDp = DOCK_RADIUS + (MINI_RADIUS - DOCK_RADIUS) * progress.value
        val radius = with(density) { radiusDp.dp.toPx() }
            .coerceAtMost(min(size.width, size.height) / 2f)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, radius, radius))
    }
}

// ---------------------------------------------------------------------------------------------
// Arrangements
// ---------------------------------------------------------------------------------------------

@Composable
private fun FullDockContent(
    player: PlayerController,
    subLine: DockSubLine,
    waveSeed: String?,
    upcoming: Int,
    audioPlayerQueue: AudioPlayerQueue,
    savedTracksViewModel: SavedTracksViewModel,
    onOpenNowPlaying: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleMini: () -> Unit
) {
    val hasTrack = player.ui.currentQueueEntry != null
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NowPlayingBlock(
            player = player,
            subLine = subLine,
            onClick = onOpenNowPlaying,
            modifier = Modifier.width(270.dp)
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleButton(player)
                PreviousButton(player)
                DockPlayKey(player)
                NextButton(player)
                PlayerHeartButton(
                    audioPlayerQueue = audioPlayerQueue,
                    savedTracksViewModel = savedTracksViewModel
                )
            }
            WaveformRow(player = player, seed = waveSeed, modifier = Modifier.widthIn(max = 460.dp))
        }
        Row(
            modifier = Modifier.width(170.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuietGlyphButton(
                icon = Glyphs.Equalizer,
                contentDescription = stringResource(Res.string.shell_dock_sound),
                onClick = onOpenSound,
                iconSize = 18.dp
            )
            Box {
                QuietGlyphButton(
                    icon = Glyphs.Queue,
                    contentDescription = if (upcoming > 0) {
                        stringResource(Res.string.shell_dock_queue_count, upcoming)
                    } else {
                        stringResource(Res.string.shell_dock_queue)
                    },
                    onClick = onOpenQueue,
                    iconSize = 18.dp
                )
                if (upcoming > 0) {
                    CountBadge(upcoming, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
                }
            }
            QuietGlyphButton(
                icon = Glyphs.Lyrics,
                contentDescription = stringResource(Res.string.shell_dock_lyrics),
                onClick = onOpenNowPlaying,
                enabled = hasTrack,
                iconSize = 18.dp
            )
            QuietGlyphButton(
                icon = Glyphs.MiniPlayer,
                contentDescription = stringResource(Res.string.shell_dock_mini),
                onClick = onToggleMini,
                iconSize = 18.dp
            )
        }
    }
}

@Composable
private fun MiniDockContent(
    player: PlayerController,
    subLine: DockSubLine,
    waveSeed: String?,
    onOpenNowPlaying: () -> Unit,
    onExitMini: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        NowPlayingBlock(
            player = player,
            subLine = subLine,
            onClick = onOpenNowPlaying,
            modifier = Modifier.fillMaxWidth().padding(end = 30.dp).align(Alignment.TopStart)
        )
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)) {
            QuietGlyphButton(
                icon = Glyphs.Expand,
                contentDescription = stringResource(Res.string.shell_dock_exit_mini),
                onClick = onExitMini,
                size = 30.dp,
                iconSize = 16.dp
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviousButton(player)
                DockPlayKey(player)
                NextButton(player)
            }
            WaveformRow(player = player, seed = waveSeed, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

/** Cover disc + "Title · Artist" + the lyric line. Clicking it opens now playing. */
@Composable
private fun NowPlayingBlock(
    player: PlayerController,
    subLine: DockSubLine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val hasTrack = player.ui.currentQueueEntry != null
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .keyboardFocusRing(shape)
            .clip(shape)
            .clickable(
                enabled = hasTrack,
                onClickLabel = stringResource(Res.string.shell_dock_open_now_playing),
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverDisc(coverModel = player.ui.coverModel, spinning = player.ui.isPlaying)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            val nothingPlaying = stringResource(Res.string.shell_dock_nothing_playing)
            val title = if (hasTrack) {
                buildAnnotatedString {
                    append(player.ui.title)
                    if (player.ui.artists.isNotBlank()) {
                        withStyle(SpanStyle(color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)) {
                            append(" · ")
                            append(player.ui.artists)
                        }
                    }
                }
            } else {
                buildAnnotatedString { append(nothingPlaying) }
            }
            Text(
                text = title,
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (hasTrack) colors.onSurface else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subLine.text.isNotEmpty()) {
                LyricLine(subLine)
            }
        }
    }
}

/** One line that slides up when it changes (fades only with reduced motion). */
@Composable
private fun LyricLine(subLine: DockSubLine) {
    val colors = AppTheme.colors
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = subLine,
        modifier = Modifier.height(20.dp).clipToBounds(),
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            } else {
                (slideInVertically(tween(500)) { it } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(500)) { -it } + fadeOut(tween(200)))
            }
        },
        label = "dock_lyric_line"
    ) { line ->
        Text(
            text = line.text,
            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (line.isLyric) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 54dp round cover that turns once every 9s while playing and holds its angle when paused. */
@Composable
private fun CoverDisc(coverModel: Any?, spinning: Boolean) {
    val colors = AppTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning, reduceMotion) {
        if (!spinning || reduceMotion) return@LaunchedEffect
        while (true) {
            rotation.animateTo(rotation.value + 360f, tween(DISC_TURN_MILLIS, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    Box(
        modifier = Modifier
            .size(54.dp)
            .graphicsLayer { rotationZ = rotation.value }
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
    ) {
        AsyncImage(
            model = coverModel,
            // Decorative: the title sits right beside it.
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ShuffleButton(player: PlayerController) {
    val ui = player.ui
    QuietGlyphButton(
        icon = Glyphs.Shuffle,
        contentDescription = stringResource(
            if (ui.isShuffling) Res.string.player_disable_shuffle_content_description
            else Res.string.player_enable_shuffle_content_description
        ),
        onClick = player.onShuffle,
        enabled = transportEnabled(player),
        active = ui.isShuffling,
        size = 32.dp,
        iconSize = 17.dp
    )
}

@Composable
private fun PreviousButton(player: PlayerController) {
    QuietGlyphButton(
        icon = Glyphs.SkipPrevious,
        contentDescription = stringResource(Res.string.player_previous_content_description),
        onClick = player.onPrevious,
        enabled = transportEnabled(player),
        tint = AppTheme.colors.onSurface,
        size = 32.dp,
        iconSize = 17.dp
    )
}

@Composable
private fun NextButton(player: PlayerController) {
    QuietGlyphButton(
        icon = Glyphs.SkipNext,
        contentDescription = stringResource(Res.string.player_next_content_description),
        onClick = player.onNext,
        enabled = transportEnabled(player),
        tint = AppTheme.colors.onSurface,
        size = 32.dp,
        iconSize = 17.dp
    )
}

private fun transportEnabled(player: PlayerController): Boolean =
    !player.isJamGuest && player.ui.currentQueueEntry != null

/** 40dp white play/pause key with a canvas-coloured glyph (spinner while a start is pending). */
@Composable
private fun DockPlayKey(player: PlayerController) {
    val colors = AppTheme.colors
    val ui = player.ui
    val ink = colors.background
    MillenniumIconButton(
        onClick = player.onPlayPause,
        // While loading it stays pressable so a second press cancels the start.
        enabled = ui.currentQueueEntry != null || player.isLoading,
        modifier = Modifier.keyboardFocusRing(CircleShape),
        style = MillenniumIconButtonStyle.Quiet,
        contentDescription = if (player.isLoading) {
            stringResource(Res.string.player_pause_content_description)
        } else {
            null
        },
        background = SolidColor(colors.onSurface),
        foreground = ink,
        shape = CircleShape,
        size = DpSize(40.dp, 40.dp)
    ) {
        if (player.isLoading) {
            LoadingRing(color = ink, size = 16.dp)
        } else {
            MillenniumIcon(
                if (ui.isPlaying) Glyphs.Pause else Glyphs.Play,
                contentDescription = stringResource(
                    if (ui.isPlaying) Res.string.player_pause_content_description
                    else Res.string.player_play_or_pause_content_description
                ),
                tint = ink,
                size = 15.dp
            )
        }
    }
}

/** Elapsed time, waveform seek bar, total time. */
@Composable
private fun WaveformRow(player: PlayerController, seed: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val timeStyle = AppTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPlayerTime(player.ui.position),
            style = timeStyle,
            color = colors.onSurfaceVariant,
            maxLines = 1
        )
        WaveformSeekBar(player = player, seed = seed, modifier = Modifier.weight(1f))
        Text(
            text = formatPlayerTime(player.ui.displayDuration),
            style = timeStyle,
            color = colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Seek bar drawn as [WAVE_BARS] thin rounded bars. Heights are made up, but stable per track:
 * a seeded generator (seed = track id) under a sine envelope, so every song gets its own shape.
 * Played bars use the text colour, the rest white at 18%. Click or drag to seek; arrow keys step.
 */
@Composable
private fun WaveformSeekBar(player: PlayerController, seed: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val heights = remember(seed) { waveformHeights(seed) }
    val latest = rememberUpdatedState(player)
    val enabled = player.canSeek
    val progress = player.seekProgress.coerceIn(0f, 1f)
    val label = stringResource(Res.string.shell_dock_seek)
    val playedColor = if (enabled) colors.onSurface else colors.onSurfaceVariant
    val restColor = Color.White.copy(alpha = 0.18f)

    fun scrub(fraction: Float) = latest.value.onSeekChange(fraction.coerceIn(0f, 1f))
    fun commit() = latest.value.onSeekFinished()
    fun seekTo(fraction: Float) {
        scrub(fraction)
        commit()
    }

    Box(
        modifier = modifier
            .height(18.dp)
            .keyboardFocusRing(RoundedCornerShape(4.dp))
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        seekTo(target)
                        true
                    }
                }
            }
            .onKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        seekTo(latest.value.seekProgress - WAVE_KEY_STEP); true
                    }
                    Key.DirectionRight -> {
                        seekTo(latest.value.seekProgress + WAVE_KEY_STEP); true
                    }
                    else -> false
                }
            }
            .focusable(enabled)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    if (size.width > 0) seekTo(offset.x / size.width)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> if (size.width > 0) scrub(offset.x / size.width) },
                    onDragEnd = { commit() },
                    onDragCancel = { commit() }
                ) { change, _ ->
                    change.consume()
                    if (size.width > 0) scrub(change.position.x / size.width)
                }
            }
            .drawBehind {
                val gap = 2.dp.toPx()
                val barWidth = ((size.width - gap * (WAVE_BARS - 1)) / WAVE_BARS).coerceAtLeast(1f)
                val corner = CornerRadius(min(barWidth / 2f, 2.dp.toPx()))
                for (i in 0 until WAVE_BARS) {
                    val barHeight = heights[i].dp.toPx().coerceAtMost(size.height)
                    drawRoundRect(
                        color = if (i.toFloat() / WAVE_BARS < progress) playedColor else restColor,
                        topLeft = Offset(i * (barWidth + gap), (size.height - barHeight) / 2f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = corner
                    )
                }
            }
    )
}

/**
 * Bar heights in dp (4..16) for [seed]: the mockup's LCG (`sd = (sd*9301+49297) % 233280`)
 * seeded from the track id, under a `sin^0.6` envelope so the shape swells in the middle.
 */
private fun waveformHeights(seed: String?): FloatArray {
    var state = ((seed?.hashCode() ?: 7).toLong() and 0x7fffffffL) % 233_280L
    return FloatArray(WAVE_BARS) { i ->
        state = (state * 9_301L + 49_297L) % 233_280L
        val random = state / 233_280f
        val envelope = sin(i.toDouble() / WAVE_BARS * PI).toFloat().coerceAtLeast(0f).pow(0.6f)
        4f + (envelope * 12f * (0.45f + random * 0.55f)).roundToInt()
    }
}
