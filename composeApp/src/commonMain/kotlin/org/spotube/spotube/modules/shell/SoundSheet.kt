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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.spotube.core.audioplayer.AudioOutputDevice
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.EqPreset
import org.spotube.spotube.core.audioplayer.EqualizerBandsHz
import org.spotube.spotube.core.audioplayer.SleepTimerChoice
import org.spotube.spotube.core.audioplayer.SoundSettings
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.settings.sections.displayLabel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.overlay_crossfade_title
import spotube.composeapp.generated.resources.overlay_eq_band
import spotube.composeapp.generated.resources.overlay_eq_custom
import spotube.composeapp.generated.resources.overlay_eq_preset_acoustic
import spotube.composeapp.generated.resources.overlay_eq_preset_bass
import spotube.composeapp.generated.resources.overlay_eq_preset_flat
import spotube.composeapp.generated.resources.overlay_eq_preset_night
import spotube.composeapp.generated.resources.overlay_eq_preset_vocal
import spotube.composeapp.generated.resources.overlay_eq_presets
import spotube.composeapp.generated.resources.overlay_eq_title
import spotube.composeapp.generated.resources.overlay_eq_unsupported
import spotube.composeapp.generated.resources.overlay_more_title
import spotube.composeapp.generated.resources.overlay_normalize_body
import spotube.composeapp.generated.resources.overlay_normalize_restart
import spotube.composeapp.generated.resources.overlay_normalize_title
import spotube.composeapp.generated.resources.overlay_output_change
import spotube.composeapp.generated.resources.overlay_output_default
import spotube.composeapp.generated.resources.overlay_output_none
import spotube.composeapp.generated.resources.overlay_output_title
import spotube.composeapp.generated.resources.overlay_quality_change
import spotube.composeapp.generated.resources.overlay_quality_change_description
import spotube.composeapp.generated.resources.overlay_quality_title
import spotube.composeapp.generated.resources.overlay_sleep_15
import spotube.composeapp.generated.resources.overlay_sleep_30
import spotube.composeapp.generated.resources.overlay_sleep_60
import spotube.composeapp.generated.resources.overlay_sleep_end_of_track
import spotube.composeapp.generated.resources.overlay_sleep_off
import spotube.composeapp.generated.resources.overlay_sleep_remaining
import spotube.composeapp.generated.resources.overlay_sleep_title
import spotube.composeapp.generated.resources.overlay_sleep_toast_end_of_track
import spotube.composeapp.generated.resources.overlay_sleep_toast_minutes
import spotube.composeapp.generated.resources.overlay_sleep_toast_off
import spotube.composeapp.generated.resources.overlay_soon
import spotube.composeapp.generated.resources.overlay_sound_title
import spotube.composeapp.generated.resources.overlay_sound_volume
import spotube.composeapp.generated.resources.overlay_speed_title
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private val PlaybackSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f)
private val SpeedLabels = listOf("0.75×", "1×", "1.25×", "1.5×")

/** Short band labels under the EQ box (mockup `.eqlabels`). */
private val FrequencyLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/** Mockup `--ease`: cubic-bezier(.22, 1, .36, 1). */
private val SoftEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val EqBoxShape = RoundedCornerShape(16.dp)
private val EqPlotHeight = 140.dp
private val KnobSize = 14.dp

/**
 * The sound & EQ sheet (mockup `#sheet`): volume, a custom 10-band EQ with presets, sleep timer,
 * crossfade (disabled: one VLC player can't overlap two tracks), playback speed, and "more"
 * options (normalization, streaming quality, output device). Controls the platform can't honour
 * are hidden, or explained, rather than shown dead.
 */
@Composable
internal fun SoundSheet(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    audioPlayer: AudioPlayerInterface = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
    val colors = AppTheme.colors
    val player = rememberPlayerController(audioPlayer = audioPlayer)
    val shellActions = LocalShellActions.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { SoundSettings.ensureLoaded() }
    val sound by SoundSettings.state.collectAsState()
    val speed by audioPlayer.playbackSpeedFlow.collectAsState()
    val userSettings by settingsRepository.userSettings.collectAsState()
    val title = stringResource(Res.string.overlay_sound_title)

    val presetLabels = listOf(
        stringResource(Res.string.overlay_eq_preset_flat),
        stringResource(Res.string.overlay_eq_preset_bass),
        stringResource(Res.string.overlay_eq_preset_vocal),
        stringResource(Res.string.overlay_eq_preset_acoustic),
        stringResource(Res.string.overlay_eq_preset_night)
    )
    val sleepLabels = listOf(
        stringResource(Res.string.overlay_sleep_off),
        stringResource(Res.string.overlay_sleep_15),
        stringResource(Res.string.overlay_sleep_30),
        stringResource(Res.string.overlay_sleep_60),
        stringResource(Res.string.overlay_sleep_end_of_track)
    )

    val deadline = sound.sleepTimerDeadline
    val remainingMinutes by produceState<Int?>(initialValue = null, deadline) {
        if (deadline == null) {
            value = null
            return@produceState
        }
        while (true) {
            val left = -deadline.elapsedNow()
            value = ((left.inWholeSeconds + 59) / 60).toInt().coerceAtLeast(1)
            delay(10.seconds)
        }
    }

    RightOverlayPanel(title = title, onClose = onClose, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OverlayHeader(title = title, onClose = onClose)

            OverlaySectionLabel(stringResource(Res.string.overlay_sound_volume))
            PlayerVolumeRow(player = player, modifier = Modifier.fillMaxWidth())

            // Equalizer
            val presetName = sound.preset?.let { presetLabels[it.ordinal] }
                ?: stringResource(Res.string.overlay_eq_custom)
            OverlaySectionLabel(
                text = stringResource(Res.string.overlay_eq_title),
                trailing = if (audioPlayer.supportsEqualizer) presetName else null
            )
            if (audioPlayer.supportsEqualizer) {
                EqualizerEditor(
                    bandsDb = sound.bandsDb,
                    onBandChange = SoundSettings::setBand,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OverlaySegmentedRow(
                    options = presetLabels,
                    selectedIndex = sound.preset?.ordinal ?: -1,
                    onSelect = { index -> SoundSettings.selectPreset(EqPreset.entries[index]) },
                    groupLabel = stringResource(Res.string.overlay_eq_presets)
                )
            } else {
                Text(
                    text = stringResource(Res.string.overlay_eq_unsupported),
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            // Sleep timer
            OverlaySectionLabel(
                text = stringResource(Res.string.overlay_sleep_title),
                trailing = remainingMinutes?.let { stringResource(Res.string.overlay_sleep_remaining, it) }
            )
            OverlaySegmentedRow(
                options = sleepLabels,
                selectedIndex = sound.sleepTimer.ordinal,
                onSelect = { index ->
                    val choice = SleepTimerChoice.entries[index]
                    SoundSettings.setSleepTimer(choice)
                    val label = sleepLabels[index]
                    scope.launch {
                        val message = when (choice) {
                            SleepTimerChoice.Off -> getString(Res.string.overlay_sleep_toast_off)
                            SleepTimerChoice.EndOfTrack -> getString(Res.string.overlay_sleep_toast_end_of_track)
                            else -> getString(Res.string.overlay_sleep_toast_minutes, label)
                        }
                        shellActions.showToast(message, player.ui.coverModel)
                    }
                },
                groupLabel = stringResource(Res.string.overlay_sleep_title)
            )

            // Crossfade: not possible with a single VLC player; shown so it's discoverable.
            val crossfadeTitle = stringResource(Res.string.overlay_crossfade_title)
            OverlaySectionLabel(text = crossfadeTitle, trailing = stringResource(Res.string.overlay_soon))
            MillenniumSlider(
                value = 0f,
                onValueChange = {},
                label = crossfadeTitle,
                enabled = false,
                valueRange = 0f..12f,
                modifier = Modifier.fillMaxWidth()
            )

            // Speed. Jam guests follow the host's clock, so they can't change it.
            OverlaySectionLabel(stringResource(Res.string.overlay_speed_title))
            OverlaySegmentedRow(
                options = SpeedLabels,
                selectedIndex = PlaybackSpeeds.indexOfFirst { abs(it - speed) < 0.01f },
                onSelect = { index -> scope.launch { audioPlayer.setPlaybackSpeed(PlaybackSpeeds[index]) } },
                enabled = !player.isJamGuest,
                groupLabel = stringResource(Res.string.overlay_speed_title)
            )

            // More
            OverlaySectionLabel(stringResource(Res.string.overlay_more_title))
            if (audioPlayer.supportsVolumeNormalization) {
                OverlayToggleRow(
                    title = stringResource(Res.string.overlay_normalize_title),
                    body = if (sound.normalizationNeedsRestart) {
                        stringResource(Res.string.overlay_normalize_restart)
                    } else {
                        stringResource(Res.string.overlay_normalize_body)
                    },
                    checked = sound.normalization,
                    onCheckedChange = SoundSettings::setNormalization,
                    enabled = sound.loaded
                )
                OverlayDivider()
            }
            OverlayOptionRow(
                title = stringResource(Res.string.overlay_quality_title),
                body = userSettings.streamingMusicQuality.displayLabel()
            ) {
                OverlaySelectBox(
                    label = stringResource(Res.string.overlay_quality_change),
                    onClick = onOpenSettings,
                    contentDescription = stringResource(Res.string.overlay_quality_change_description)
                )
            }
            if (audioPlayer.supportsOutputDevices) {
                OverlayDivider()
                OutputDeviceRow(audioPlayer = audioPlayer)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Equalizer
// ---------------------------------------------------------------------------------------------

private fun yFraction(gainDb: Float): Float =
    (SoundSettings.MAX_GAIN_DB - gainDb) / (2f * SoundSettings.MAX_GAIN_DB)

private fun gainLabel(gainDb: Float): String {
    val whole = gainDb.roundToInt()
    return if (whole > 0) "+$whole dB" else "$whole dB"
}

/**
 * Mockup `.eqbox`: ten vertical tracks with a dashed 0 dB line, a smooth accent curve through
 * the band gains (cubic Bézier between band centres) with a fading fill under it, and a round
 * knob per band. Drag a knob vertically (snaps to 1 dB), or focus it and use the arrow keys.
 */
@Composable
private fun EqualizerEditor(
    bandsDb: List<Float>,
    onBandChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val reduceMotion = LocalReduceMotion.current
    var draggingBand by remember { mutableIntStateOf(-1) }

    val animated = ArrayList<State<Float>>(bandsDb.size)
    for (index in bandsDb.indices) {
        animated += animateFloatAsState(
            targetValue = bandsDb[index],
            animationSpec = if (reduceMotion || draggingBand == index) snap() else tween(350, easing = SoftEase),
            label = "eq_band"
        )
    }
    val accent = colors.primary

    Column(
        modifier = modifier
            .clip(EqBoxShape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, OverlayLine, EqBoxShape)
            .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(EqPlotHeight)) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val count = animated.size
                if (count == 0) return@Canvas
                val columnWidth = size.width / count
                val h = size.height

                // Tracks
                for (k in 0 until count) {
                    val x = (k + 0.5f) * columnWidth
                    drawLine(
                        color = Color.White.copy(alpha = 0.09f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                // 0 dB
                val inset = 4.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(inset, h / 2f),
                    end = Offset(size.width - inset, h / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                )

                // Curve + fill
                val points = animated.mapIndexed { k, state ->
                    Offset((k + 0.5f) * columnWidth, yFraction(state.value) * h)
                }
                val curve = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (k in 1 until points.size) {
                        val p0 = points[k - 1]
                        val p1 = points[k]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                val fill = Path().apply {
                    addPath(curve)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0f)),
                        startY = 0f,
                        endY = h
                    )
                )
                drawPath(
                    path = curve,
                    color = accent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            Row(modifier = Modifier.matchParentSize()) {
                for (index in bandsDb.indices) {
                    EqBandColumn(
                        index = index,
                        gainDb = bandsDb[index],
                        animatedGain = animated[index],
                        dragging = draggingBand == index,
                        onDragChange = { active ->
                            draggingBand = if (active) index else if (draggingBand == index) -1 else draggingBand
                        },
                        onGainChange = { gain -> onBandChange(index, gain) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            FrequencyLabels.forEach { label ->
                Text(
                    text = label,
                    style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EqBandColumn(
    index: Int,
    gainDb: Float,
    animatedGain: State<Float>,
    dragging: Boolean,
    onDragChange: (Boolean) -> Unit,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val active = hovered || dragging || focused
    val latestGain by rememberUpdatedState(gainDb)
    val latestOnGainChange by rememberUpdatedState(onGainChange)
    val latestOnDragChange by rememberUpdatedState(onDragChange)
    var heightPx by remember { mutableIntStateOf(0) }
    val knobScale by animateFloatAsState(if (active) 1.25f else 1f, tween(150), label = "eq_knob_scale")
    val bandName = stringResource(
        Res.string.overlay_eq_band,
        EqualizerBandsHz.getOrNull(index)?.toString() ?: FrequencyLabels.getOrElse(index) { "" }
    )
    val max = SoundSettings.MAX_GAIN_DB
    val columnShape = RoundedCornerShape(8.dp)

    fun step(delta: Float) {
        latestOnGainChange((latestGain + delta).coerceIn(-max, max))
    }

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height }
            .keyboardFocusRing(columnShape)
            .hoverable(interaction)
            .focusable(interactionSource = interaction)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.DirectionRight -> { step(1f); true }
                    Key.DirectionDown, Key.DirectionLeft -> { step(-1f); true }
                    Key.PageUp -> { step(3f); true }
                    Key.PageDown -> { step(-3f); true }
                    Key.MoveHome -> { latestOnGainChange(0f); true }
                    else -> false
                }
            }
            .semantics {
                contentDescription = bandName
                stateDescription = gainLabel(gainDb)
                progressBarRangeInfo = ProgressBarRangeInfo(gainDb, -max..max, steps = (2 * max).toInt() - 1)
                setProgress { target ->
                    latestOnGainChange(round(target).coerceIn(-max, max))
                    true
                }
            }
            .pointerInput(index) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    fun update(y: Float) {
                        val h = size.height.toFloat()
                        if (h <= 0f) return
                        val gain = round(max - (y / h) * 2f * max).coerceIn(-max, max)
                        latestOnGainChange(gain)
                    }
                    latestOnDragChange(true)
                    try {
                        update(down.position.y)
                        drag(down.id) { change ->
                            update(change.position.y)
                            change.consume()
                        }
                    } finally {
                        latestOnDragChange(false)
                    }
                }
            }
    ) {
        val knobOffset = Modifier.offset {
            val center = yFraction(animatedGain.value) * heightPx
            IntOffset(0, (center - KnobSize.toPx() / 2f).roundToInt())
        }
        if (active) {
            Text(
                text = gainLabel(gainDb),
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = colors.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .wrapContentWidth(unbounded = true)
                    .offset {
                        val center = yFraction(animatedGain.value) * heightPx
                        IntOffset(0, (center - 22.dp.toPx()).roundToInt())
                    }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(knobOffset)
                .size(KnobSize)
                .graphicsLayer {
                    scaleX = knobScale
                    scaleY = knobScale
                }
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = size.minDimension / 2f + 4.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(colors.onSurface)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Output device
// ---------------------------------------------------------------------------------------------

@Composable
private fun OutputDeviceRow(audioPlayer: AudioPlayerInterface) {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<AudioOutputDevice>>(emptyList()) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Re-read when the menu opens: devices come and go (headphones, Bluetooth).
    LaunchedEffect(expanded) {
        val (list, current) = withContext(Dispatchers.Default) {
            audioPlayer.outputDevices() to audioPlayer.currentOutputDevice()
        }
        devices = list
        currentId = current
    }

    val currentName = devices.firstOrNull { it.id == currentId }?.name
        ?: stringResource(Res.string.overlay_output_default)
    OverlayOptionRow(title = stringResource(Res.string.overlay_output_title), body = currentName) {
        MillenniumDropdownMenu(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            anchor = {
                OverlaySelectBox(
                    label = stringResource(Res.string.overlay_quality_change),
                    onClick = { expanded = !expanded },
                    contentDescription = stringResource(Res.string.overlay_output_change),
                    showChevron = true
                )
            }
        ) {
            if (devices.isEmpty()) {
                MillenniumDropdownMenuItem(
                    label = stringResource(Res.string.overlay_output_none),
                    onClick = {},
                    enabled = false
                )
            }
            devices.forEach { device ->
                MillenniumDropdownMenuItem(
                    label = device.name,
                    selected = device.id == currentId,
                    onClick = {
                        expanded = false
                        currentId = device.id
                        scope.launch { audioPlayer.setOutputDevice(device.id) }
                    }
                )
            }
        }
    }
}
