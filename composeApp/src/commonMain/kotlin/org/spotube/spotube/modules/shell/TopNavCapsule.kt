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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_back
import spotube.composeapp.generated.resources.shell_nav_home
import spotube.composeapp.generated.resources.shell_nav_library
import spotube.composeapp.generated.resources.shell_nav_search
import spotube.composeapp.generated.resources.shell_nav_stats

private val PillShape = RoundedCornerShape(50)

/** White plate behind the selected destination. */
private val SelectedPlate = Color.White.copy(alpha = 0.14f)

/** Faint fill that makes the search item read as a field. */
private val FieldFill = Color.White.copy(alpha = 0.05f)

/**
 * The wide layout's primary navigation: a floating glass capsule centred at the top of the
 * window with Home, Library, Stats and a field-like Search button that opens the command palette
 * (⌘K / Ctrl K).
 *
 * @param currentRoute the active top-level route, for the selected plate.
 * @param compact narrows the search field for tight windows.
 */
@Composable
fun TopNavCapsule(
    currentRoute: NavKey?,
    onNavigate: (Routes) -> Unit,
    onOpenPalette: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .shellGlass(PillShape, fill = shellGlassFill(), border = colors.outline, elevation = 16.dp)
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPill(
            icon = Glyphs.Home,
            label = stringResource(Res.string.shell_nav_home),
            selected = currentRoute == Routes.Home,
            onClick = { onNavigate(Routes.Home) }
        )
        NavPill(
            icon = Glyphs.Library,
            label = stringResource(Res.string.shell_nav_library),
            selected = currentRoute == Routes.Library,
            onClick = { onNavigate(Routes.Library) }
        )
        NavPill(
            icon = Glyphs.Stats,
            label = stringResource(Res.string.shell_nav_stats),
            selected = currentRoute == Routes.Stats,
            onClick = { onNavigate(Routes.Stats) }
        )
        SearchPill(
            width = if (compact) 150.dp else 220.dp,
            selected = currentRoute == Routes.Search,
            onClick = onOpenPalette
        )
    }
}

@Composable
private fun pillColors(selected: Boolean, hovered: Boolean, rest: Color): Pair<Color, Color> {
    val colors = AppTheme.colors
    val background by animateColorAsState(
        targetValue = when {
            selected -> SelectedPlate
            hovered -> colors.hover
            else -> rest
        },
        animationSpec = tween(150),
        label = "nav_pill_bg"
    )
    val content by animateColorAsState(
        targetValue = if (selected || hovered) colors.onSurface else colors.onSurfaceVariant,
        animationSpec = tween(150),
        label = "nav_pill_fg"
    )
    return background to content
}

@Composable
private fun NavPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val (background, content) = pillColors(selected, hovered, Color.Transparent)
    Row(
        modifier = Modifier
            .height(34.dp)
            .keyboardFocusRing(PillShape)
            .clip(PillShape)
            .background(background)
            .hoverable(interactionSource)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillenniumIcon(icon, contentDescription = null, tint = content, size = 17.dp)
        Text(
            text = label,
            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchPill(width: Dp, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val (background, content) = pillColors(selected, hovered, FieldFill)
    Row(
        modifier = Modifier
            .width(width)
            .height(34.dp)
            .keyboardFocusRing(PillShape)
            .clip(PillShape)
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(start = 15.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillenniumIcon(Glyphs.Search, contentDescription = null, tint = content, size = 17.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.shell_nav_search),
            style = AppTheme.typography.labelLarge,
            color = content,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = shortcutLabel("K"),
                style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Round glass back button shown at the top-left while there is somewhere to go back to. */
@Composable
fun ShellBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassRoundButton(
        label = stringResource(Res.string.shell_back),
        onClick = onClick,
        modifier = modifier
    ) {
        MillenniumIcon(
            Glyphs.ChevronLeft,
            contentDescription = null,
            tint = AppTheme.colors.onSurface,
            size = 18.dp
        )
    }
}
