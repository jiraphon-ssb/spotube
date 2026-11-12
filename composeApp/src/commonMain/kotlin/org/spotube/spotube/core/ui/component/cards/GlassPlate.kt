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

package org.spotube.spotube.core.ui.component.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellTopInset

/**
 * True inside the wide (floating glass) shell. There the room light is drawn behind the pages, so
 * pages paint no opaque background and pad their tops by [LocalAppShellTopInset].
 */
@Composable
fun isWideShell(): Boolean = LocalAppShellTopInset.current > 0.dp

/** Fill of a page "glass" plate: the panel colour, see-through so the room light shows. */
@Composable
fun pageGlassFill(alpha: Float = 0.5f): Color = AppTheme.colors.surface.copy(alpha = alpha)

/**
 * A page glass plate (cards, sheets, toolbars): translucent panel fill with a 1dp hairline.
 * No blur and no shadow, so the room light reads through and nothing costs a full pass.
 * Put [org.spotube.spotube.core.ui.component.keyboardFocusRing] before this when the plate is
 * clickable, so the ring isn't clipped.
 */
@Composable
fun Modifier.glassPlate(shape: Shape, fill: Color = pageGlassFill()): Modifier = this
    .clip(shape)
    .background(fill)
    .border(1.dp, AppTheme.colors.outline, shape)
