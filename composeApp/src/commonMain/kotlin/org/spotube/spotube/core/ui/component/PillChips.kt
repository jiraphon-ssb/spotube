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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * One filter chip: a quiet glass pill that turns solid (text colour on canvas) when selected.
 * Hover only brightens the label and plate; no gloss, no lift.
 */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> colors.onSurface
            hovered -> colors.surfaceContainerHighest
            else -> colors.surfaceContainerHigh
        },
        tween(140),
        label = "chip_bg"
    )
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        style = AppTheme.typography.labelLarge,
        color = when {
            selected -> colors.background
            hovered -> colors.onSurface
            else -> colors.onSurfaceVariant
        },
        maxLines = 1,
        modifier = modifier
            // Ring before clip/clickable: the chip has no indication, so this is its only focus cue.
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 32.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** A horizontally scrolling row of [PillChip]s; [selectedIndex] -1 selects none. */
@Composable
fun PillChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val rowState = rememberLazyListState()
    LazyRow(
        state = rowState,
        modifier = modifier.dragScrollable(rowState),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(labels) { index, label ->
            PillChip(label = label, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}
