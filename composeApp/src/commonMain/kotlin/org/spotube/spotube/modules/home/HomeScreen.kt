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


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackCard
import org.spotube.spotube.core.ui.component.UserCard
import org.spotube.spotube.core.ui.component.FeaturedCarousel
import org.spotube.spotube.core.ui.component.PillChipRow
import androidx.compose.foundation.shape.RoundedCornerShape
import org.spotube.spotube.core.ui.component.VerticalScrollbar
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.shell.ConnectStatusBanner
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.spotube.spotube.core.ui.component.WindowChromeBand
import org.spotube.spotube.core.ui.component.plainText
import org.spotube.spotube.core.ui.component.cards.AlbumCoverTile
import org.spotube.spotube.core.ui.component.cards.ArtPill
import org.spotube.spotube.core.ui.component.cards.ArtistCoverTile
import org.spotube.spotube.core.ui.component.cards.PlaylistCoverTile
import org.spotube.spotube.core.ui.component.cards.TrackCoverTile
import org.spotube.spotube.core.ui.component.cards.UserCoverTile
import org.spotube.spotube.core.ui.component.cards.isWideShell
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import spotube.composeapp.generated.resources.pages_home_new
import org.spotube.spotube.modules.support.LocalSupportPrompt
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.support_button_label
import spotube.composeapp.generated.resources.devices_title
import spotube.composeapp.generated.resources.home_title_browse
import spotube.composeapp.generated.resources.home_shelf_next
import spotube.composeapp.generated.resources.home_shelf_previous
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.jam_screen_title
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.spotube.spotube.modules.shell.SpotubeWordmark
import spotube.composeapp.generated.resources.common_retry
import spotube.composeapp.generated.resources.home_empty_body
import spotube.composeapp.generated.resources.home_empty_title
import spotube.composeapp.generated.resources.home_load_more_failed

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel) {
    val navigationCommands = koinInject<NavigationCommands>()
    val openSupport = LocalSupportPrompt.current
    val isSmallScreen = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    // Wide glass shell: no title bar (the floating nav capsule says where we are), no opaque
    // canvas (the room light shows through) and the content clears the floating chrome.
    val wide = isWideShell()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedGenreId = when (val s = state) {
        is HomeScreenState.Data -> s.selectedGenreId
        else -> null
    }

    LaunchedEffect(selectedGenreId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (
                    lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 2
                ) {
                    viewModel.loadMoreData()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (wide) Modifier else Modifier.background(MillenniumTheme.colors.background))
    ) {
        if (!wide) {
            ApplicationMainBar(
                backButton = false,
                title = {
                    if (isSmallScreen) {
                        SpotubeWordmark()
                    } else {
                        Text(stringResource(Res.string.home_title_browse))
                    }

                },
                actions = {
                    if (isSmallScreen) {
                        MillenniumIconButton(
                            onClick = { navigationCommands.navigateTo(Routes.Devices) },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxMirrorScreenRegular,
                                contentDescription = stringResource(Res.string.devices_title)
                            )
                        }
                        MillenniumIconButton(
                            onClick = { navigationCommands.navigateTo(Routes.Jam) },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxSoundTwotone,
                                contentDescription = stringResource(Res.string.jam_screen_title)
                            )
                        }
                        val supportLabel = stringResource(Res.string.support_button_label)
                        MillenniumIconButton(
                            onClick = openSupport,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart2,
                                contentDescription = supportLabel
                            )
                        }
                    }
                })
        }
        if (isSmallScreen) {
            ConnectStatusBanner(
                onOpenRemote = { navigationCommands.navigateTo(Routes.RemoteControl) },
                onOpenJam = { navigationCommands.navigateTo(Routes.Jam) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (!isSmallScreen) {
                HomeContent(
                    listState = listState,
                    state = state,
                    wide = wide,
                    onRetry = { viewModel.refresh() },
                    onRetryAppend = { viewModel.retryAppend() },
                    onGenreSelected = { viewModel.selectGenre(it) })
            } else {
                MillenniumPullToRefreshBox(
                    isRefreshing = state is HomeScreenState.Loading,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeContent(
                        listState = listState,
                        state = state,
                        wide = wide,
                        onRetry = { viewModel.refresh() },
                        onRetryAppend = { viewModel.retryAppend() },
                        onGenreSelected = { viewModel.selectGenre(it) })
                }
            }

            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
            // Keeps the window draggable (and its buttons) under the floating chrome.
            WindowChromeBand(Modifier.align(Alignment.TopCenter))
        }
    }
}

/** Minimum width of a shelf cover; the grid fits as many columns as this allows. */
private val TileMinWidth = 150.dp
private val TileGap = 18.dp

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    state: HomeScreenState,
    wide: Boolean,
    onRetry: () -> Unit,
    onRetryAppend: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    val shellBottomInset = LocalAppShellBottomInset.current
    val shellTopInset = LocalAppShellTopInset.current
    val contentPadding = remember(shellBottomInset, shellTopInset) {
        PaddingValues(top = shellTopInset + 16.dp, bottom = 16.dp + shellBottomInset)
    }
    val sidePadding = if (wide) 48.dp else 16.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = ((maxWidth - sidePadding * 2 + TileGap) / (TileMinWidth + TileGap))
            .toInt().coerceAtLeast(2)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(TileGap)
        ) {
            when (state) {
                is HomeScreenState.Loading -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(3) {
                                SkeletonTree(true) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .padding(horizontal = sidePadding)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(horizontal = sidePadding)
                                        ) {
                                            items(4) {
                                                PlayableCard(
                                                    title = "Item Title",
                                                    subtitle = "Subtitle",
                                                    imageURL = "https://placehold.co/600x400"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is HomeScreenState.Error -> {
                    item {
                        ErrorDisplay(
                            errorMessage = state.message,
                            onRetry = onRetry
                        )
                    }
                }

                is HomeScreenState.Data -> {
                    if (wide) {
                        item(key = "home_greeting") {
                            HomeGreeting(Modifier.padding(horizontal = sidePadding))
                        }
                        item(key = "home_bento") {
                            HomeBento(
                                featured = state.featuredItems,
                                modifier = Modifier.padding(horizontal = sidePadding)
                            )
                        }
                    } else if (state.featuredItems.isNotEmpty()) {
                        item(key = "home_featured") {
                            FeaturedCarousel(items = state.featuredItems)
                        }
                    }

                    if (state.genres.isNotEmpty()) {
                        item(key = "home_browse_header") {
                            BrowseHeader(
                                genres = state.genres,
                                selectedGenreId = state.selectedGenreId,
                                onGenreSelected = onGenreSelected,
                                sidePadding = sidePadding
                            )
                        }
                    }

                    // Sections the plugin returned without items would only render a lone title.
                    val currentSections = state.selectedGenreId
                        ?.let { state.browseSections[it] }
                        ?.filter { it.items.isNotEmpty() }

                    when {
                        // No genres at all: nothing will ever load into this slot, so don't skeleton.
                        state.selectedGenreId == null -> {
                            if (state.featuredItems.isEmpty()) {
                                item { HomeEmptyState(onRetry = onRetry) }
                            }
                        }

                        currentSections == null -> {
                            if (state.appendError) {
                                item { AppendErrorRow(onRetry = onRetryAppend) }
                            } else {
                                item { SectionSkeleton() }
                            }
                        }

                        currentSections.isEmpty() && !state.appendError -> {
                            if (state !is HomeScreenState.Data.LoadingMore) {
                                item { HomeEmptyState(onRetry = onRetry) }
                            }
                        }

                        else -> {
                            currentSections.forEachIndexed { sectionIndex, section ->
                                item(key = "home_section_${sectionIndex}_title") {
                                    SectionTitle(
                                        title = section.title,
                                        subtitle = section.description,
                                        modifier = Modifier.padding(horizontal = sidePadding)
                                    )
                                }
                                val rows = section.items.chunked(columns)
                                items(
                                    count = rows.size,
                                    key = { "home_section_${sectionIndex}_row_${it}_of_$columns" }
                                ) { rowIndex ->
                                    ShelfGridRow(
                                        items = rows[rowIndex],
                                        columns = columns,
                                        modifier = Modifier.padding(horizontal = sidePadding)
                                    )
                                }
                            }
                            if (state.appendError) {
                                item { AppendErrorRow(onRetry = onRetryAppend) }
                            }
                        }
                    }

                    if (state is HomeScreenState.Data.LoadingMore) {
                        item { SectionSkeleton() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSkeleton() {
    SkeletonTree(true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(4) {
                    PlayableCard(
                        title = "Item Title",
                        subtitle = "Subtitle",
                        imageURL = "https://placehold.co/600x400"
                    )
                }
            }
        }
    }
}

/** The plugin answered but had nothing to show: say so instead of leaving a blank page. */
@Composable
private fun HomeEmptyState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 420.dp)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(
            imageVector = Glyphs.Albums,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            size = 28.dp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.home_empty_body),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        MillenniumButton(onClick = onRetry, style = MillenniumButtonStyle.Secondary) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}

/** A failed page load under content that stays put. */
@Composable
private fun AppendErrorRow(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.home_load_more_failed),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant
        )
        MillenniumButton(onClick = onRetry, style = MillenniumButtonStyle.Quiet) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}

/** "Browse" with the genre chips beside it. */
@Composable
private fun BrowseHeader(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit,
    sidePadding: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = sidePadding, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.home_title_browse),
            style = SectionTitleStyle(),
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(end = 16.dp).semantics { heading() }
        )
        PillChipRow(
            labels = genres.map { it.name },
            selectedIndex = genres.indexOfFirst { it.id == selectedGenreId },
            onSelect = { onGenreSelected(genres[it].id) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = sidePadding)
        )
    }
}

/** A browse section's own name over its covers. */
@Composable
private fun SectionTitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (!subtitle.isNullOrEmpty()) Text(
            text = subtitle,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One row of the shelf grid; short rows keep their column widths. */
@Composable
private fun ShelfGridRow(items: List<MetadataBrowseItem>, columns: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
        verticalAlignment = Alignment.Top
    ) {
        items.forEach { browseItem ->
            val tileModifier = Modifier.weight(1f)
            when (browseItem) {
                is MetadataBrowseItem.Album -> AlbumCoverTile(album = browseItem.data, modifier = tileModifier)
                is MetadataBrowseItem.Artist -> ArtistCoverTile(artist = browseItem.data, modifier = tileModifier)
                is MetadataBrowseItem.Playlist -> PlaylistCoverTile(
                    playlist = browseItem.data,
                    modifier = tileModifier,
                    subtitle = browseItem.data.description?.let(::plainText)?.takeIf { it.isNotBlank() }
                        ?: browseItem.data.owner?.displayName
                )
                is MetadataBrowseItem.Track -> {
                    val isNew = remember(browseItem.data.album?.releaseDate) {
                        isNewRelease(browseItem.data.album?.releaseDate)
                    }
                    val newLabel = stringResource(Res.string.pages_home_new)
                    TrackCoverTile(
                        track = browseItem.data,
                        modifier = tileModifier,
                        topStart = { if (isNew) ArtPill(newLabel) }
                    )
                }
                is MetadataBrowseItem.User -> UserCoverTile(user = browseItem.data, modifier = tileModifier)
            }
        }
        repeat(columns - items.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** Released in the last four weeks (full "yyyy-MM-dd" dates only; year-only dates never count). */
private fun isNewRelease(releaseDate: String?): Boolean {
    if (releaseDate == null || releaseDate.length < 10) return false
    val released = runCatching { LocalDate.parse(releaseDate.substring(0, 10)) }.getOrNull() ?: return false
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return released.daysUntil(today) in 0..28
}

/** Section titles: 20/28 Bold, tightened, matching Artist and Search. */
@Composable
private fun SectionTitleStyle(): TextStyle =
    AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
