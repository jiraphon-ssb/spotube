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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackContent
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackContentViewModel
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackSheet
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContent
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.spotube.spotube.modules.shell.player_queue.QueueSheet

/** Top of the nav capsule / back / avatar buttons. */
private val ChromeTop = 16.dp

/** Space the floating top chrome takes: 16dp margin + 44dp capsule + 16dp breathing room. */
internal val WideShellTopInset = 76.dp

/** Toast baseline: just above the dock (mockup: bottom 112px). */
private val ToastBottom = 112.dp

/** Below this width the nav capsule's search field narrows so it clears the corner buttons. */
private val CompactNavWidth = 1040.dp

/**
 * The wide (desktop) layout: no sidebar, pages take the whole window over the [RoomLight], with
 * floating glass chrome on top:
 *
 * - top: back button (left, after the macOS traffic lights), [TopNavCapsule] (centre), the
 *   "other" menu avatar (right)
 * - bottom: [PlayerDock], which also morphs into the mini player
 * - overlays: queue drawer, sound sheet, command palette (Phase C slots), toasts
 *
 * Keyboard: ⌘K / Ctrl+K toggles the palette, ⌘, / Ctrl+, opens Settings, Escape closes the
 * topmost overlay (menu, palette, sound, queue, mini). The shell root holds focus so these work
 * even when nothing inside has been focused yet.
 */
@Composable
internal fun WideAppShell(
    navigator: Navigator,
    navigationState: NavigationState,
    viewModel: AppShellViewModel,
    queueViewModel: PlayerQueueContentViewModel,
    alternativeViewModel: AlternativeTrackContentViewModel,
    content: @Composable () -> Unit,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    libraryState: LibraryState = koinInject(),
    downloadsViewModel: DownloadsViewModel = koinViewModel()
) {
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val isQueueSheetVisible by queueViewModel.isQueueVisible.collectAsState()
    val isAlternativeVisible by alternativeViewModel.isAlternativeVisible.collectAsState()
    val queueOpen by viewModel.queueOpen.collectAsState()
    val soundOpen by viewModel.soundOpen.collectAsState()
    val paletteOpen by viewModel.paletteOpen.collectAsState()
    val mini by viewModel.mini.collectAsState()
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsState()
    val downloads by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    // Pages fade out while the dock is a mini card (read in the draw phase only).
    val pageFade = animateFloatAsState(
        targetValue = if (mini) 0f else 1f,
        animationSpec = tween(350),
        label = "shell_page_fade"
    )

    val leftInset = remember { leftWindowControlsInset() }
    val rightInset = remember { rightWindowControlsInset() }

    val backStack = navigationState.backStacks[navigationState.topLevelRoute]
    val canGoBack = (backStack?.size ?: 0) > 1 ||
        navigationState.topLevelRoute != navigationState.startRoute

    fun leaveMini() = viewModel.setMini(false)

    fun navigateTo(route: Routes) {
        menuOpen = false
        leaveMini()
        navigator.navigate(route)
    }

    fun openNowPlaying() {
        leaveMini()
        viewModel.closeQueue()
        viewModel.closeSound()
        viewModel.showFullPlayer()
    }

    fun onMenuSelect(item: OtherMenuItem) {
        when (item) {
            // About has no deep link into Settings yet; it lands on Settings.
            OtherMenuItem.Settings, OtherMenuItem.About -> navigateTo(Routes.Settings)
            OtherMenuItem.Downloads -> {
                libraryState.currentTab.value = LibraryTab.Downloads
                navigateTo(Routes.Library)
            }
            OtherMenuItem.LocalFiles -> {
                libraryState.currentTab.value = LibraryTab.LocalTracks
                navigateTo(Routes.Library)
            }
            OtherMenuItem.Plugins -> navigateTo(Routes.Plugins)
            OtherMenuItem.Devices -> navigateTo(Routes.Devices)
            OtherMenuItem.Jam -> navigateTo(Routes.Jam)
        }
    }

    val shellFocus = remember { FocusRequester() }
    var shellHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { shellFocus.requestFocus() } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { shellHasFocus = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val command = event.isMetaPressed || event.isCtrlPressed
                when {
                    command && event.key == Key.K -> {
                        menuOpen = false
                        viewModel.togglePalette()
                        true
                    }
                    command && event.key == Key.Comma -> {
                        viewModel.closePalette()
                        navigateTo(Routes.Settings)
                        true
                    }
                    event.key == Key.Escape -> if (menuOpen) {
                        menuOpen = false
                        true
                    } else {
                        viewModel.closeTopmostOverlay()
                    }
                    else -> false
                }
            }
            .focusRequester(shellFocus)
            .focusable()
            // When focus was dropped (e.g. the focused element left the screen), a click anywhere
            // gives it back to the shell so the shortcuts keep working.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!shellHasFocus) runCatching { shellFocus.requestFocus() }
                }
            }
    ) {
        RoomLight(currentCoverModel = currentEntry?.artworkModel, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pageFade.value }
        ) {
            content()
        }
        if (mini) {
            // The faded-out page must not take clicks or hover.
            Box(Modifier.fillMaxSize().blockPointerPassThrough())
        }

        if (!isFullPlayerVisible) {
            AnimatedVisibility(
                visible = !mini && canGoBack,
                modifier = Modifier.align(Alignment.TopStart).padding(start = leftInset, top = ChromeTop),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                ShellBackButton(onClick = {
                    menuOpen = false
                    navigator.pop()
                })
            }
            AnimatedVisibility(
                visible = !mini,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = ChromeTop),
                enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { -it },
                exit = fadeOut(tween(400)) + slideOutVertically(tween(500)) { -it }
            ) {
                TopNavCapsule(
                    currentRoute = navigationState.topLevelRoute,
                    onNavigate = ::navigateTo,
                    onOpenPalette = {
                        menuOpen = false
                        viewModel.openPalette()
                    },
                    modifier = Modifier.blockPointerPassThrough(),
                    compact = maxWidth < CompactNavWidth
                )
            }
            AnimatedVisibility(
                visible = !mini,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = rightInset, top = ChromeTop),
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400))
            ) {
                OtherMenuButton(onClick = { menuOpen = !menuOpen })
            }

            PlayerDock(
                mini = mini,
                onOpenNowPlaying = ::openNowPlaying,
                onOpenSound = viewModel::toggleSound,
                onOpenQueue = viewModel::toggleQueue,
                onToggleMini = viewModel::toggleMini,
                modifier = Modifier.fillMaxSize(),
                audioPlayerQueue = audioPlayerQueue
            )
        }

        if (menuOpen) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { menuOpen = false }
                )
            )
        }
        AnimatedVisibility(
            visible = menuOpen && !isFullPlayerVisible,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = rightInset),
            enter = fadeIn(tween(180)) + scaleIn(
                animationSpec = tween(250),
                initialScale = 0.98f,
                transformOrigin = TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(tween(150)) + scaleOut(
                animationSpec = tween(150),
                targetScale = 0.98f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
        ) {
            OtherMenuPanel(
                activeDownloads = downloads.activeCount,
                onSelect = ::onMenuSelect,
                modifier = Modifier.blockPointerPassThrough()
            )
        }

        // Full-window player sits under the queue / alternative-source sheets so those still
        // open on top of it.
        if (isFullPlayerVisible) {
            AppFullPlayer(
                onCollapse = viewModel::hideFullPlayer,
                onAlternativeSource = alternativeViewModel::toggleAlternativeVisibility
            )
        }

        // Existing sheets, still opened from the full player and other screens.
        if (isQueueSheetVisible) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { queueViewModel.setQueueVisibility(false) })
            )
        }
        if (isAlternativeVisible) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { alternativeViewModel.setAlternativeVisibility(false) })
            )
        }
        QueueSheet(
            isVisible = isQueueSheetVisible,
            onDismiss = { queueViewModel.setQueueVisibility(false) },
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerQueueContent(viewModel = queueViewModel, modifier = Modifier.fillMaxSize())
        }
        AlternativeTrackSheet(
            isVisible = isAlternativeVisible,
            onDismiss = { alternativeViewModel.setAlternativeVisibility(false) },
            modifier = Modifier.fillMaxSize()
        ) {
            AlternativeTrackContent(viewModel = alternativeViewModel, modifier = Modifier.fillMaxSize())
        }

        // ---- Phase C overlay slots ---------------------------------------------------------
        // The drawer and the sheet slide in from the right on a spring (no overshoot).
        AnimatedVisibility(
            visible = queueOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(160)) + slideInHorizontally(
                androidx.compose.animation.core.spring(dampingRatio = 0.92f, stiffness = 380f)
            ) { it / 3 },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(260)) { it / 3 }
        ) {
            QueueDrawerSlot(onClose = viewModel::closeQueue, queueViewModel = queueViewModel)
        }
        AnimatedVisibility(
            visible = soundOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(160)) + slideInHorizontally(
                androidx.compose.animation.core.spring(dampingRatio = 0.92f, stiffness = 380f)
            ) { it / 3 },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(260)) { it / 3 }
        ) {
            SoundSheetSlot(
                onClose = viewModel::closeSound,
                onOpenSettings = {
                    viewModel.closeSound()
                    viewModel.hideFullPlayer()
                    navigateTo(Routes.Settings)
                }
            )
        }
        AnimatedVisibility(
            visible = paletteOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            CommandPaletteSlot(
                onClose = viewModel::closePalette,
                onOpenSearch = {
                    viewModel.closePalette()
                    viewModel.hideFullPlayer()
                    navigateTo(Routes.Search)
                },
                onNavigate = { route ->
                    viewModel.closePalette()
                    viewModel.hideFullPlayer()
                    navigateTo(route)
                }
            )
        }

        ToastHost(
            toasts = viewModel.toasts,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ToastBottom)
        )
    }
}
