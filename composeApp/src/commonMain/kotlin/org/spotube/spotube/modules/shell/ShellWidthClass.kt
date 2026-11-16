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

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the shell has, by window width. One rule for every page so they all change
 * layout at the same widths:
 * - [Narrow] (< 600): the top capsule goes away; the dock grows a tab row under the track row.
 * - [Medium] (600–899): icon-only capsule, single-row dock without the volume slider.
 * - [Full] (≥ 900): the full v8 layout (the capsule itself drops its labels below 1040).
 */
enum class ShellWidthClass {
    Narrow,
    Medium,
    Full;

    companion object {
        val MediumMinWidth: Dp = 600.dp
        val FullMinWidth: Dp = 900.dp

        fun of(width: Dp): ShellWidthClass = when {
            width >= FullMinWidth -> Full
            width >= MediumMinWidth -> Medium
            else -> Narrow
        }
    }
}

/** The current [ShellWidthClass]; provided by AppShell. */
val LocalShellWidthClass = staticCompositionLocalOf { ShellWidthClass.Full }

/** Below this width phones and tablets keep the compact (bottom bar + sheet) shell. */
val CompactShellMaxWidth: Dp = 840.dp

/**
 * Desktop windows always get the floating glass shell, laid out per [ShellWidthClass]: its
 * mouse-and-keyboard controls stay usable when narrow, unlike the compact shell's drag sheet.
 * Phones and tablets keep the compact shell until [CompactShellMaxWidth].
 */
fun usesGlassShell(isDesktop: Boolean, width: Dp): Boolean =
    isDesktop || width >= CompactShellMaxWidth

/** How the top navigation capsule is drawn. */
enum class CapsuleMode {
    /** Narrow: no capsule; the tab row in the dock navigates. */
    Hidden,

    /** Medium: icons, with the label only on the selected destination; search is an icon. */
    Icons,

    /** 900–1039: labels, with a shorter search field. */
    Compact,

    /** ≥ 1040: labels and the full-width search field. */
    Full
}

/** Below this width the capsule's search field narrows so it clears the corner buttons. */
val CapsuleFullMinWidth: Dp = 1040.dp

// Medium dock: one 68dp row, 12dp in from the window edges.
internal val MediumDockHeight: Dp = 68.dp
internal val MediumDockMargin: Dp = 12.dp
internal val MediumDockClearance: Dp = MediumDockMargin + MediumDockHeight + 16.dp

// Narrow dock: a 2dp progress line, the 56dp track row and the 54dp tab row, 8dp from the edges.
internal val NarrowDockMargin: Dp = 8.dp
internal val NarrowDockProgressHeight: Dp = 2.dp
internal val NarrowDockTrackRowHeight: Dp = 56.dp
internal val NarrowDockTabRowHeight: Dp = 54.dp
internal val NarrowDockHeight: Dp =
    NarrowDockProgressHeight + NarrowDockTrackRowHeight + NarrowDockTabRowHeight
internal val NarrowDockClearance: Dp = NarrowDockMargin + NarrowDockHeight + 12.dp

/** Narrow top band: 36dp buttons centred on the macOS traffic lights, plus breathing room. */
internal val NarrowShellTopInset: Dp = 56.dp

/**
 * What the glass shell draws at a window width.
 *
 * @property capsule how the top navigation capsule looks.
 * @property tabBar whether the dock carries the navigation tabs (Narrow).
 * @property miniPlayer whether the mini-player card is offered (it does not fit Narrow).
 * @property topInset space pages leave for the top chrome.
 * @property bottomInset space pages leave for the dock.
 */
data class ShellChromeSpec(
    val widthClass: ShellWidthClass,
    val capsule: CapsuleMode,
    val tabBar: Boolean,
    val miniPlayer: Boolean,
    val topInset: Dp,
    val bottomInset: Dp
)

fun shellChromeFor(width: Dp): ShellChromeSpec = when (val widthClass = ShellWidthClass.of(width)) {
    ShellWidthClass.Full -> ShellChromeSpec(
        widthClass = widthClass,
        capsule = if (width >= CapsuleFullMinWidth) CapsuleMode.Full else CapsuleMode.Compact,
        tabBar = false,
        miniPlayer = true,
        topInset = WideShellTopInset,
        bottomInset = PlayerDockClearance
    )
    ShellWidthClass.Medium -> ShellChromeSpec(
        widthClass = widthClass,
        capsule = CapsuleMode.Icons,
        tabBar = false,
        miniPlayer = true,
        topInset = WideShellTopInset,
        bottomInset = MediumDockClearance
    )
    ShellWidthClass.Narrow -> ShellChromeSpec(
        widthClass = widthClass,
        capsule = CapsuleMode.Hidden,
        tabBar = true,
        miniPlayer = false,
        topInset = NarrowShellTopInset,
        bottomInset = NarrowDockClearance
    )
}

/** The smallest desktop window: the narrow layout is designed down to this size. */
val MinimumWindowWidth: Dp = 360.dp
val MinimumWindowHeight: Dp = 600.dp
