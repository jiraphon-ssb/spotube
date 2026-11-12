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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_overlay_close

// The wide layout's overlays. The shell renders each while its flag in AppShellViewModel is true
// (Escape and an outside click close it). The bodies live in QueueDrawer.kt, SoundSheet.kt and
// CommandPalette.kt; this file holds the slot entry points and the pieces they share.

/** The queue drawer: now playing, reorderable "up next", autoplay, save as playlist. */
@Composable
fun QueueDrawerSlot(
    onClose: () -> Unit,
    queueViewModel: PlayerQueueContentViewModel,
    modifier: Modifier = Modifier
) {
    QueueDrawer(onClose = onClose, queueViewModel = queueViewModel, modifier = modifier)
}

/** The sound & EQ sheet. [onOpenSettings] closes the sheet and goes to Settings. */
@Composable
fun SoundSheetSlot(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    SoundSheet(onClose = onClose, onOpenSettings = onOpenSettings, modifier = modifier)
}

/**
 * The ⌘K command palette: search results plus app commands. [onNavigate] and [onOpenSearch]
 * are called after the palette asked to close.
 */
@Composable
fun CommandPaletteSlot(
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigate: (Routes) -> Unit = {}
) {
    CommandPalette(
        onClose = onClose,
        onOpenSearch = onOpenSearch,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

internal val OverlayPanelShape = RoundedCornerShape(24.dp)
internal val OverlayPanelWidth = 380.dp
private val OverlayEdge = 14.dp

/** Mockup "line": the hairline used for dividers inside overlays. */
internal val OverlayLine = Color.White.copy(alpha = 0.10f)

/** Full-window layer that closes the overlay on an outside click. */
@Composable
internal fun OverlayDismissLayer(onDismiss: () -> Unit, scrim: Color = Color.Transparent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}

/**
 * A floating glass panel on the right edge (queue drawer, sound sheet): 380dp wide, 14dp from
 * the top and right, ending above the player dock.
 */
@Composable
internal fun RightOverlayPanel(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    val bottomInset = LocalAppShellBottomInset.current
    Box(modifier = modifier.fillMaxSize()) {
        OverlayDismissLayer(onDismiss = onClose)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = OverlayEdge,
                    end = OverlayEdge,
                    bottom = if (bottomInset > OverlayEdge) bottomInset else OverlayEdge
                )
                .width(OverlayPanelWidth)
                .fillMaxHeight()
                .shellGlass(
                    OverlayPanelShape,
                    fill = shellGlassFill(alpha = 0.94f),
                    border = colors.outline,
                    elevation = 24.dp
                )
                .blockPointerPassThrough()
                .semantics { paneTitle = title },
            content = content
        )
    }
}

/** Panel title (mockup h3) with a close button on the right. */
@Composable
internal fun OverlayHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp),
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        actions()
        QuietGlyphButton(
            icon = Glyphs.Close,
            contentDescription = stringResource(Res.string.shell_overlay_close),
            onClick = onClose,
            size = 32.dp,
            iconSize = 18.dp
        )
    }
}

/** Section label (mockup h4), with an optional faint value on the right. */
@Composable
internal fun OverlaySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                color = colors.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * Mockup `.seg`: equal-width options in a glass track, one selected. Screen readers get a radio
 * group; Tab reaches each option.
 */
@Composable
internal fun OverlaySegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    groupLabel: String? = null
) {
    val colors = AppTheme.colors
    val trackShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, OverlayLine, trackShape)
            .padding(3.dp)
            .selectableGroup()
            .then(if (groupLabel != null) Modifier.semantics { contentDescription = groupLabel } else Modifier)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val shape = RoundedCornerShape(9.dp)
            val background by animateColorAsState(
                if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                tween(150),
                label = "seg_bg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .keyboardFocusRing(shape)
                    .clip(shape)
                    .background(background)
                    .hoverable(interaction, enabled = enabled)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = if (selected || (hovered && enabled)) colors.onSurface else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/** Mockup `.switch`: drawing only; put it inside a [toggleable] row so the whole row is the target. */
@Composable
internal fun OverlaySwitchVisual(checked: Boolean, enabled: Boolean = true) {
    val colors = AppTheme.colors
    val track by animateColorAsState(
        if (checked) colors.primary else Color.White.copy(alpha = 0.18f),
        tween(200),
        label = "switch_track"
    )
    val thumbOffset by animateDpAsState(if (checked) 16.dp else 0.dp, tween(250), label = "switch_thumb")
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Mockup `.opt` with a switch: title, small body text, switch. The whole row toggles. */
@Composable
internal fun OverlayToggleRow(
    title: String,
    body: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(if (hovered && enabled) colors.hover.copy(alpha = 0.5f) else Color.Transparent)
            .hoverable(interaction, enabled = enabled)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverlayOptionText(title = title, body = body, modifier = Modifier.weight(1f))
        OverlaySwitchVisual(checked = checked, enabled = enabled)
    }
}

/** Mockup `.opt` with any trailing control (not itself clickable). */
@Composable
internal fun OverlayOptionRow(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverlayOptionText(title = title, body = body, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun OverlayOptionText(title: String, body: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = colors.onSurface
        )
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** 1dp hairline between option rows (mockup `.opt` border-top). */
@Composable
internal fun OverlayDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(OverlayLine))
}

/** Mockup `.selbox`: a small glass button showing a value, optionally with a chevron. */
@Composable
internal fun OverlaySelectBox(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(9.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .height(32.dp)
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(Color.White.copy(alpha = if (hovered && enabled) 0.10f else 0.06f))
            .border(1.dp, OverlayLine, shape)
            .hoverable(interaction, enabled = enabled)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            MillenniumIcon(
                Glyphs.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                size = 13.dp,
                modifier = Modifier.rotate(90f)
            )
        }
    }
}

/** Six-dot drag grip (Lucide "grip-vertical" as fills), not in [Glyphs]. */
internal val OverlayGripGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "OverlayGrip",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(9f, 15f).forEach { x ->
            listOf(5f, 12f, 19f).forEach { y ->
                val r = 1.4f
                addPath(
                    pathData = addPathNodes("M${x - r},${y}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z"),
                    fill = SolidColor(Color.Black)
                )
            }
        }
    }.build()
}
