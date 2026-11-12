/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library

import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.spotube.spotube.core.ui.component.PillChipRow
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only reports viewport geometry. The owning ViewModel decides when to load another page. */
@Composable
internal fun <T> LibraryMediaCollection(
    items: List<T>,
    itemKey: (T) -> String,
    viewMode: LibraryViewMode,
    onViewportChanged: (Int?, Int) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit,
    skeleton: @Composable (Boolean) -> Unit,
    loading: Boolean = false,
    loadingMore: Boolean = false,
    header: (@Composable (Boolean) -> Unit)? = null,
    /** Name to file each item under in the A–Z divider index (grid view only). */
    indexLabel: ((T) -> String)? = null,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val reportViewport by rememberUpdatedState(onViewportChanged)
    val padding = PaddingValues(top = 16.dp, bottom = LocalAppShellBottomInset.current + 16.dp)
    LaunchedEffect(viewMode, gridState, listState) {
        val positions = if (viewMode == LibraryViewMode.List) snapshotFlow {
            listState.layoutInfo.let { it.visibleItemsInfo.lastOrNull()?.index to it.totalItemsCount }
        } else snapshotFlow {
            gridState.layoutInfo.let { it.visibleItemsInfo.lastOrNull()?.index to it.totalItemsCount }
        }
        positions.distinctUntilChanged().collect { (last, total) -> reportViewport(last, total) }
    }
    if (viewMode == LibraryViewMode.List) {
        LazyColumn(
            modifier.fillMaxSize(), state = listState, contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) items(8) { SkeletonTree(true) { skeleton(true) } }
            else {
                if (header != null) item(key = "library_header") { header(true) }
                itemsWithUniqueKeys(items, id = itemKey, keyPrefix = "media:") { itemContent(it, true) }
                if (loadingMore) items(4) { SkeletonTree(true) { skeleton(true) } }
            }
        }
    } else {
        val headerOffset = if (header != null) 1 else 0
        // Only worth an index once there's something to flip through, and only when the list is
        // in name order; under any other sort the dividers would point at scattered items.
        val nameOrdered = remember(items, indexLabel) {
            indexLabel != null && items.map { indexOrder(indexLetter(indexLabel(it))) }
                .zipWithNext().all { (a, b) -> compareValues(a.first, b.first).let { it < 0 || (it == 0 && a.second <= b.second) } }
        }
        val showIndex = indexLabel != null && !loading && items.size >= 12 && nameOrdered
        Column(modifier.fillMaxSize()) {
        if (showIndex) {
            LibraryIndexDividers(
                items = items,
                label = indexLabel!!,
                gridState = gridState,
                headerOffset = headerOffset
            )
        }
        // Cards carry their own inset (the hover plate), so the crate packs them tight.
        LazyVerticalGrid(
            GridCells.Adaptive(160.dp), Modifier.fillMaxWidth().weight(1f), state = gridState,
            contentPadding = padding, horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (loading) items(8) { SkeletonTree(true) { skeleton(false) } }
            else {
                if (header != null) item(key = "library_header") { header(false) }
                itemsWithUniqueKeys(items, id = itemKey, keyPrefix = "media:") { itemContent(it, false) }
                if (loadingMore) items(4) { SkeletonTree(true) { skeleton(false) } }
            }
        }
        }
    }
}

/** Thai leading vowels: dictionaries file a word under the consonant that follows. */
private val ThaiLeadingVowels = setOf('เ', 'แ', 'โ', 'ใ', 'ไ')

/** The divider a name is filed under: "#" for digits/symbols, A–Z, or a Thai consonant. */
internal fun indexLetter(name: String): String {
    val c = name.trim().firstOrNull { it.isLetterOrDigit() && it !in ThaiLeadingVowels } ?: return "#"
    return when {
        c.isDigit() -> "#"
        c in 'ก'..'ฮ' -> c.toString()
        c.isLetter() -> c.uppercaseChar().toString()
        else -> "#"
    }
}

/** "#" first, then Latin, then Thai, each in its own alphabetical order. */
private fun indexOrder(letter: String): Pair<Int, String> = when {
    letter == "#" -> 0 to letter
    letter.first() in 'ก'..'ฮ' -> 2 to letter
    else -> 1 to letter
}

/**
 * The library's A–Z index: one chip per starting letter present. Clicking a chip jumps to its
 * first item; the chip of the first visible item is selected as you scroll.
 */
@Composable
private fun <T> LibraryIndexDividers(
    items: List<T>,
    label: (T) -> String,
    gridState: LazyGridState,
    headerOffset: Int
) {
    val scope = rememberCoroutineScope()
    val letters = remember(items) { items.map { indexLetter(label(it)) } }
    val tabs = remember(letters) { letters.distinct().sortedWith(compareBy({ indexOrder(it).first }, { it })) }
    val current by remember(letters, tabs) {
        derivedStateOf {
            letters.getOrNull(gridState.firstVisibleItemIndex - headerOffset)?.let(tabs::indexOf) ?: -1
        }
    }
    PillChipRow(
        labels = tabs,
        selectedIndex = current,
        onSelect = { tab ->
            val first = letters.indexOf(tabs[tab])
            if (first >= 0) scope.launch { gridState.animateScrollToItem(first + headerOffset) }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    )
}
