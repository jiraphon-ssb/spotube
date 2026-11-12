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

package org.spotube.spotube.core.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.launch
import org.spotube.spotube.core.ui.theming.AppTheme

/** The crate's easing: quick to leave, slow to settle, like a sleeve tipping onto your fingers. */
private val CrateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private const val FlipMillis = 320
private const val SpineAngle = 62f

/**
 * Record sleeves standing in a crate. Every sleeve shows only its spine except the facing one,
 * which tips forward full-face. Moving the mouse across the crate flips through it; left/right
 * arrows do the same from the keyboard and Enter opens the facing sleeve.
 *
 * Flipping follows real mouse movement only: a flipped sleeve widens and pushes its neighbours,
 * and Compose sends synthetic moves on layout changes, so moves whose x didn't change are ignored.
 * Touch drags scroll the crate without flipping.
 *
 * [facingIndex] is hoisted so callers can describe the facing sleeve (a shelf-talker, a caption).
 */
@Composable
fun <T> CrateRow(
    items: List<T>,
    key: (T) -> Any,
    cover: (T) -> String?,
    label: (T) -> String,
    facingIndex: Int,
    onFacingChange: (Int) -> Unit,
    onOpen: (T) -> Unit,
    modifier: Modifier = Modifier,
    faceSize: Dp = 230.dp,
    spineWidth: Dp = 40.dp,
    /** Called when the last sleeves come into view, to load the next page. */
    onNearEnd: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    if (onNearEnd != null) {
        val nearEnd by rememberUpdatedState(onNearEnd)
        LaunchedEffect(listState) {
            // Key on (last visible, total) so a new page that still fits on screen asks again.
            snapshotFlow {
                val info = listState.layoutInfo
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
            }.distinctUntilChanged().collect { (last, total) ->
                if (total > 0 && last >= total - 2) nearEnd()
            }
        }
    }
    val scope = rememberCoroutineScope()
    val focus = remember { MutableInteractionSource() }
    val focused by focus.collectIsFocusedAsState()
    val currentFacing by rememberUpdatedState(facingIndex)
    val currentItems by rememberUpdatedState(items)
    val setFacing by rememberUpdatedState(onFacingChange)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun step(delta: Int) {
        if (currentItems.isEmpty()) return
        val next = (currentFacing + delta).coerceIn(0, currentItems.lastIndex)
        if (next == currentFacing) return
        setFacing(next)
        // Keep a couple of spines visible to the left of the facing sleeve.
        scope.launch { listState.animateScrollToItem((next - 2).coerceAtLeast(0)) }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .height(faceSize + 16.dp)
            .focusable(interactionSource = focus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { step(-1); true }
                    Key.DirectionRight -> { step(1); true }
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        currentItems.getOrNull(currentFacing)?.let(onOpen); true
                    }
                    else -> false
                }
            }
            .pointerInput(rtl) {
                awaitPointerEventScope {
                    var lastX = Float.NaN
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Move) continue
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.type != PointerType.Mouse) continue
                        val rawX = change.position.x
                        if (rawX == lastX) continue
                        lastX = rawX
                        // Lazy item offsets run from the start edge, which is the right in RTL.
                        val x = if (rtl) size.width - rawX else rawX
                        val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                            x >= it.offset && x < it.offset + it.size
                        } ?: continue
                        if (hit.index != currentFacing) setFacing(hit.index)
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            Sleeve(
                model = cover(item),
                label = label(item),
                facing = index == facingIndex,
                showFocus = focused && index == facingIndex,
                faceSize = faceSize,
                spineWidth = spineWidth,
                onClick = { if (index == currentFacing) onOpen(item) else setFacing(index) }
            )
        }
    }
}

@Composable
private fun Sleeve(
    model: String?,
    label: String,
    facing: Boolean,
    showFocus: Boolean,
    faceSize: Dp,
    spineWidth: Dp,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (facing) 1f else 0f,
        animationSpec = tween(FlipMillis, easing = CrateEasing),
        label = "sleeve_flip"
    )
    val width = lerp(spineWidth, faceSize, progress)
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .padding(start = lerp(0.dp, 10.dp, progress), end = lerp(0.dp, 18.dp, progress))
            .width(width)
            .height(faceSize)
            .graphicsLayer {
                rotationY = SpineAngle * (1f - progress)
                transformOrigin = TransformOrigin(0f, 1f)
                cameraDistance = 14f * density
                translationY = -10.dp.toPx() * progress
            }
            .shadow(lerp(3.dp, 20.dp, progress), shape)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .then(if (showFocus) Modifier.border(2.dp, colors.kraft, shape) else Modifier)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                selected = facing
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Spines sit in the crate's shadow; the shade lifts as the sleeve comes forward.
        if (progress < 1f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - progress }
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.12f))
                        )
                    )
            )
        }
    }
}

/**
 * The crate itself: a kraft rim along the top, the crate floor, and a lip along the bottom.
 * [tabs] (divider cards) stand on the rim.
 */
@Composable
fun CrateFrame(
    modifier: Modifier = Modifier,
    tabs: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        if (tabs != null) Box(Modifier.padding(start = 12.dp)) { tabs() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(colors.surfaceContainerHigh)
                .drawBehind {
                    drawRect(colors.kraft, size = Size(size.width, 3.dp.toPx()))
                    val lip = 14.dp.toPx()
                    drawRect(colors.surfaceContainerHighest, topLeft = Offset(0f, size.height - lip), size = Size(size.width, lip))
                }
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Divider cards: kraft tabs standing up from a crate, labelled by hand. The selected card stands
 * tallest; hovering lifts a card halfway.
 */
@Composable
fun DividerTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowState = rememberLazyListState()
    LazyRow(
        state = rowState,
        modifier = modifier.dragScrollable(rowState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        itemsIndexed(labels) { index, label ->
            DividerTab(label = label, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun DividerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateDpAsState(
        targetValue = when {
            selected -> 12.dp
            hovered -> 10.dp
            else -> 6.dp
        },
        animationSpec = tween(200, easing = CrateEasing),
        label = "divider_lift"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
            .background(if (selected) colors.kraft else colors.surfaceContainerHighest)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = 16.dp, end = 16.dp, top = lift, bottom = 5.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.hand,
            color = when {
                selected -> colors.onKraft
                hovered -> colors.onSurface
                else -> colors.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

/** Ink on the shelf-talker's paper card. */
private val TalkerInk = Color(0xFF2A2019)
private val TalkerMeta = Color(0xFF6B5A48)

/**
 * A shelf-talker: the handwritten recommendation card a shop tapes next to a record. Paper card,
 * a strip of yellow tape, the title in type and the note by hand.
 */
@Composable
fun ShelfTalker(
    title: String,
    note: String?,
    meta: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    Box(modifier = modifier.width(300.dp).graphicsLayer { rotationZ = 1.6f }) {
        Column(
            modifier = Modifier
                .padding(top = 7.dp)
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(3.dp))
                .background(colors.onSurface, RoundedCornerShape(3.dp))
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)
        ) {
            Text(
                text = title,
                style = AppTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                color = TalkerInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = AppTheme.typography.hand,
                    color = TalkerInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (action != null || !meta.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    action?.invoke()
                    if (action != null) Spacer(Modifier.width(12.dp))
                    if (!meta.isNullOrBlank()) {
                        Text(
                            text = meta,
                            style = AppTheme.typography.labelLarge,
                            color = TalkerMeta,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 46.dp, height = 14.dp)
                .graphicsLayer { rotationZ = -3f }
                .background(colors.sticker.copy(alpha = 0.85f))
        )
    }
}
