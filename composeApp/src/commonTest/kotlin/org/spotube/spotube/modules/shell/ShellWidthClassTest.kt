package org.spotube.spotube.modules.shell

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellWidthClassTest {

    @Test
    fun narrowBelow600() {
        assertEquals(ShellWidthClass.Narrow, ShellWidthClass.of(320.dp))
        assertEquals(ShellWidthClass.Narrow, ShellWidthClass.of(360.dp))
        assertEquals(ShellWidthClass.Narrow, ShellWidthClass.of(599.dp))
    }

    @Test
    fun mediumFrom600To899() {
        assertEquals(ShellWidthClass.Medium, ShellWidthClass.of(600.dp))
        assertEquals(ShellWidthClass.Medium, ShellWidthClass.of(840.dp))
        assertEquals(ShellWidthClass.Medium, ShellWidthClass.of(899.dp))
    }

    @Test
    fun fullFrom900() {
        assertEquals(ShellWidthClass.Full, ShellWidthClass.of(900.dp))
        assertEquals(ShellWidthClass.Full, ShellWidthClass.of(1040.dp))
        assertEquals(ShellWidthClass.Full, ShellWidthClass.of(2560.dp))
    }

    @Test
    fun fractionalWidthJustUnderABoundaryStaysInTheLowerClass() {
        assertEquals(ShellWidthClass.Narrow, ShellWidthClass.of(599.5.dp))
        assertEquals(ShellWidthClass.Medium, ShellWidthClass.of(899.9.dp))
    }

    @Test
    fun desktopUsesTheGlassShellAtEveryWidth() {
        assertTrue(usesGlassShell(isDesktop = true, width = 360.dp))
        assertTrue(usesGlassShell(isDesktop = true, width = 600.dp))
        assertTrue(usesGlassShell(isDesktop = true, width = 1440.dp))
    }

    @Test
    fun phonesAndTabletsKeepTheCompactShellBelow840() {
        assertFalse(usesGlassShell(isDesktop = false, width = 400.dp))
        assertFalse(usesGlassShell(isDesktop = false, width = 839.dp))
        assertTrue(usesGlassShell(isDesktop = false, width = 840.dp))
    }

    @Test
    fun fullWidthShowsTheLabelledCapsuleAndTheMiniPlayer() {
        val chrome = shellChromeFor(1280.dp)
        assertEquals(CapsuleMode.Full, chrome.capsule)
        assertFalse(chrome.tabBar)
        assertTrue(chrome.miniPlayer)
        assertEquals(WideShellTopInset, chrome.topInset)
        assertEquals(PlayerDockClearance, chrome.bottomInset)
    }

    @Test
    fun capsuleSearchNarrowsBelow1040() {
        assertEquals(CapsuleMode.Full, shellChromeFor(1040.dp).capsule)
        assertEquals(CapsuleMode.Compact, shellChromeFor(1039.dp).capsule)
        assertEquals(CapsuleMode.Compact, shellChromeFor(900.dp).capsule)
    }

    @Test
    fun mediumUsesIconCapsuleAndTheSingleRowDock() {
        val chrome = shellChromeFor(700.dp)
        assertEquals(CapsuleMode.Icons, chrome.capsule)
        assertFalse(chrome.tabBar)
        assertTrue(chrome.miniPlayer)
        assertEquals(WideShellTopInset, chrome.topInset)
        assertEquals(MediumDockClearance, chrome.bottomInset)
    }

    @Test
    fun narrowMovesNavigationIntoTheDockAndDropsTheMiniPlayer() {
        val chrome = shellChromeFor(360.dp)
        assertEquals(CapsuleMode.Hidden, chrome.capsule)
        assertTrue(chrome.tabBar)
        assertFalse(chrome.miniPlayer)
        assertEquals(NarrowShellTopInset, chrome.topInset)
        assertEquals(NarrowDockClearance, chrome.bottomInset)
    }

    @Test
    fun pagesClearTheTallerNarrowDock() {
        // Track row + tab row is taller than the single-row dock, so pages need more room.
        assertTrue(NarrowDockClearance > MediumDockClearance)
        assertTrue(NarrowDockClearance >= NarrowDockHeight)
    }

    @Test
    fun minimumWindowIs360By600() {
        assertEquals(360.dp, MinimumWindowWidth)
        assertEquals(600.dp, MinimumWindowHeight)
    }
}
