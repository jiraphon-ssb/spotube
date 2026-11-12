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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.spotubeTokens
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.modules.shell.LocalRoomLight
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_glass_accent_follow
import spotube.composeapp.generated.resources.settings_glass_accent_subtitle
import spotube.composeapp.generated.resources.settings_glass_accent_title
import spotube.composeapp.generated.resources.settings_glass_reduce_motion_subtitle
import spotube.composeapp.generated.resources.settings_glass_reduce_motion_title
import spotube.composeapp.generated.resources.settings_glass_room_light_subtitle
import spotube.composeapp.generated.resources.settings_glass_room_light_title
import spotube.composeapp.generated.resources.settings_palette_blue
import spotube.composeapp.generated.resources.settings_palette_lime
import spotube.composeapp.generated.resources.settings_palette_orange
import spotube.composeapp.generated.resources.settings_section_appearance

/** The "follow song" swatch: a colour wheel (mockup `.swatch.auto`), purely decorative. */
private val FollowSongSweep = listOf(
    Color(0xFFE88A8A), Color(0xFFE8C37A), Color(0xFF8FE3B5),
    Color(0xFF8AB8E8), Color(0xFFB59BE8), Color(0xFFE88A8A)
)

internal fun LazyListScope.appearanceSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true) {
    settingsSectionCard(
        title = if (includeHeader) Res.string.settings_section_appearance else null,
        items = listOf(
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_glass_accent_title),
                    subtitle = stringResource(Res.string.settings_glass_accent_subtitle),
                    trailingContent = {
                        AccentSwatches(
                            followSong = settings.followSongAccent,
                            palette = settings.palette,
                            onFollowSong = {
                                settingsViewModel.updateSettings { copy(followSongAccent = true) }
                            },
                            onPalette = { palette ->
                                settingsViewModel.updateSettings {
                                    copy(palette = palette, followSongAccent = false)
                                }
                            })
                    })
            },
            {
                val roomLight = LocalRoomLight.current
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_glass_room_light_title),
                    subtitle = stringResource(Res.string.settings_glass_room_light_subtitle),
                    checked = settings.roomLightEnabled,
                    onCheckedChange = { enabled ->
                        // Immediate feedback; App keeps it in sync with the saved setting.
                        roomLight.enabled = enabled
                        settingsViewModel.updateSettings { copy(roomLightEnabled = enabled) }
                    })
            },
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_glass_reduce_motion_title),
                    subtitle = stringResource(Res.string.settings_glass_reduce_motion_subtitle),
                    checked = settings.reduceMotion,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings { copy(reduceMotion = enabled) }
                    })
            })
    )
}

@Composable
private fun MillenniumPalette.displayLabel(): String = when (this) {
    MillenniumPalette.Blue -> stringResource(Res.string.settings_palette_blue)
    MillenniumPalette.Orange -> stringResource(Res.string.settings_palette_orange)
    MillenniumPalette.Lime -> stringResource(Res.string.settings_palette_lime)
}

/** Follow song, then the fixed accents (Ice / Aqua / Silver = the palette slots). */
@Composable
private fun AccentSwatches(
    followSong: Boolean,
    palette: MillenniumPalette,
    onFollowSong: () -> Unit,
    onPalette: (MillenniumPalette) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AccentSwatch(
            fill = Brush.sweepGradient(FollowSongSweep),
            label = stringResource(Res.string.settings_glass_accent_follow),
            selected = followSong,
            onClick = onFollowSong)
        MillenniumPalette.entries.forEach { entry ->
            val accent = remember(entry) { spotubeTokens(entry).accentFill }
            AccentSwatch(
                fill = SolidColor(accent),
                label = entry.displayLabel(),
                selected = !followSong && entry == palette,
                onClick = { onPalette(entry) })
        }
    }
}

@Composable
private fun AccentSwatch(
    fill: Brush,
    label: String,
    selected: Boolean,
    onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .keyboardFocusRing(CircleShape)
            .scale(if (hovered) 1.08f else 1f)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, if (selected) colors.onSurface else Color.Transparent, CircleShape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            })
}
