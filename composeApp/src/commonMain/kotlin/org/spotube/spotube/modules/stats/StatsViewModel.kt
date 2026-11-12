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

package org.spotube.spotube.modules.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.spotube.spotube.core.history.ListeningStats
import org.spotube.spotube.core.history.LocalListeningHistoryRepository
import org.spotube.spotube.core.history.StatsRange
import kotlin.time.Clock

/** Listening stats for the selected [StatsRange], recomputed as sessions are recorded. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val repository: LocalListeningHistoryRepository
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.Week)
    val selectedRange: StateFlow<StatsRange> = range.asStateFlow()

    /** Null until the history has been read once. */
    val stats: StateFlow<ListeningStats?> = range
        .flatMapLatest { repository.stats(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectRange(newRange: StatsRange) {
        range.value = newRange
    }

    /** Local date now; the chart marks this day. */
    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
