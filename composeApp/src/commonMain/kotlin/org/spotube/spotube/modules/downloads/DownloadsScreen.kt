/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.component.cards.MediaArtwork
import org.spotube.spotube.core.ui.component.cards.MediaEmptyState
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.settings.components.GlassButton
import org.spotube.spotube.modules.settings.components.GlassListCard
import org.spotube.spotube.modules.settings.components.GlassPageHeader
import org.spotube.spotube.modules.settings.components.GlassProgressBar
import org.spotube.spotube.modules.settings.components.GlassStatusPill
import org.spotube.spotube.modules.settings.components.isWideShellLayout
import org.spotube.spotube.modules.shell.LocalAppShellTopInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import spotube.composeapp.generated.resources.*

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = koinViewModel(),
    headerInAppBar: Boolean = false,
    topContent: @Composable () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val wide = isWideShellLayout()
    Column(
        modifier
            .fillMaxSize()
            .then(if (headerInAppBar) Modifier.padding(top = LocalAppShellTopInset.current) else Modifier)
    ) {
        if (headerInAppBar && wide) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.TopCenter) {
                GlassPageHeader(
                    title = stringResource(Res.string.library_tab_downloads),
                    subtitle = downloadsSubtitle(state),
                    modifier = Modifier.widthIn(max = 960.dp)
                ) {
                    if (state.canClearCompleted) {
                        GlassButton(
                            text = stringResource(Res.string.downloads_clear_completed),
                            onClick = viewModel::clearCompleted
                        )
                    }
                }
            }
            topContent()
        } else if (headerInAppBar) {
            ApplicationMainBar(
                backButton = false,
                title = { DownloadsToolbarTitle() },
                subtitle = { DownloadsToolbarSummary(state) },
                actions = { DownloadsToolbarActions(state, viewModel::clearCompleted) }
            )
            topContent()
        }
        DownloadsContent(
            state,
            viewModel::cancel,
            viewModel::retry,
            viewModel::remove,
            viewModel::clearCompleted,
            Modifier.weight(1f)
                .then(if (headerInAppBar) Modifier.padding(horizontal = 12.dp) else Modifier),
            showHeader = !headerInAppBar
        )
    }
}

/** "N downloading · M done · Saved to …" for the glass page header. */
@Composable
private fun downloadsSubtitle(state: DownloadsUiState): String? = listOfNotNull(
    if (state.items.isNotEmpty()) {
        stringResource(Res.string.downloads_summary, state.activeCount, state.completedCount)
    } else null,
    if (state.folder.isNotBlank()) stringResource(Res.string.downloads_saved_to, state.folder) else null
).joinToString(" · ").ifBlank { null }

@Composable
internal fun DownloadsToolbarTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(
            Iconsax.IconsaxDirectboxReceive,
            null,
            tint = AppTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            stringResource(Res.string.library_tab_downloads),
            style = AppTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun DownloadsToolbarSummary(state: DownloadsUiState) {
    Column(Modifier.padding(start = 28.dp)) {
        if (state.items.isNotEmpty()) Text(
            stringResource(Res.string.downloads_summary, state.activeCount, state.completedCount),
            style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant
        )
        // Always visible (empty list included), so users know where their files end up.
        if (state.folder.isNotBlank()) Text(
            stringResource(Res.string.downloads_saved_to, state.folder),
            style = AppTheme.typography.bodySmall, color = MillenniumTheme.colors.muted,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun DownloadsToolbarActions(state: DownloadsUiState, onClearCompleted: () -> Unit) {
    if (state.canClearCompleted) MillenniumButton(
        onClick = onClearCompleted,
        style = MillenniumButtonStyle.Secondary
    ) {
        Text(
            stringResource(Res.string.downloads_clear_completed),
            style = AppTheme.typography.labelMedium
        )
    }
}

@Composable
internal fun DownloadsContent(
    state: DownloadsUiState,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val inset = LocalAppShellBottomInset.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize()) {
            if (showHeader) Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    DownloadsToolbarTitle()
                    DownloadsToolbarSummary(state)
                }
                DownloadsToolbarActions(state, onClearCompleted)
            }
            if (state.items.isEmpty()) MediaEmptyState(
                Iconsax.IconsaxDirectboxReceive,
                stringResource(Res.string.downloads_empty),
                Modifier.padding(bottom = inset)
            )
            else LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = if (showHeader) 0.dp else 16.dp,
                    bottom = inset + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { row ->
                    DownloadListRow(
                        row,
                        { onCancel(row.id) },
                        { onRetry(row.id) },
                        { onRemove(row.id) })
                }
            }
        }
    }
}

@Composable
internal fun DownloadListRow(
    item: DownloadRowUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AppTheme.colors
    val completed = item.status == DownloadStatus.Completed
    GlassListCard(
        title = item.title,
        subtitle = item.artists,
        leading = {
            MediaArtwork(item.artworkUrl, item.title, Modifier.size(52.dp), shape = RoundedCornerShape(10.dp))
        },
        below = {
            // Completed rows say it with the pill instead.
            if (!completed) {
                Text(
                    when (item.status) {
                        DownloadStatus.Downloading -> stringResource(
                            Res.string.downloads_status_downloading,
                            item.percent
                        )

                        DownloadStatus.Queued -> stringResource(Res.string.downloads_status_queued)
                        DownloadStatus.Completed -> stringResource(Res.string.downloads_status_completed)
                        DownloadStatus.Cancelled -> stringResource(Res.string.downloads_status_cancelled)
                        is DownloadStatus.Failed -> if (item.error != null) stringResource(
                            Res.string.downloads_status_failed_error,
                            item.error
                        ) else stringResource(Res.string.downloads_status_failed)
                    },
                    style = AppTheme.typography.labelSmall,
                    color = when (item.status) {
                        is DownloadStatus.Failed -> colors.error
                        else -> colors.onSurfaceVariant
                    },
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (item.status == DownloadStatus.Downloading) {
                GlassProgressBar(progress = item.progress, modifier = Modifier.padding(top = 8.dp))
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (completed) {
                    GlassStatusPill(stringResource(Res.string.downloads_ready_offline))
                }
                MillenniumIconButton(
                    onClick = when (item.action) {
                        DownloadRowAction.Cancel -> onCancel; DownloadRowAction.Retry -> onRetry; DownloadRowAction.Remove -> onRemove
                    },
                    modifier = Modifier.size(36.dp), style = MillenniumIconButtonStyle.Quiet
                ) {
                    MillenniumIcon(
                        when (item.action) {
                            DownloadRowAction.Cancel -> Iconsax.IconsaxCloseSquare; DownloadRowAction.Retry -> Iconsax.IconsaxRefreshRight; DownloadRowAction.Remove -> Iconsax.IconsaxTrash
                        },
                        stringResource(
                            when (item.action) {
                                DownloadRowAction.Cancel -> Res.string.downloads_action_cancel; DownloadRowAction.Retry -> Res.string.downloads_action_retry; DownloadRowAction.Remove -> Res.string.downloads_action_remove
                            }
                        )
                    )
                }
            }
        }
    )
}
