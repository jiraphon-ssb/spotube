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


import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.min

data class CompactSheetUiState(
    val progress: Float,
    val showExpandedPlayer: Boolean,
    val expandedPlayerAlpha: Float,
    val floatingPlayerAlpha: Float,
    val bottomBarAlpha: Float
)

data class LyricsOverlayState(
    val isVisible: Boolean = false
)

data class CompactChromeUiState(val measuredBottomBarHeight: Dp? = null)

/**
 * A short confirmation shown above the player dock ("Added to queue", ...). [coverModel] is a
 * Coil model for the small thumbnail; [undo], when set, adds an Undo action.
 */
data class ShellToast(
    val message: String,
    val coverModel: Any? = null,
    val undo: (() -> Unit)? = null
)

class AppShellViewModel : ViewModel() {
    val sidebarMinWidth = 840.dp
    val bottomBarHeight = 80.dp
    // The background bleed overlaps the bottom bar and is not part of the peek height.
    val floatingPlayerHeight = FloatingPlayerContentHeight
    val largePlayerInset = 100.dp
    val floatingPlayerDismissProgress = 0.7f
    val expandedPlayerVisibilityThreshold = 0.001f

    private val lyricsOverlayVisible = MutableStateFlow(false)
    private val compactChrome = MutableStateFlow(CompactChromeUiState())
    val compactChromeUiState = compactChrome.asStateFlow()

    fun onBottomBarMeasured(height: Dp) {
        if (height > 0.dp) compactChrome.value = CompactChromeUiState(height)
    }

    val isLyricsOverlayVisible: StateFlow<Boolean> = lyricsOverlayVisible.asStateFlow()

    @Composable
    fun useSidebar(maxWidth: Dp? = null): Boolean {
        if (maxWidth != null) {
            return maxWidth >= sidebarMinWidth
        }
        return currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded
    }

    fun bottomOverlayInset(
        useSidebar: Boolean,
        navigationBarInset: Dp,
        chrome: CompactChromeUiState
    ): Dp =
        if (useSidebar) largePlayerInset + navigationBarInset
        else (chrome.measuredBottomBarHeight ?: (bottomBarHeight + navigationBarInset)) +
                floatingPlayerHeight

    fun sheetProgress(sheetOffsetPx: Float, sheetHeightPx: Float): Float =
        if (sheetHeightPx > 0f) {
            (sheetOffsetPx / sheetHeightPx).coerceIn(0f, 1f)
        } else {
            0f
        }

    fun floatingPlayerAlpha(progress: Float): Float {
        val normalized = min(progress / floatingPlayerDismissProgress, 1f)
        return 1f - normalized
    }

    fun compactSheetUiState(progress: Float): CompactSheetUiState {
        return CompactSheetUiState(
            progress = progress,
            showExpandedPlayer = progress > expandedPlayerVisibilityThreshold,
            expandedPlayerAlpha = progress,
            floatingPlayerAlpha = floatingPlayerAlpha(progress),
            bottomBarAlpha = 1f - progress
        )
    }

    fun showLyricsOverlay() {
        lyricsOverlayVisible.value = true
    }

    fun hideLyricsOverlay() {
        lyricsOverlayVisible.value = false
    }

    fun toggleLyricsOverlay() {
        lyricsOverlayVisible.update { !it }
    }

    private val fullPlayerVisible = MutableStateFlow(false)

    /** Desktop "now playing" view that covers the whole window (artwork + lyrics/queue). */
    val isFullPlayerVisible: StateFlow<Boolean> = fullPlayerVisible.asStateFlow()

    fun showFullPlayer() {
        fullPlayerVisible.value = true
    }

    fun hideFullPlayer() {
        fullPlayerVisible.value = false
    }

    // --- Wide-layout overlays -----------------------------------------------------------------

    private val queueOpenFlow = MutableStateFlow(false)
    private val soundOpenFlow = MutableStateFlow(false)
    private val paletteOpenFlow = MutableStateFlow(false)
    private val miniFlow = MutableStateFlow(false)

    /** The queue drawer (right edge). Mutually exclusive with the sound sheet. */
    val queueOpen: StateFlow<Boolean> = queueOpenFlow.asStateFlow()

    /** The sound & EQ sheet. Mutually exclusive with the queue drawer. */
    val soundOpen: StateFlow<Boolean> = soundOpenFlow.asStateFlow()

    /** The ⌘K command palette. */
    val paletteOpen: StateFlow<Boolean> = paletteOpenFlow.asStateFlow()

    /** Mini player mode: pages fade out and the dock shrinks to a card in the corner. */
    val mini: StateFlow<Boolean> = miniFlow.asStateFlow()

    fun openQueue() {
        soundOpenFlow.value = false
        queueOpenFlow.value = true
    }

    fun closeQueue() {
        queueOpenFlow.value = false
    }

    fun toggleQueue() {
        if (queueOpenFlow.value) closeQueue() else openQueue()
    }

    fun openSound() {
        queueOpenFlow.value = false
        soundOpenFlow.value = true
    }

    fun closeSound() {
        soundOpenFlow.value = false
    }

    fun toggleSound() {
        if (soundOpenFlow.value) closeSound() else openSound()
    }

    fun openPalette() {
        paletteOpenFlow.value = true
    }

    fun closePalette() {
        paletteOpenFlow.value = false
    }

    fun togglePalette() {
        paletteOpenFlow.update { !it }
    }

    fun setMini(enabled: Boolean) {
        if (enabled) {
            // The drawer and sheet belong to the full layout; don't leave them over the card.
            queueOpenFlow.value = false
            soundOpenFlow.value = false
        }
        miniFlow.value = enabled
    }

    fun toggleMini() = setMini(!miniFlow.value)

    /**
     * Closes the topmost shell overlay, in the order palette, sound, queue, mini.
     * Returns false when none was open (so Escape can fall through to the page).
     */
    fun closeTopmostOverlay(): Boolean = when {
        paletteOpenFlow.value -> { closePalette(); true }
        soundOpenFlow.value -> { closeSound(); true }
        queueOpenFlow.value -> { closeQueue(); true }
        miniFlow.value -> { setMini(false); true }
        else -> false
    }

    // --- Toasts -------------------------------------------------------------------------------

    private val toastFlow = MutableSharedFlow<ShellToast>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Toasts to show. Not replayed: a toast raised while no host is listening is dropped. */
    val toasts: SharedFlow<ShellToast> = toastFlow.asSharedFlow()

    fun showToast(toast: ShellToast) {
        toastFlow.tryEmit(toast)
    }

    // Explicit Unit: an implicit return type on an overload that calls its sibling overload can
    // trip the compiler's recursive-type check.
    fun showToast(message: String, coverModel: Any? = null, undo: (() -> Unit)? = null): Unit =
        showToast(ShellToast(message, coverModel, undo))
}
