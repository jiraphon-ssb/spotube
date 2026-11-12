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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.cards.glassPlate
import org.spotube.spotube.core.ui.component.cards.pageGlassFill
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.core.ui.theming.coolGlow
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.lyrics.LyricsScreen
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.pages_now_skip_hint
import spotube.composeapp.generated.resources.pages_now_soon
import spotube.composeapp.generated.resources.pages_now_sound
import spotube.composeapp.generated.resources.pages_now_translation
import spotube.composeapp.generated.resources.player_collapse_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.queue_title

/**
 * Desktop "now playing" that takes over the window. Left: the current cover with the next three
 * queue covers fanned behind it in 3D (clicking the stack skips to the next one, the cards spring
 * forward). Right: title and artists, a tools row (translation, not available yet; Sound & EQ;
 * Queue) and the synced lyrics with a left-to-right fill across the sung line. A glass transport
 * bar sits at the bottom (the shell hides its dock here). Close with the collapse button or Escape.
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
    val shellActions = LocalShellActions.current
    val colors = AppTheme.colors
    val focusRequester = remember { FocusRequester() }
    // macOS draws its window buttons over the top-left corner of the content.
    val topInset = remember { if (getPlatform().type == PlatformType.MacOS) 28.dp else 0.dp }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    // Nothing left to show: fall back to the normal shell.
    LaunchedEffect(player.ui.currentQueueEntry == null) {
        if (player.ui.currentQueueEntry == null) onCollapse()
    }

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    // Backdrop lit by the artwork; it covers the page underneath, so it stays opaque.
    val tint by rememberArtworkTint(player.ui.coverModel, fallback = colors.surfaceContainerHigh)
    fun glow() = colors.coolGlow(tint, 0.35f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow().copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.42f),
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
                // The veil of the mockup: lighter at the top, darker toward the transport.
                drawRect(
                    Brush.verticalGradient(
                        0f to colors.background.copy(alpha = 0.08f),
                        1f to colors.background.copy(alpha = 0.5f)
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
                .padding(start = 56.dp, end = 32.dp, top = 16.dp + topInset, bottom = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                QuietGlyphButton(
                    icon = Glyphs.Collapse,
                    contentDescription = stringResource(Res.string.player_collapse_content_description),
                    onClick = onCollapse
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val leftWidth = (maxWidth * 0.42f).coerceIn(320.dp, 480.dp)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QueueStack(
                        player = player,
                        modifier = Modifier.width(leftWidth).fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 30.dp, top = 24.dp)
                    ) {
                        PlayerTitleBlock(
                            player = player,
                            titleStyle = AppTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            audioPlayerQueue = audioPlayerQueue,
                            savedTracksViewModel = savedTracksViewModel,
                            onArtistClick = { id ->
                                onCollapse()
                                navigationCommands.navigateTo(Routes.Artist(id))
                            }
                        )
                        Row(
                            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // No translation source exists yet: shown, but disabled.
                            ToolPill(
                                icon = TranslateGlyph,
                                label = stringResource(Res.string.pages_now_translation),
                                trailing = stringResource(Res.string.pages_now_soon),
                                enabled = false,
                                onClick = {}
                            )
                            ToolPill(
                                icon = Glyphs.Equalizer,
                                label = stringResource(Res.string.pages_now_sound),
                                onClick = shellActions::openSound
                            )
                            ToolPill(
                                icon = Glyphs.Queue,
                                label = stringResource(Res.string.queue_title),
                                onClick = shellActions::openQueue
                            )
                        }
                        LyricsScreen(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            embedded = true
                        )
                    }
                }
            }

            // The shell hides its dock while this view is up, so the transport lives here.
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .glassPlate(RoundedCornerShape(30.dp), fill = pageGlassFill(0.55f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        PlayerSecondaryActions(
                            player = player,
                            downloadsViewModel = downloadsViewModel,
                            playerOptionsViewModel = playerOptionsViewModel,
                            onLyrics = {},
                            onAlternativeSource = onAlternativeSource,
                            onGoToAlbum = { id ->
                                onCollapse()
                                navigationCommands.navigateTo(Routes.Album(id))
                            },
                            showLyrics = false
                        )
                    }
                    TransportRow(player = player, large = false, modifier = Modifier.padding(horizontal = 24.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        PlayerVolumeRow(player = player, modifier = Modifier.width(180.dp))
                    }
                }
                PlayerSeekBar(player = player)
            }
        }
    }
}

/** Depth steps of the fanned stack (index = depth 0..3, then the card leaving to the left). */
private val StackTranslateX = floatArrayOf(0f, 64f, 112f, 150f)
private val StackScale = floatArrayOf(1f, 0.909f, 0.833f, 0.769f) // perspective 1400, z 0/-140/-280/-420
private val StackRotation = floatArrayOf(0f, -24f, -30f, -34f)
private val StackBrightness = floatArrayOf(1f, 0.55f, 0.4f, 0.28f)
private val StackAlpha = floatArrayOf(1f, 1f, 0.85f, 0.6f)
private const val LeaveTranslateX = -160f
private const val LeaveScale = 1.06f // z +80
private const val LeaveRotation = 18f

/** Linear step between two depths; depth < 0 blends depth 0 into the leaving pose. */
private fun stackValue(depth: Float, steps: FloatArray, leave: Float): Float {
    if (depth <= 0f) return steps[0] + (leave - steps[0]) * (-depth).coerceAtMost(1f)
    val lo = depth.toInt().coerceAtMost(steps.lastIndex)
    val hi = (lo + 1).coerceAtMost(steps.lastIndex)
    val t = (depth - lo).coerceIn(0f, 1f)
    return steps[lo] + (steps[hi] - steps[lo]) * t
}

/**
 * The current cover with up to three queued covers fanned behind it (rotationY / translation /
 * scale for depth, darker the further back). Clicking skips to the next track: the front card
 * leaves to the left and the rest spring one step forward.
 */
@Composable
private fun QueueStack(player: PlayerController, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val current = player.ui.currentQueueEntry
    val upNext = player.ui.upNext(3).map { it.entry }
    val cards = listOfNotNull(current) + upNext
    val canSkip = upNext.isNotEmpty() && !player.isJamGuest
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val skipLabel = stringResource(Res.string.player_next_content_description)
    val skipHint = stringResource(Res.string.pages_now_skip_hint)

    // The previous front card plays its exit once, then is dropped.
    var leaving by remember { mutableStateOf<QueueEntry?>(null) }
    var lastFront by remember { mutableStateOf(current) }
    LaunchedEffect(current?.url) {
        val previous = lastFront
        lastFront = current
        if (previous != null && previous.url != current?.url && upNext.none { it.url == previous.url }) {
            leaving = previous
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        val side: Dp = minOf(410.dp, maxHeight * 0.92f, maxWidth - 160.dp).coerceAtLeast(200.dp)
        val shape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(side)
                .keyboardFocusRing(shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = canSkip,
                    role = Role.Button,
                    onClickLabel = skipLabel,
                    onClick = player.onNext
                )
                .semantics { if (canSkip) stateDescription = skipHint }
        ) {
            // Farthest first, so nearer cards draw on top.
            for (index in cards.indices.reversed()) {
                val entry = cards[index]
                key(entry.url) {
                    StackCard(
                        model = entry.artworkModel,
                        targetDepth = index.toFloat(),
                        lift = if (index == 0 && hovered && canSkip) 1f else 0f,
                        reduceMotion = reduceMotion
                    )
                }
            }
            leaving?.let { gone ->
                key("leaving:" + gone.url) {
                    StackCard(
                        model = gone.artworkModel,
                        targetDepth = -1f,
                        initialDepth = 0f,
                        lift = 0f,
                        reduceMotion = reduceMotion,
                        onSettled = { if (leaving?.url == gone.url) leaving = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun StackCard(
    model: Any?,
    targetDepth: Float,
    lift: Float,
    reduceMotion: Boolean,
    initialDepth: Float = targetDepth + 1f,
    onSettled: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val depth = remember { Animatable(if (reduceMotion) targetDepth else initialDepth.coerceAtMost(3f)) }
    LaunchedEffect(targetDepth, reduceMotion) {
        if (reduceMotion) depth.snapTo(targetDepth)
        else depth.animateTo(targetDepth, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
        onSettled()
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val d = depth.value
                translationX = stackValue(d, StackTranslateX, LeaveTranslateX).dp.toPx() - lift * 4.dp.toPx()
                val s = stackValue(d, StackScale, LeaveScale) * (1f + lift * 0.01f)
                scaleX = s
                scaleY = s
                rotationY = stackValue(d, StackRotation, LeaveRotation)
                alpha = if (d < 0f) (1f + d).coerceIn(0f, 1f) else stackValue(d, StackAlpha, 0f)
                cameraDistance = 12f * density
                shadowElevation = (if (d <= 0.5f) 40.dp else 20.dp).toPx()
                this.shape = shape
                clip = true
            }
            .background(colors.surfaceContainerHighest)
            .drawWithContent {
                drawContent()
                val d = depth.value
                val brightness = if (d <= 0f) 1f else stackValue(d, StackBrightness, 1f)
                if (brightness < 1f) drawRect(Color.Black.copy(alpha = 1f - brightness))
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/** A 34dp glass pill for the tools row; [trailing] is a muted note (e.g. "soon"). */
@Composable
private fun ToolPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: String? = null,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(50)
    val ink = if (enabled) colors.onSurface else colors.onSurfaceVariant
    Row(
        modifier = Modifier
            .height(34.dp)
            .keyboardFocusRing(shape)
            .glassPlate(shape)
            .background(if (hovered && enabled) colors.hover else Color.Transparent)
            .hoverable(interaction, enabled = enabled)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillenniumIcon(imageVector = icon, contentDescription = null, tint = ink, size = 16.dp)
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            maxLines = 1
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = AppTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

/** Globe glyph for "translation" (the shared glyph set has none; Glyphs.kt is not edited here). */
private val TranslateGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "Translate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(
            "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z",
            "M3 12h18",
            "M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9s1.3-6.4 3.8-9z"
        ).forEach { data ->
            addPath(
                pathData = androidx.compose.ui.graphics.vector.addPathNodes(data),
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        }
    }.build()
}
