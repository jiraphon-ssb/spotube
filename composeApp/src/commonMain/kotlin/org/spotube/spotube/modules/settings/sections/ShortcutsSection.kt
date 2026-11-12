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

package org.spotube.spotube.modules.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_glass_nav_shortcuts
import spotube.composeapp.generated.resources.settings_glass_shortcut_close
import spotube.composeapp.generated.resources.settings_glass_shortcut_play
import spotube.composeapp.generated.resources.settings_glass_shortcut_search
import spotube.composeapp.generated.resources.settings_glass_shortcut_settings
import spotube.composeapp.generated.resources.settings_glass_shortcut_skip

private data class Shortcut(val label: StringResource, val keys: String)

/** Read-only list of the shell's keyboard shortcuts (desktop only). */
internal fun LazyListScope.shortcutsSection() {
    val mod = if (getPlatform().type == PlatformType.MacOS) "⌘" else "Ctrl+"
    val shortcuts = listOf(
        Shortcut(Res.string.settings_glass_shortcut_play, "Space"),
        Shortcut(Res.string.settings_glass_shortcut_skip, "$mod→ / $mod←"),
        Shortcut(Res.string.settings_glass_shortcut_search, "${mod}K"),
        Shortcut(Res.string.settings_glass_shortcut_settings, "$mod,"),
        Shortcut(Res.string.settings_glass_shortcut_close, "Esc")
    )
    settingsSectionCard(
        title = Res.string.settings_glass_nav_shortcuts,
        items = shortcuts.map { shortcut ->
            // Typed explicitly: a bare lambda inferred through map's type variable may not be
            // treated as @Composable.
            val row: @Composable () -> Unit = { ShortcutRow(stringResource(shortcut.label), shortcut.keys) }
            row
        }
    )
}

@Composable
private fun ShortcutRow(label: String, keys: String) {
    val colors = AppTheme.colors
    val keyShape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 11.dp)
            .clearAndSetSemantics { contentDescription = "$label: $keys" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            label,
            style = AppTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            keys,
            style = AppTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            modifier = Modifier
                .clip(keyShape)
                .background(colors.surfaceContainerHigh)
                .border(1.dp, colors.outline, keyShape)
                .padding(horizontal = 8.dp, vertical = 1.dp)
        )
    }
}
