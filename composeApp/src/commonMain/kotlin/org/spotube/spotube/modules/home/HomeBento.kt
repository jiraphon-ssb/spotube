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

package org.spotube.spotube.modules.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.spotube.core.history.ListeningHistoryRepository
import org.spotube.spotube.core.history.RecentCollection
import org.spotube.spotube.core.history.RecentType
import org.spotube.spotube.core.history.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.core.ui.component.FeaturedItem
import org.spotube.spotube.core.ui.component.FeaturedKind
import org.spotube.spotube.core.ui.component.FeaturedPlayButton
import org.spotube.spotube.core.ui.component.addToQueue
import org.spotube.spotube.core.ui.component.cards.glassPlate
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.component.open
import org.spotube.spotube.core.ui.component.toFeatured
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.coolGlow
import org.spotube.spotube.core.ui.theming.rememberArtworkTint
import org.spotube.spotube.modules.shell.LocalShellActions
import org.spotube.spotube.modules.shell.roomLightPreview
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.a11y_play_item
import spotube.composeapp.generated.resources.featured_open
import spotube.composeapp.generated.resources.media_album
import spotube.composeapp.generated.resources.media_playlist
import spotube.composeapp.generated.resources.media_song
import spotube.composeapp.generated.resources.pages_added_to_queue
import spotube.composeapp.generated.resources.pages_home_add_to_queue
import spotube.composeapp.generated.resources.pages_home_continue_subtitle
import spotube.composeapp.generated.resources.pages_home_continue_title
import spotube.composeapp.generated.resources.pages_home_greeting_afternoon
import spotube.composeapp.generated.resources.pages_home_greeting_evening
import spotube.composeapp.generated.resources.pages_home_greeting_morning
import spotube.composeapp.generated.resources.pages_home_greeting_night
import spotube.composeapp.generated.resources.pages_home_hours_left
import spotube.composeapp.generated.resources.pages_home_minutes_left
import spotube.composeapp.generated.resources.pages_home_week_hours
import spotube.composeapp.generated.resources.pages_home_week_minutes
import spotube.composeapp.generated.resources.pages_home_week_stats
import kotlin.time.Clock

/**
 * Home's top: the time-of-day greeting with the "this week" listening chip (hidden until there is
 * listening time; opens Stats).
 */
@Composable
internal fun HomeGreeting(
    modifier: Modifier = Modifier,
    history: ListeningHistoryRepository = koinInject(),
    navigationCommands: NavigationCommands = koinInject()
) {
    val colors = AppTheme.colors
    val minutes by history.minutesThisWeek.collectAsStateWithLifecycle(initialValue = 0)
    val hour = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    }
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> Res.string.pages_home_greeting_morning
            in 12..16 -> Res.string.pages_home_greeting_afternoon
            in 17..21 -> Res.string.pages_home_greeting_evening
            else -> Res.string.pages_home_greeting_night
        }
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = greeting,
            style = AppTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp
            ),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (minutes > 0) {
            WeekChip(minutes = minutes, onClick = { navigationCommands.navigateTo(Routes.Stats) })
        }
    }
}

@Composable
private fun WeekChip(minutes: Int, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(50)
    val hours = minutes / 60
    val rest = minutes % 60
    val label = if (hours > 0) {
        stringResource(Res.string.pages_home_week_hours, hours, rest)
    } else {
        stringResource(Res.string.pages_home_week_minutes, rest)
    }
    Row(
        modifier = Modifier
            .height(40.dp)
            .keyboardFocusRing(shape)
            .glassPlate(shape)
            .background(if (hovered) colors.hover else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .drawBehind {
                    drawCircle(colors.primary.copy(alpha = 0.35f), radius = size.minDimension * 1.2f)
                }
                .clip(CircleShape)
                .background(colors.primary)
        )
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            maxLines = 1
        )
        Text(
            text = "· " + stringResource(Res.string.pages_home_week_stats),
            style = AppTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * The bento under the greeting: a large featured glass card on the left; on the right a
 * "Continue" card (recent collections from the listening history, hidden when there are none) and
 * three cover tiles that open the next featured picks. Collapses to one column when narrow.
 */
@Composable
internal fun HomeBento(
    featured: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier,
    history: ListeningHistoryRepository = koinInject()
) {
    val items = remember(featured) { featured.mapNotNull { it.toFeatured() }.distinctBy { it.key } }
    val recentAll by history.recentCollections.collectAsStateWithLifecycle(initialValue = emptyList())
    // Single songs have no page to reopen; Continue is for collections.
    val recent = remember(recentAll) { recentAll.filter { it.type != RecentType.Track }.take(3) }
    val hero = items.firstOrNull()
    val moods = remember(items) { items.drop(1).take(3) }
    if (hero == null && recent.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 860.dp
        val side = @Composable { mod: Modifier ->
            if (recent.isNotEmpty() || moods.isNotEmpty()) {
                Column(mod, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recent.isNotEmpty()) ContinueCard(recent)
                    if (moods.isNotEmpty()) MoodRow(moods)
                }
            }
        }
        if (hero != null && twoColumns && (recent.isNotEmpty() || moods.isNotEmpty())) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                FeatureCard(hero, Modifier.weight(1.5f))
                side(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hero != null) FeatureCard(hero, Modifier.fillMaxWidth())
                side(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FeatureCard(
    item: FeaturedItem,
    modifier: Modifier = Modifier,
    navigationCommands: NavigationCommands = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject()
) {
    val colors = AppTheme.colors
    val shellActions = LocalShellActions.current
    val tint by rememberArtworkTint(item.artworkUrl, fallback = colors.ambient)
    val shape = RoundedCornerShape(24.dp)
    val addedMessage = stringResource(Res.string.pages_added_to_queue, item.title)
    BoxWithConstraints(
        modifier = modifier
            .height(360.dp)
            .glassPlate(shape)
            // The cover's light pools behind it, top right.
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.coolGlow(tint, 0.3f).copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.3f),
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
    ) {
        val pad = 30.dp
        val artSize = minOf(280.dp, maxHeight - pad * 2, maxWidth * 0.42f)
        val textWidth = maxWidth - artSize - pad * 3
        val bigTitle = textWidth >= 380.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(pad)
                .size(artSize)
                .shadow(40.dp, RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceContainerHighest)
                .roomLightPreview(item.artworkUrl)
                // A pointer shortcut only: "Open" is the keyboard path.
                .focusProperties { canFocus = false }
                .clickable(onClickLabel = item.title) { item.open(navigationCommands) }
        ) {
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = pad, bottom = pad, end = artSize + pad * 2)
        ) {
            KindTag(item.kind)
            Text(
                text = item.title,
                style = if (bigTitle) {
                    AppTheme.typography.displayLarge.copy(letterSpacing = (-2.2).sp)
                } else {
                    AppTheme.typography.displaySmall
                },
                color = colors.onSurface,
                maxLines = if (bigTitle) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            val line = item.note ?: item.subtitle
            if (!line.isNullOrBlank()) {
                Text(
                    text = line,
                    style = AppTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturedPlayButton(
                    item = item,
                    size = 52.dp,
                    contentDescription = stringResource(Res.string.a11y_play_item, item.title)
                )
                GlassPill(
                    label = stringResource(Res.string.featured_open),
                    onClick = { item.open(navigationCommands) }
                )
                GlassPill(
                    label = stringResource(Res.string.pages_home_add_to_queue),
                    icon = Glyphs.AddTo,
                    onClick = {
                        // The destination picker adds it asynchronously (maybe on another
                        // device), so there is no reliable entry to take back: no undo.
                        if (item.addToQueue(remotePlaybackController)) {
                            shellActions.showToast(addedMessage, item.artworkUrl)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KindTag(kind: FeaturedKind) {
    val colors = AppTheme.colors
    Text(
        text = stringResource(
            when (kind) {
                FeaturedKind.Album -> Res.string.media_album
                FeaturedKind.Playlist -> Res.string.media_playlist
                FeaturedKind.Song -> Res.string.media_song
            }
        ),
        style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = colors.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** A 42dp glass pill button (label, optional leading glyph). */
@Composable
internal fun GlassPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: androidx.compose.ui.unit.Dp = 42.dp,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(50)
    val ink = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .height(height)
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            MillenniumIcon(imageVector = icon, contentDescription = null, tint = ink, size = 17.dp)
        }
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueCard(recent: List<RecentCollection>, navigationCommands: NavigationCommands = koinInject()) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPlate(RoundedCornerShape(24.dp))
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.pages_home_continue_title),
            style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(Res.string.pages_home_continue_subtitle),
            style = AppTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        recent.forEach { entry ->
            ContinueRow(entry, onClick = { openRecent(entry, navigationCommands) })
        }
    }
}

internal fun openRecent(entry: RecentCollection, navigationCommands: NavigationCommands) {
    when (entry.type) {
        RecentType.Playlist -> navigationCommands.navigateTo(
            if (entry.id == SAVED_TRACKS_COLLECTION_ID) Routes.SavedTracks else Routes.Playlist(entry.id)
        )
        RecentType.Album -> navigationCommands.navigateTo(Routes.Album(entry.id))
        RecentType.Artist -> navigationCommands.navigateTo(Routes.Artist(entry.id))
        RecentType.Track -> Unit
    }
}

@Composable
private fun ContinueRow(entry: RecentCollection, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(if (hovered) colors.hover else Color.Transparent)
            .roomLightPreview(entry.coverUrl)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.surfaceContainerHighest)
        ) {
            if (entry.coverUrl != null) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = AppTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val remaining = entry.remainingMinutes
            if (remaining != null && remaining > 0) {
                Text(
                    text = if (remaining >= 60) {
                        stringResource(Res.string.pages_home_hours_left, remaining / 60, remaining % 60)
                    } else {
                        stringResource(Res.string.pages_home_minutes_left, remaining)
                    },
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        val progress = entry.progress
        if (progress != null) {
            Box(
                Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.onSurface)
                )
            }
        }
    }
}

/** Three short cover tiles (the next featured picks) with a dark left wash and their name. */
@Composable
private fun MoodRow(items: List<FeaturedItem>, navigationCommands: NavigationCommands = koinInject()) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            MoodTile(item, onClick = { item.open(navigationCommands) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoodTile(item: FeaturedItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(58.dp)
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(colors.surfaceContainerHighest)
            .border(1.dp, colors.outline, shape)
            .roomLightPreview(item.artworkUrl)
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = item.title,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF06090D).copy(alpha = if (hovered) 0.65f else 0.78f),
                            Color(0xFF06090D).copy(alpha = 0.2f)
                        )
                    )
                )
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                text = item.title,
                style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = item.subtitle
            if (!sub.isNullOrBlank()) {
                Text(
                    text = sub,
                    style = AppTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
