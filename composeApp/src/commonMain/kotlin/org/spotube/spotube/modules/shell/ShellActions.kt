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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Shell-level actions any screen can trigger (open the queue drawer, show a toast, ...), so pages
 * don't need a reference to [AppShellViewModel].
 */
@Stable
interface ShellActions {
    fun openQueue()
    fun openSound()
    fun openPalette()
    fun toggleMini()
    fun showToast(message: String, coverModel: Any? = null, undo: (() -> Unit)? = null)
}

/** Inert actions, used outside the shell (previews, tests). */
private object NoOpShellActions : ShellActions {
    override fun openQueue() {}
    override fun openSound() {}
    override fun openPalette() {}
    override fun toggleMini() {}
    override fun showToast(message: String, coverModel: Any?, undo: (() -> Unit)?) {}
}

val LocalShellActions = staticCompositionLocalOf<ShellActions> { NoOpShellActions }

/** [ShellActions] backed by the shell's view model. */
internal class ViewModelShellActions(private val viewModel: AppShellViewModel) : ShellActions {
    override fun openQueue() = viewModel.openQueue()
    override fun openSound() = viewModel.openSound()
    override fun openPalette() = viewModel.openPalette()
    override fun toggleMini() = viewModel.toggleMini()
    override fun showToast(message: String, coverModel: Any?, undo: (() -> Unit)?) =
        viewModel.showToast(message, coverModel, undo)
}
