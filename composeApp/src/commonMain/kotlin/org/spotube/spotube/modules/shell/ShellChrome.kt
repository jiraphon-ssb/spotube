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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop

/** Shadow under floating glass (nav capsule, dock, menus). */
private val GlassShadowColor = Color.Black.copy(alpha = 0.55f)

/** Fill of floating glass chrome: the panel colour, see-through so the room light shows. */
@Composable
internal fun shellGlassFill(alpha: Float = 0.55f): Color = AppTheme.colors.surface.copy(alpha = alpha)

/**
 * Floating "glass": translucent fill, 1dp hairline and an optional soft shadow. No blur, on
 * purpose: a backdrop blur under every floating element costs a full pass per frame.
 * Put [keyboardFocusRing] before this so the ring isn't clipped.
 */
internal fun Modifier.shellGlass(
    shape: Shape,
    fill: Color,
    border: Color,
    elevation: Dp = 0.dp
): Modifier {
    val shadowed = if (elevation > 0.dp) {
        shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = GlassShadowColor,
            spotColor = GlassShadowColor
        )
    } else {
        this
    }
    return shadowed
        .clip(shape)
        .background(fill)
        .border(1.dp, border, shape)
}

/**
 * Makes this element a hit target over its whole area, so clicks and hover on the gaps between
 * its buttons don't fall through to the page underneath. Consumes nothing itself.
 */
internal fun Modifier.blockPointerPassThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
        }
    }
}

/**
 * Left space the shell keeps free for the window buttons. macOS draws its traffic lights in the
 * top-left corner of the content; elsewhere they are on the right (see [rightWindowControlsInset]).
 */
internal fun leftWindowControlsInset(): Dp =
    if (getPlatform().type == PlatformType.MacOS) 84.dp else 18.dp

/** Right space kept free for the Windows/Linux window buttons drawn by the pages' title bars. */
internal fun rightWindowControlsInset(): Dp {
    val platform = getPlatform()
    return if (platform.isDesktop() && platform.type != PlatformType.MacOS) 150.dp else 18.dp
}

/** "⌘K" on macOS, "Ctrl K" elsewhere. */
internal fun shortcutLabel(key: String): String =
    if (getPlatform().type == PlatformType.MacOS) "⌘$key" else "Ctrl $key"

/** A 40dp round glass button (back, avatar). Hover brightens it; keyboard focus draws a ring. */
@Composable
internal fun GlassRoundButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .size(size)
            .keyboardFocusRing(CircleShape)
            .shellGlass(CircleShape, fill = shellGlassFill(), border = colors.outline, elevation = 10.dp)
            .background(if (hovered) colors.hover else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Small accent count pill (queue length, active downloads). Decorative: callers label the count. */
@Composable
internal fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.accentFill)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = AppTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = colors.onAccentFill,
            maxLines = 1
        )
    }
}
