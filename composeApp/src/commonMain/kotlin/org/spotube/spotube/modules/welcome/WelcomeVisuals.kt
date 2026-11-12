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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.modules.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.TextUnit
import org.spotube.spotube.core.ui.theming.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.modules.shell.SpotubeWordmark
import spotube.composeapp.generated.resources.*

/**
 * Reapplies the Millennium theme for the onboarding pages using the app's active configuration,
 * so onboarding matches the palette and appearance used by the rest of the app.
 */
@Composable
fun WelcomeTheme(content: @Composable () -> Unit) {
    val dark = AppTheme.colors.surface.luminance() < 0.5f
    MillenniumTheme(
        dark = dark,
        config = MillenniumTheme.config,
        content = content)
}

@Composable
fun WelcomeBackground(modifier: Modifier = Modifier) {
    val scheme = AppTheme.colors
    Box(
        // Flat: both former gradient stops resolve to the same panel colour anyway.
        modifier.fillMaxSize().background(scheme.surface)
    )
}

@Composable
fun WelcomeWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = AppTheme.typography.displaySmall.fontSize
) {
    SpotubeWordmark(
        modifier = modifier,
        fontSize = fontSize,
        text = stringResource(Res.string.welcome_app_name))
}
