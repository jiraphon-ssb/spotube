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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Draws a text-coloured ring just outside this element while it holds keyboard focus (Tab /
 * arrow keys). Pointer presses also focus a clickable, but they switch the input mode to touch,
 * so a mouse click never shows the ring.
 *
 * Order matters. `onFocusChanged` observes the focus target that comes *after* it, and drawing
 * outside the bounds only survives when nothing before it clips. So put this modifier
 * **before** `clip`, `clickable`/`focusable` (and before `graphicsLayer` if the ring should not
 * scale):
 *
 * ```kotlin
 * Modifier
 *     .keyboardFocusRing(shape)
 *     .clip(shape)
 *     .background(colors.surfaceContainerHigh)
 *     .clickable(onClick = onClick)
 * ```
 *
 * The ring only reflects focus of that element itself, not of a focused child inside it.
 *
 * @param shape outline the ring follows, normally the element's own clip shape.
 * @param width stroke width of the ring.
 */
fun Modifier.keyboardFocusRing(shape: Shape, width: Dp = 2.dp): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val inputModeManager = LocalInputModeManager.current
    val color = AppTheme.colors.onSurface
    this
        .onFocusChanged { focused = it.isFocused }
        .drawWithContent {
            drawContent()
            // inputMode is snapshot state, so switching to the mouse while focused hides the ring.
            if (focused && inputModeManager.inputMode == InputMode.Keyboard) {
                val stroke = width.toPx()
                // Stroke is centred on the path: push the path out by half a stroke plus a 1dp gap
                // so the whole ring sits outside the element and never covers its edge.
                val offset = stroke / 2f + 1.dp.toPx()
                val outline = shape.createOutline(
                    Size(size.width + offset * 2f, size.height + offset * 2f),
                    layoutDirection,
                    this
                )
                translate(left = -offset, top = -offset) {
                    drawOutline(outline = outline, color = color, style = Stroke(width = stroke))
                }
            }
        }
}
