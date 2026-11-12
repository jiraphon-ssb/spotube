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

package org.spotube.spotube.core.history

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One listening session: a single track played from start (or resume) until it changed, stopped
 * or the app closed. Persisted as one JSON line; the recorder re-writes a session's line at
 * checkpoints, so readers keep the last line per [sessionId] ([dedupeSessions]).
 */
@Serializable
data class ListeningEvent(
    val sessionId: String,
    val startedAtMillis: Long,
    /** Wall-clock time spent in the PLAYING state. */
    val playedMs: Long,
    val trackId: String,
    val title: String,
    val artists: List<HistoryArtist> = emptyList(),
    val albumId: String? = null,
    val albumTitle: String? = null,
    val coverUrl: String? = null,
    /** From the track's album metadata; usually empty (most metadata plugins don't send genres). */
    val genres: List<String> = emptyList(),
    val durationMs: Long = 0L,
    val collection: HistoryCollection? = null
)

@Serializable
data class HistoryArtist(
    val name: String,
    val id: String? = null,
    val imageUrl: String? = null
)

/** The playlist / album the session was played from, as it stood when the session was saved. */
@Serializable
data class HistoryCollection(
    val id: String,
    val type: RecentType,
    val title: String = "",
    val coverUrl: String? = null,
    val progress: Float? = null,
    val remainingMs: Long? = null
)

/** Period shown on the Stats screen. History is capped at [HISTORY_MAX_AGE_DAYS], so no "year". */
enum class StatsRange(val days: Int) { Week(7), Month(30) }

data class DayMinutes(val date: LocalDate, val minutes: Int)

data class ArtistCount(val name: String, val id: String?, val imageUrl: String?, val plays: Int)

data class GenreShare(val name: String, val percent: Int)

data class Streaks(val current: Int, val best: Int)

/** Everything the Stats screen shows for one [range]. */
data class ListeningStats(
    val range: StatsRange,
    /** One entry per day of the range, oldest first, ending today. */
    val days: List<DayMinutes>,
    val totalMinutes: Int,
    val previousTotalMinutes: Int,
    /** Change vs the previous period of the same length, in percent; null when that period is empty. */
    val deltaPercent: Int?,
    /** Sessions long enough to count as a play ([countsAsPlay]). */
    val tracksPlayed: Int,
    /** Tracks played in the range that were never played before it (within the stored history). */
    val newTracks: Int,
    val topArtists: List<ArtistCount>,
    /** Empty when no played track carried genre data: hide the panel. */
    val genres: List<GenreShare>,
    val currentStreak: Int,
    val bestStreak: Int,
    /** False when nothing has been recorded at all: show the empty state. */
    val hasHistory: Boolean
)
