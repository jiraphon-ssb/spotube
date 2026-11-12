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

package org.spotube.spotube.modules.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppColors
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellTopInset

// Glass building blocks shared by the restyled utility pages (Settings, Downloads, Plugins,
// Devices, Jam): translucent white plates with a hairline border, so the shell's room light shows
// through. Mockup v8: `.glass`, `.lcard`, `.sgroup`, `.btn2`, `.pill2`, `.prog`, `.phd`.

/** Rounded corners of a list card (`.lcard`). */
val GlassCardShape: Shape = RoundedCornerShape(16.dp)

/** Rounded corners of a settings group / stats panel (`.sgroup`, `.panel`). */
val GlassGroupShape: Shape = RoundedCornerShape(20.dp)

/** Mockup `.pill2` green: the success tone of the Glass Night tokens. */
private val ReadyGreen = Color(0xFF8FE3B5)

/** True inside the wide (floating glass) shell, where pages draw over the room light. */
@Composable
fun isWideShellLayout(): Boolean = LocalAppShellTopInset.current > 0.dp

/** A translucent plate with a hairline border. */
fun Modifier.glassSurface(colors: AppColors, shape: Shape = GlassCardShape): Modifier =
    this
        .clip(shape)
        .background(colors.surfaceContainerHigh)
        .border(1.dp, colors.outline, shape)

/** Page title block (`.phd`): big title, muted line under it, actions on the right. */
@Composable
fun GlassPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.headlineLarge.copy(letterSpacing = (-0.8).sp),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/**
 * One glass list card (`.lcard`): leading art/icon, title + muted line (+ optional extra content
 * such as a progress bar), trailing control. Clickable when [onClick] is set.
 */
@Composable
fun GlassListCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    below: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = onClick != null && enabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.keyboardFocusRing(GlassCardShape) else Modifier)
            .clip(GlassCardShape)
            .background(if (interactive && hovered) colors.surfaceContainerHighest else colors.surfaceContainerHigh)
            .border(1.dp, colors.outline, GlassCardShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .hoverable(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled,
                            onClickLabel = onClickLabel,
                            role = Role.Button,
                            onClick = onClick
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (leading != null) {
            Box(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) { leading() }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.5f)
        ) {
            Text(
                text = title,
                style = AppTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodySmall,
                    color = subtitleColor ?: colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            below?.invoke(this)
        }
        trailing?.invoke()
    }
}

/** Square tile for a leading icon in a [GlassListCard] (`.lcard .ico`). */
@Composable
fun GlassIconTile(imageVector: ImageVector, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = imageVector,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Thin accent progress bar (`.prog`). */
@Composable
fun GlassProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primary)
        )
    }
}

/** Small green status pill (`.pill2`), e.g. "Ready offline". */
@Composable
fun GlassStatusPill(text: String, modifier: Modifier = Modifier, color: Color = ReadyGreen) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .heightIn(min = 24.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Compact secondary button (`.btn2`): 34dp, 10dp corners. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    danger: Boolean = false
) {
    MillenniumButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = MillenniumButtonStyle.Secondary,
        foreground = if (danger) AppTheme.colors.error else null,
        shape = RoundedCornerShape(10.dp),
        padding = PaddingValues(horizontal = 14.dp),
        minSize = DpSize(0.dp, 34.dp)
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** A grouped glass card (`.sgroup`) with an optional heading inside it. */
@Composable
fun GlassGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(colors, GlassGroupShape)
            .padding(vertical = 6.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 2.dp)
                    .semantics { heading() }
            )
        }
        content()
    }
}

/** Hairline between rows of a [GlassGroup] (`.srow` border-top). */
@Composable
fun GlassRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(AppTheme.colors.outline)
    )
}
