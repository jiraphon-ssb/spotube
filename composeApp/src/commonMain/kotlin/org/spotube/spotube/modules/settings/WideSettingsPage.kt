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

package org.spotube.spotube.modules.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalReduceMotion
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop
import org.spotube.spotube.modules.settings.components.GlassGroupShape
import org.spotube.spotube.modules.settings.components.GlassPageHeader
import org.spotube.spotube.modules.settings.components.glassSurface
import org.spotube.spotube.modules.settings.sections.appearanceSection
import org.spotube.spotube.modules.settings.sections.cacheSection
import org.spotube.spotube.modules.settings.sections.desktopSection
import org.spotube.spotube.modules.settings.sections.downloadsSection
import org.spotube.spotube.modules.settings.sections.jamSection
import org.spotube.spotube.modules.settings.sections.languageRegionSection
import org.spotube.spotube.modules.settings.sections.playbackSection
import org.spotube.spotube.modules.settings.sections.pluginsSection
import org.spotube.spotube.modules.settings.sections.shortcutsSection
import org.spotube.spotube.modules.settings.sections.supportSection
import org.spotube.spotube.modules.settings.sections.updatesSection
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_glass_nav_downloads_cache
import spotube.composeapp.generated.resources.settings_glass_nav_general
import spotube.composeapp.generated.resources.settings_glass_nav_shortcuts
import spotube.composeapp.generated.resources.settings_glass_sections
import spotube.composeapp.generated.resources.settings_glass_subtitle
import spotube.composeapp.generated.resources.settings_screen_title
import spotube.composeapp.generated.resources.settings_section_about
import spotube.composeapp.generated.resources.settings_section_appearance
import spotube.composeapp.generated.resources.settings_section_jam
import spotube.composeapp.generated.resources.settings_section_playback
import spotube.composeapp.generated.resources.settings_section_plugins
import spotube.composeapp.generated.resources.settings_section_support

private val SettingsNavWidth = 220.dp
private val SettingsNavItemShape = RoundedCornerShape(10.dp)

/** Entries of the wide settings page's section nav, in display order. */
internal enum class SettingsNavTab(val titleRes: StringResource) {
    General(Res.string.settings_glass_nav_general),
    Appearance(Res.string.settings_section_appearance),
    Playback(Res.string.settings_section_playback),
    Plugins(Res.string.settings_section_plugins),
    GroupJam(Res.string.settings_section_jam),
    DownloadsCache(Res.string.settings_glass_nav_downloads_cache),
    Shortcuts(Res.string.settings_glass_nav_shortcuts),
    Support(Res.string.settings_section_support),
    About(Res.string.settings_section_about),
}

/** Adds one settings section; every section adds exactly one lazy item. */
private typealias SectionEmitter = LazyListScope.() -> Unit

private class NavGroup(val tab: SettingsNavTab, val sections: List<SectionEmitter>)

/**
 * Settings in the wide (glass) shell (mockup `#v-set`): a page over the room light with a glass
 * section nav on the left and every settings group stacked in one scrolling column on the right.
 * Picking a section scrolls the column to it; scrolling highlights the section in view.
 */
@Composable
internal fun WideSettingsPage(settingsViewModel: SettingsViewModel) {
    val navigatorCommands: NavigationCommands = koinInject()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val isDesktop = remember { getPlatform().isDesktop() }
    val reduceMotion = LocalReduceMotion.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val settings = settingsState
    val groups: List<NavGroup> = if (settings == null) emptyList() else buildList<NavGroup> {
        add(NavGroup(SettingsNavTab.General, buildList<SectionEmitter> {
            add { languageRegionSection(settings, settingsViewModel) }
            if (isDesktop) add { desktopSection(settings, settingsViewModel) }
        }))
        add(NavGroup(SettingsNavTab.Appearance, listOf<SectionEmitter>(
            { appearanceSection(settings, settingsViewModel) })))
        add(NavGroup(SettingsNavTab.Playback, listOf<SectionEmitter>(
            {
                playbackSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    navigatorCommands = navigatorCommands,
                    requestLocalNetworkPermission = requestLocalNetworkPermission)
            })))
        add(NavGroup(SettingsNavTab.Plugins, listOf<SectionEmitter>({ pluginsSection(navigatorCommands) })))
        add(NavGroup(SettingsNavTab.GroupJam, listOf<SectionEmitter>({ jamSection(settings, navigatorCommands) })))
        add(NavGroup(SettingsNavTab.DownloadsCache, listOf<SectionEmitter>(
            { downloadsSection(settings, settingsViewModel) },
            { cacheSection(settings, settingsViewModel) })))
        if (isDesktop) {
            add(NavGroup(SettingsNavTab.Shortcuts, listOf<SectionEmitter>({ shortcutsSection() })))
        }
        add(NavGroup(SettingsNavTab.Support, listOf<SectionEmitter>({ supportSection(settings, settingsViewModel) })))
        add(NavGroup(SettingsNavTab.About, listOf<SectionEmitter>({ updatesSection(settings, settingsViewModel) })))
    }
    // First lazy item index of each group.
    val startIndices = groups.runningFold(0) { acc, group -> acc + group.sections.size }

    var clickedTab by remember { mutableStateOf<SettingsNavTab?>(null) }
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val atEnd by remember { derivedStateOf { !listState.canScrollForward } }
    val scrolledTab = groups.indices.lastOrNull { startIndices[it] <= firstVisible }?.let { groups[it].tab }
        ?: groups.firstOrNull()?.tab
    // The last groups can never reach the top of the column; keep the picked one lit at the end.
    val activeTab = if (atEnd && clickedTab != null) clickedTab else scrolledTab

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = LocalAppShellTopInset.current)
        ) {
            GlassPageHeader(
                title = stringResource(Res.string.settings_screen_title),
                subtitle = stringResource(Res.string.settings_glass_subtitle)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SettingsSectionNav(
                    tabs = groups.map { it.tab },
                    activeTab = activeTab,
                    onSelect = { tab ->
                        val index = groups.indexOfFirst { it.tab == tab }
                        if (index >= 0) {
                            clickedTab = tab
                            scope.launch {
                                if (reduceMotion) {
                                    listState.scrollToItem(startIndices[index])
                                } else {
                                    listState.animateScrollToItem(startIndices[index])
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .width(SettingsNavWidth)
                        .fillMaxHeight()
                )
                if (settings == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        MillenniumCircularProgress(size = 40.dp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = LocalAppShellBottomInset.current + 24.dp)
                    ) {
                        groups.forEach { group -> group.sections.forEach { emit -> emit(this) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionNav(
    tabs: List<SettingsNavTab>,
    activeTab: SettingsNavTab?,
    onSelect: (SettingsNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val sectionsLabel = stringResource(Res.string.settings_glass_sections)
    Box(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(colors, GlassGroupShape)
                .padding(8.dp)
                .selectableGroup()
                .semantics { contentDescription = sectionsLabel },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEach { tab ->
                SettingsNavItem(
                    label = stringResource(tab.titleRes),
                    selected = tab == activeTab,
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun SettingsNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Text(
        text = label,
        style = AppTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (selected || hovered) colors.onSurface else colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .keyboardFocusRing(SettingsNavItemShape)
            .background(
                color = when {
                    selected -> colors.onSurface.copy(alpha = 0.12f)
                    hovered -> colors.hover
                    else -> Color.Transparent
                },
                shape = SettingsNavItemShape
            )
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { this.selected = selected }
            .heightIn(min = 38.dp)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}
