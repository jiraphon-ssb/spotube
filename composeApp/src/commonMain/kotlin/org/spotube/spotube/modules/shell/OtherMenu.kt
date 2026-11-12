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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_menu_about
import spotube.composeapp.generated.resources.shell_menu_devices
import spotube.composeapp.generated.resources.shell_menu_downloads
import spotube.composeapp.generated.resources.shell_menu_downloads_active
import spotube.composeapp.generated.resources.shell_menu_jam
import spotube.composeapp.generated.resources.shell_menu_local_files
import spotube.composeapp.generated.resources.shell_menu_open
import spotube.composeapp.generated.resources.shell_menu_plugins
import spotube.composeapp.generated.resources.shell_menu_settings

/** Destinations in the top-right "other" menu. */
enum class OtherMenuItem { Settings, Downloads, Plugins, Devices, Jam, LocalFiles, About }

/** Width of the "other" dropdown. */
internal val OtherMenuWidth = 250.dp

/**
 * The round avatar button at the top-right that opens the "other" menu. There is no signed-in
 * profile to read (metadata plugins expose no "me" endpoint), so it shows a person glyph.
 */
@Composable
internal fun OtherMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassRoundButton(
        label = stringResource(Res.string.shell_menu_open),
        onClick = onClick,
        modifier = modifier
    ) {
        MillenniumIcon(
            Glyphs.Person,
            contentDescription = null,
            tint = AppTheme.colors.onSurface,
            size = 18.dp
        )
    }
}

/**
 * The "other" dropdown: everything that isn't a primary destination. Composed only while open;
 * keyboard focus moves to its first row so Tab walks the items.
 *
 * @param activeDownloads downloads queued or in progress; shown as a badge when above zero.
 */
@Composable
internal fun OtherMenuPanel(
    activeDownloads: Int,
    onSelect: (OtherMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstItem.requestFocus() } }

    Column(
        modifier = modifier
            .width(OtherMenuWidth)
            .shellGlass(
                RoundedCornerShape(18.dp),
                fill = shellGlassFill(alpha = 0.94f),
                border = colors.outline,
                elevation = 24.dp
            )
            .padding(8.dp)
    ) {
        MenuRow(
            icon = Glyphs.Settings,
            label = stringResource(Res.string.shell_menu_settings),
            onClick = { onSelect(OtherMenuItem.Settings) },
            modifier = Modifier.focusRequester(firstItem),
            trailing = { ShortcutHint(shortcutLabel(",")) }
        )
        val downloadsLabel = stringResource(Res.string.shell_menu_downloads)
        val downloadsA11y = if (activeDownloads > 0) {
            stringResource(Res.string.shell_menu_downloads_active, activeDownloads)
        } else {
            downloadsLabel
        }
        MenuRow(
            icon = Glyphs.Download,
            label = downloadsLabel,
            a11yLabel = downloadsA11y,
            onClick = { onSelect(OtherMenuItem.Downloads) },
            trailing = if (activeDownloads > 0) {
                { CountBadge(activeDownloads) }
            } else {
                null
            }
        )
        MenuRow(
            icon = Glyphs.Plugins,
            label = stringResource(Res.string.shell_menu_plugins),
            onClick = { onSelect(OtherMenuItem.Plugins) }
        )
        MenuRow(
            icon = Glyphs.Devices,
            label = stringResource(Res.string.shell_menu_devices),
            onClick = { onSelect(OtherMenuItem.Devices) }
        )
        MenuRow(
            icon = Glyphs.GroupJam,
            label = stringResource(Res.string.shell_menu_jam),
            onClick = { onSelect(OtherMenuItem.Jam) }
        )
        MenuRow(
            icon = Glyphs.LocalFiles,
            label = stringResource(Res.string.shell_menu_local_files),
            onClick = { onSelect(OtherMenuItem.LocalFiles) }
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline)
        )
        MenuRow(
            icon = Glyphs.Info,
            label = stringResource(Res.string.shell_menu_about),
            onClick = { onSelect(OtherMenuItem.About) }
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    a11yLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .keyboardFocusRing(shape)
            .clip(shape)
            .background(if (hovered) colors.hover else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (a11yLabel != null) Modifier.semantics { contentDescription = a11yLabel } else Modifier
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillenniumIcon(icon, contentDescription = null, tint = colors.onSurfaceVariant, size = 17.dp)
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun ShortcutHint(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        color = AppTheme.colors.onSurfaceVariant,
        maxLines = 1
    )
}
