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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.shell_toast_undo

/** How long a toast stays up. Hovering it pauses the countdown. */
private const val TOAST_MILLIS = 3_200L
private const val TOAST_TICK_MILLIS = 100L

private val ToastShape = RoundedCornerShape(50)

/**
 * Shows [toasts] one at a time as a glass pill (place it bottom-centre, above the dock). A new
 * toast replaces the current one. Each auto-dismisses after 3.2s; the timer pauses while the
 * pointer is over it so the Undo action can be reached. Screen readers hear it politely.
 */
@Composable
fun ToastHost(toasts: Flow<ShellToast>, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<ShellToast?>(null) }
    var visible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered = interactionSource.collectIsHoveredAsState()

    LaunchedEffect(toasts) {
        toasts.collectLatest { toast ->
            current = toast
            visible = true
            var remaining = TOAST_MILLIS
            while (remaining > 0) {
                delay(TOAST_TICK_MILLIS)
                if (!hovered.value) remaining -= TOAST_TICK_MILLIS
            }
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(250)) + slideInVertically(tween(400)) { it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 2 }
    ) {
        current?.let { toast ->
            ToastPill(
                toast = toast,
                interactionSource = interactionSource,
                onUndo = {
                    toast.undo?.invoke()
                    visible = false
                }
            )
        }
    }
}

@Composable
private fun ToastPill(
    toast: ShellToast,
    interactionSource: MutableInteractionSource,
    onUndo: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .shellGlass(ToastShape, fill = shellGlassFill(alpha = 0.9f), border = colors.outline, elevation = 18.dp)
            .blockPointerPassThrough()
            .hoverable(interactionSource)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(
                start = if (toast.coverModel != null) 10.dp else 16.dp,
                end = if (toast.undo != null) 8.dp else 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (toast.coverModel != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = toast.coverModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = toast.message,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (toast.undo != null) {
            val shape = RoundedCornerShape(50)
            Text(
                text = stringResource(Res.string.shell_toast_undo),
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
                maxLines = 1,
                modifier = Modifier
                    .keyboardFocusRing(shape)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onUndo)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
