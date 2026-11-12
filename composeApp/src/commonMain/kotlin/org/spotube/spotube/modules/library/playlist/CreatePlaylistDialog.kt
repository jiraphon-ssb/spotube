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

package org.spotube.spotube.modules.library.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.spotube.core.ui.component.keyboardFocusRing
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.shell.LocalShellActions
import org.spotube.spotube.resources.glyphs.Glyphs
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.library_playlist_form_create
import spotube.composeapp.generated.resources.library_playlist_form_create_title
import spotube.composeapp.generated.resources.library_playlist_form_description_label
import spotube.composeapp.generated.resources.library_playlist_form_description_placeholder
import spotube.composeapp.generated.resources.library_playlist_form_name_label
import spotube.composeapp.generated.resources.pages_library_cover_colour
import spotube.composeapp.generated.resources.pages_library_cover_colour_option
import spotube.composeapp.generated.resources.pages_library_create_failed
import spotube.composeapp.generated.resources.pages_library_created
import spotube.composeapp.generated.resources.pages_library_default_playlist_name

/** Cover colours offered in the dialog (the mockup's five). */
private val CoverColours = listOf(
    Color(0xFF9CC2FF),
    Color(0xFF6FE3D6),
    Color(0xFFE8A3C6),
    Color(0xFFEBCB78),
    Color(0xFFB59BE8)
)

/**
 * The glass "Create playlist" dialog: name, optional description and a cover colour. Creates the
 * playlist through [LibraryRepository.createPlaylist] (the same path as the library's playlist
 * form); the saved-playlist list reloads by itself when the new id lands.
 *
 * The colour only tints the preview here: the plugin API takes a cover *image*, and there is no
 * image encoder in common code yet.
 * TODO(art): render the colour into a small JPEG and pass it as `imageBase64`.
 */
@Composable
internal fun CreatePlaylistDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    repository: LibraryRepository = koinInject()
) {
    val colors = AppTheme.colors
    val shellActions = LocalShellActions.current
    val scope = rememberCoroutineScope()
    val name = rememberTextFieldState()
    val description = rememberTextFieldState()
    var colourIndex by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val title = stringResource(Res.string.library_playlist_form_create_title)
    val defaultName = stringResource(Res.string.pages_library_default_playlist_name)
    val failedMessage = stringResource(Res.string.pages_library_create_failed)

    LaunchedEffect(visible) {
        if (visible) {
            failed = false
            runCatching { nameFocus.requestFocus() }
        }
    }

    fun close() {
        if (submitting) return
        name.clearText()
        description.clearText()
        colourIndex = 0
        failed = false
        onDismiss()
    }

    fun create() {
        if (submitting) return
        val finalName = name.text.toString().trim().ifBlank { defaultName }
        val finalDescription = description.text.toString().trim().ifBlank { null }
        submitting = true
        failed = false
        scope.launch {
            try {
                val created = repository.createPlaylist(
                    name = finalName,
                    description = finalDescription,
                    isPublic = true,
                    isCollaborating = false,
                    imageBase64 = "",
                    trackIds = emptyList()
                )
                submitting = false
                if (created == null) {
                    failed = true
                } else {
                    shellActions.showToast(getString(Res.string.pages_library_created, finalName))
                    close()
                }
            } catch (e: CancellationException) {
                submitting = false
                throw e
            } catch (e: Exception) {
                submitting = false
                failed = true
            }
        }
    }

    MillenniumDialog(
        visible = visible,
        onDismissRequest = ::close,
        paneTitle = title,
        panelBackground = SolidColor(Color(0xFF121821).copy(alpha = 0.94f)),
        shape = RoundedCornerShape(24.dp),
        panelBorderColor = colors.outline,
        panelShadowElevation = 24.dp,
        panelGlowColor = Color.Transparent,
        panelMaxWidth = 520.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = title,
                style = AppTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                val colour = CoverColours[colourIndex]
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(colour, lerp(colour, Color(0xFF0B1420), 0.7f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MillenniumIcon(
                        imageVector = Glyphs.Playlists,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        size = 40.dp
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(Res.string.library_playlist_form_name_label))
                    MillenniumTextField(
                        state = name,
                        placeholder = defaultName,
                        minimumHeight = 42.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                    )
                    FieldLabel(
                        stringResource(Res.string.library_playlist_form_description_label),
                        Modifier.padding(top = 12.dp)
                    )
                    MillenniumTextField(
                        state = description,
                        placeholder = stringResource(Res.string.library_playlist_form_description_placeholder),
                        minimumHeight = 42.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FieldLabel(stringResource(Res.string.pages_library_cover_colour), Modifier.padding(top = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoverColours.forEachIndexed { index, option ->
                            val isSelected = index == colourIndex
                            val optionLabel = stringResource(Res.string.pages_library_cover_colour_option, index + 1)
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .keyboardFocusRing(CircleShape)
                                    .clip(CircleShape)
                                    .background(option)
                                    .border(2.dp, if (isSelected) colors.onSurface else Color.Transparent, CircleShape)
                                    .clickable(role = Role.RadioButton) { colourIndex = index }
                                    .semantics {
                                        selected = isSelected
                                        contentDescription = optionLabel
                                    }
                            )
                        }
                    }
                }
            }
            if (failed) {
                Text(
                    text = failedMessage,
                    style = AppTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                MillenniumButton(onClick = ::close, style = MillenniumButtonStyle.Secondary, enabled = !submitting) {
                    Text(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(onClick = ::create, style = MillenniumButtonStyle.Primary, loading = submitting) {
                    Text(stringResource(Res.string.library_playlist_form_create))
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier.padding(bottom = 6.dp)
    )
}
