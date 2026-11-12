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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import org.spotube.millennium.composables.MillenniumTopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

@Composable
actual fun ApplicationMainBar(
    title: @Composable (() -> Unit),
    subtitle: @Composable () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    backButton: Boolean,
    transparent: Boolean) {
    // Wide (floating glass) shell: no panel band, and the shell's back button replaces the bar's.
    val wide = LocalAppShellTopInset.current > 0.dp
    val clear = transparent || wide
    return MillenniumTopAppBar(
        title = title,
        subtitle = subtitle,
        actions = actions,
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            if (backButton && !wide) {
                ApplicationBackButton()
            }
        },
        background = if (clear) SolidColor(Color.Transparent) else null,
        separatorColor = if (clear) Color.Transparent else null
    )
}

@Composable
actual fun WindowChromeBand(modifier: Modifier) {
    val height = LocalAppShellTopInset.current
    if (height <= 0.dp) return
    Spacer(modifier.fillMaxWidth().height(height))
}
