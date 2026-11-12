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

package org.spotube.spotube.modules.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.history.ArtistCount
import org.spotube.spotube.core.history.DayMinutes
import org.spotube.spotube.core.history.GenreShare
import org.spotube.spotube.core.history.ListeningStats
import org.spotube.spotube.core.history.StatsRange
import org.spotube.spotube.core.ui.component.PillChip
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.settings.components.GlassGroupShape
import org.spotube.spotube.modules.settings.components.glassSurface
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_stats_title
import spotube.composeapp.generated.resources.stats_artists_empty
import spotube.composeapp.generated.resources.stats_bar_tooltip
import spotube.composeapp.generated.resources.stats_chart_description
import spotube.composeapp.generated.resources.stats_empty_body
import spotube.composeapp.generated.resources.stats_empty_title
import spotube.composeapp.generated.resources.stats_kpi_best
import spotube.composeapp.generated.resources.stats_kpi_days
import spotube.composeapp.generated.resources.stats_kpi_new_tracks
import spotube.composeapp.generated.resources.stats_kpi_no_previous
import spotube.composeapp.generated.resources.stats_kpi_plays
import spotube.composeapp.generated.resources.stats_kpi_streak
import spotube.composeapp.generated.resources.stats_kpi_time
import spotube.composeapp.generated.resources.stats_kpi_time_hours
import spotube.composeapp.generated.resources.stats_kpi_time_minutes
import spotube.composeapp.generated.resources.stats_kpi_top_artist
import spotube.composeapp.generated.resources.stats_kpi_tracks
import spotube.composeapp.generated.resources.stats_kpi_vs_month
import spotube.composeapp.generated.resources.stats_kpi_vs_week
import spotube.composeapp.generated.resources.stats_panel_artists
import spotube.composeapp.generated.resources.stats_panel_genres
import spotube.composeapp.generated.resources.stats_panel_minutes
import spotube.composeapp.generated.resources.stats_range_month
import spotube.composeapp.generated.resources.stats_range_week
import spotube.composeapp.generated.resources.stats_weekday_fri
import spotube.composeapp.generated.resources.stats_weekday_mon
import spotube.composeapp.generated.resources.stats_weekday_sat
import spotube.composeapp.generated.resources.stats_weekday_sun
import spotube.composeapp.generated.resources.stats_weekday_thu
import spotube.composeapp.generated.resources.stats_weekday_tue
import spotube.composeapp.generated.resources.stats_weekday_wed
import kotlin.math.abs
import kotlin.math.ceil

/** Mockup `.kpi .up`: the success green. */
private val UpGreen = Color(0xFF8FE3B5)

private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/**
 * Listening stats (mockup `#v-stats`): range chips, four KPI tiles, minutes-per-day bars, top
 * artists and genres. Transparent so the shell's room light shows through.
 */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = koinViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val range by viewModel.selectedRange.collectAsStateWithLifecycle()
    val topInset = LocalAppShellTopInset.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxWidth()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = topInset + if (topInset > 0.dp) 6.dp else 16.dp,
                    bottom = LocalAppShellBottomInset.current + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatsHeader(range = range, onRange = viewModel::selectRange)
            val current = stats
            when {
                current == null -> Unit
                !current.hasHistory -> StatsEmptyState()
                else -> StatsBody(current, today = viewModel.today())
            }
        }
    }
}

@Composable
private fun StatsHeader(range: StatsRange, onRange: (StatsRange) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.shell_stats_title),
            style = AppTheme.typography.headlineLarge.copy(letterSpacing = (-0.8).sp),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsRange.entries.forEach { entry ->
                PillChip(
                    label = stringResource(
                        when (entry) {
                            StatsRange.Week -> Res.string.stats_range_week
                            StatsRange.Month -> Res.string.stats_range_month
                        }
                    ),
                    selected = entry == range,
                    onClick = { onRange(entry) }
                )
            }
        }
    }
}

@Composable
private fun StatsBody(stats: ListeningStats, today: LocalDate) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 900.dp
        val kpiColumns = if (maxWidth >= 720.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KpiGrid(stats, columns = kpiColumns)
            val chart: @Composable (Modifier) -> Unit = { m ->
                StatsPanel(stringResource(Res.string.stats_panel_minutes), m) {
                    MinutesChart(days = stats.days, range = stats.range, today = today)
                }
            }
            val artists: @Composable (Modifier) -> Unit = { m ->
                StatsPanel(stringResource(Res.string.stats_panel_artists), m) {
                    TopArtistsList(stats.topArtists)
                }
            }
            val genrePanel: @Composable (Modifier) -> Unit = { m ->
                StatsPanel(stringResource(Res.string.stats_panel_genres), m) {
                    GenreList(stats.genres)
                }
            }
            // Most metadata plugins send no genres: then the panel is left out entirely.
            val genres = if (stats.genres.isEmpty()) null else genrePanel
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    chart(Modifier.weight(1.4f).fillMaxHeight())
                    artists(Modifier.weight(1f).fillMaxHeight())
                    genres?.invoke(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                chart(Modifier.fillMaxWidth())
                artists(Modifier.fillMaxWidth())
                genres?.invoke(Modifier.fillMaxWidth())
            }
        }
    }
}

// ---- KPIs --------------------------------------------------------------------------------------

@Composable
private fun KpiGrid(stats: ListeningStats, columns: Int) {
    val hours = stats.totalMinutes / 60
    val minutes = stats.totalMinutes % 60
    val time = if (hours > 0) {
        stringResource(Res.string.stats_kpi_time_hours, hours.toString(), minutes.toString())
    } else {
        stringResource(Res.string.stats_kpi_time_minutes, minutes.toString())
    }
    val comparedTo = stringResource(
        when (stats.range) {
            StatsRange.Week -> Res.string.stats_kpi_vs_week
            StatsRange.Month -> Res.string.stats_kpi_vs_month
        }
    )
    val noPrevious = stringResource(Res.string.stats_kpi_no_previous)
    val topArtist = stats.topArtists.firstOrNull()

    val tiles = listOf<@Composable (Modifier) -> Unit>(
        { m ->
            KpiTile(stringResource(Res.string.stats_kpi_time), time, m) {
                val delta = stats.deltaPercent
                if (delta == null) {
                    KpiNote(noPrevious)
                } else {
                    val up = delta >= 0
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = (if (up) "▲ " else "▼ ") + "${abs(delta)}%",
                            style = AppTheme.typography.labelMedium.merge(TabularNumbers),
                            fontWeight = FontWeight.SemiBold,
                            color = if (up) UpGreen else AppTheme.colors.error
                        )
                        KpiNote(comparedTo)
                    }
                }
            }
        },
        { m ->
            KpiTile(stringResource(Res.string.stats_kpi_tracks), stats.tracksPlayed.toString(), m) {
                KpiNote(stringResource(Res.string.stats_kpi_new_tracks, stats.newTracks.toString()))
            }
        },
        { m ->
            KpiTile(stringResource(Res.string.stats_kpi_top_artist), topArtist?.name ?: "—", m) {
                if (topArtist != null) {
                    KpiNote(stringResource(Res.string.stats_kpi_plays, topArtist.plays.toString()))
                }
            }
        },
        { m ->
            KpiTile(
                stringResource(Res.string.stats_kpi_streak),
                stringResource(Res.string.stats_kpi_days, stats.currentStreak.toString()),
                m
            ) {
                KpiNote(stringResource(Res.string.stats_kpi_best, stats.bestStreak.toString()))
            }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .glassSurface(colors, GlassGroupShape)
            .padding(18.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = AppTheme.typography.headlineLarge.merge(TabularNumbers).copy(letterSpacing = (-0.8).sp),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        note()
    }
}

@Composable
private fun KpiNote(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ---- panels ------------------------------------------------------------------------------------

@Composable
private fun StatsPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .glassSurface(colors, GlassGroupShape)
            .padding(18.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() }
        )
        content()
    }
}

@Composable
private fun TopArtistsList(artists: List<ArtistCount>) {
    val colors = AppTheme.colors
    if (artists.isEmpty()) {
        Text(
            text = stringResource(Res.string.stats_artists_empty),
            style = AppTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        return
    }
    val maxPlays = artists.maxOf { it.plays }.coerceAtLeast(1)
    val playsTemplate = artists.map { stringResource(Res.string.stats_kpi_plays, it.plays.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        artists.forEachIndexed { index, artist ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = AppTheme.typography.labelMedium.merge(TabularNumbers),
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.width(18.dp)
                )
                ArtistAvatar(artist)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = artist.name,
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = playsTemplate[index],
                            style = AppTheme.typography.labelMedium.merge(TabularNumbers),
                            color = colors.onSurfaceVariant
                        )
                    }
                    ShareBar(
                        fraction = artist.plays.toFloat() / maxPlays,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(artist: ArtistCount) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = artist.name.trim().take(1).uppercase(),
            style = AppTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant
        )
        if (artist.imageUrl != null) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GenreList(genres: List<GenreShare>) {
    val colors = AppTheme.colors
    val maxPercent = genres.maxOf { it.percent }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        genres.forEach { genre ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = genre.name,
                        style = AppTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${genre.percent}%",
                        style = AppTheme.typography.labelMedium.merge(TabularNumbers),
                        color = colors.onSurfaceVariant
                    )
                }
                ShareBar(
                    fraction = genre.percent.toFloat() / maxPercent,
                    color = colors.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ShareBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(AppTheme.colors.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(color)
        )
    }
}

// ---- chart -------------------------------------------------------------------------------------

/** Pixel layout of the bar chart, shared by drawing, hit testing and the tooltip. */
private class ChartGeometry(
    val width: Float,
    val height: Float,
    val gutter: Float,
    val top: Float,
    val bottom: Float,
    val count: Int,
    barFraction: Float
) {
    val slot = if (count > 0) (width - gutter) / count else 0f
    val barWidth = slot * barFraction
    val baseline = height - bottom
    private val plotHeight = (baseline - top).coerceAtLeast(1f)

    fun barLeft(index: Int) = gutter + index * slot + (slot - barWidth) / 2f
    fun barCenter(index: Int) = gutter + index * slot + slot / 2f
    fun y(value: Float, max: Float) = baseline - (value / max) * plotHeight
    fun indexAt(x: Float): Int? =
        if (slot <= 0f || x < gutter) null else ((x - gutter) / slot).toInt().takeIf { it in 0 until count }
}

/** Y-axis step so that four steps cover [max] with round numbers. */
private fun niceStep(max: Int): Int {
    val raw = ceil(max.coerceAtLeast(1) / 4.0).toInt()
    return listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120, 180, 240, 300, 360)
        .firstOrNull { it >= raw }
        ?: (ceil(raw / 60.0).toInt() * 60)
}

/** ISO day of month without depending on a specific kotlinx-datetime accessor name. */
private fun LocalDate.dayOfMonthNumber(): Int = toString().takeLast(2).toInt()

@Composable
private fun MinutesChart(days: List<DayMinutes>, range: StatsRange, today: LocalDate) {
    val colors = AppTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val weekdays = listOf(
        stringResource(Res.string.stats_weekday_mon),
        stringResource(Res.string.stats_weekday_tue),
        stringResource(Res.string.stats_weekday_wed),
        stringResource(Res.string.stats_weekday_thu),
        stringResource(Res.string.stats_weekday_fri),
        stringResource(Res.string.stats_weekday_sat),
        stringResource(Res.string.stats_weekday_sun)
    )
    fun weekday(date: LocalDate) = weekdays[date.dayOfWeek.isoDayNumber - 1]
    val axisLabels = days.mapIndexed { index, day ->
        when (range) {
            StatsRange.Week -> weekday(day.date)
            // Every fifth day, counted back from today so today is always labelled.
            StatsRange.Month -> if ((days.size - 1 - index) % 5 == 0) day.date.dayOfMonthNumber().toString() else ""
        }
    }
    val tooltipDays = days.map { day ->
        when (range) {
            StatsRange.Week -> weekday(day.date)
            StatsRange.Month -> "${weekday(day.date)} ${day.date.dayOfMonthNumber()}"
        }
    }
    val tooltipTexts = days.mapIndexed { index, day ->
        stringResource(Res.string.stats_bar_tooltip, tooltipDays[index], day.minutes.toString())
    }
    val description = stringResource(Res.string.stats_chart_description) + ". " + tooltipTexts.joinToString(", ")

    val peakMinutes = days.maxOfOrNull { it.minutes } ?: 0
    val peakIndex = if (peakMinutes > 0) days.indexOfFirst { it.minutes == peakMinutes } else -1
    val step = niceStep(peakMinutes)
    val axisMax = step * 4

    var size by remember { mutableStateOf(IntSize.Zero) }
    var hovered by remember { mutableStateOf<Int?>(null) }

    val axisStyle = AppTheme.typography.labelSmall.merge(TabularNumbers).copy(color = colors.onSurfaceVariant.copy(alpha = 0.7f))
    val dayStyle = AppTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val todayStyle = dayStyle.copy(color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    val gridColor = colors.onSurface.copy(alpha = 0.07f)
    val barColor = colors.primary
    val barFraction = if (range == StatsRange.Week) 0.5f else 0.62f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .semantics { contentDescription = description }
                .pointerInput(days.size) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            hovered = when (event.type) {
                                PointerEventType.Exit -> null
                                else -> position?.let { pos ->
                                    ChartGeometry(
                                        size.width.toFloat(), size.height.toFloat(),
                                        gutter = 28.dp.toPx(), top = 10.dp.toPx(), bottom = 26.dp.toPx(),
                                        count = days.size, barFraction = barFraction
                                    ).indexAt(pos.x)
                                }
                            }
                        }
                    }
                }
        ) {
            val geometry = ChartGeometry(
                this.size.width, this.size.height,
                gutter = 28.dp.toPx(), top = 10.dp.toPx(), bottom = 26.dp.toPx(),
                count = days.size, barFraction = barFraction
            )
            // Grid lines + y labels.
            for (i in 0..4) {
                val value = step * i
                val y = geometry.y(value.toFloat(), axisMax.toFloat())
                drawLine(gridColor, Offset(geometry.gutter - 4.dp.toPx(), y), Offset(this.size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(value.toString(), axisStyle)
                drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
            }
            // Bars (4dp rounded tops) and day labels.
            val radius = 4.dp.toPx()
            days.forEachIndexed { index, day ->
                if (day.minutes > 0) {
                    val left = geometry.barLeft(index)
                    val top = geometry.y(day.minutes.toFloat(), axisMax.toFloat())
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = left + geometry.barWidth,
                                bottom = geometry.baseline,
                                topLeftCornerRadius = CornerRadius(radius, radius),
                                topRightCornerRadius = CornerRadius(radius, radius),
                                bottomRightCornerRadius = CornerRadius.Zero,
                                bottomLeftCornerRadius = CornerRadius.Zero
                            )
                        )
                    }
                    val emphasised = index == hovered || index == peakIndex
                    drawPath(path, barColor, alpha = if (emphasised) 1f else 0.55f)
                }
                val text = axisLabels[index]
                if (text.isNotEmpty()) {
                    val style = if (day.date == today) todayStyle else dayStyle
                    val label = textMeasurer.measure(text, style)
                    drawText(
                        label,
                        topLeft = Offset(
                            geometry.barCenter(index) - label.size.width / 2f,
                            this.size.height - label.size.height
                        )
                    )
                }
            }
        }

        val hoveredIndex = hovered
        if (hoveredIndex != null && hoveredIndex in days.indices && size.width > 0) {
            ChartTooltip(
                text = tooltipTexts[hoveredIndex],
                anchor = { density ->
                    val geometry = ChartGeometry(
                        size.width.toFloat(), size.height.toFloat(),
                        gutter = with(density) { 28.dp.toPx() },
                        top = with(density) { 10.dp.toPx() },
                        bottom = with(density) { 26.dp.toPx() },
                        count = days.size, barFraction = barFraction
                    )
                    Offset(
                        geometry.barCenter(hoveredIndex),
                        geometry.y(days[hoveredIndex].minutes.toFloat(), axisMax.toFloat())
                    )
                }
            )
        }
    }
}

/** Small dark bubble above the hovered bar (mockup `.tip`), kept inside the chart's width. */
@Composable
private fun ChartTooltip(text: String, anchor: (androidx.compose.ui.unit.Density) -> Offset) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = text,
        style = AppTheme.typography.labelMedium,
        color = colors.onSurface,
        maxLines = 1,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val point = anchor(this)
                val gap = 6.dp.roundToPx()
                val x = (point.x - placeable.width / 2f).toInt()
                    .coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
                val y = (point.y.toInt() - placeable.height - gap).coerceAtLeast(0)
                layout(placeable.width, placeable.height) { placeable.place(x, y) }
            }
            .clip(shape)
            .background(colors.background.copy(alpha = 0.92f))
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun StatsEmptyState() {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(colors, GlassGroupShape)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                imageVector = Glyphs.Stats,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.stats_empty_title),
            style = AppTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(Res.string.stats_empty_body),
            style = AppTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}
