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

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * A playlist / album (or artist / track) the user listened to recently, for "Continue listening".
 *
 * @param lastPlayedAtMillis epoch millis when listening last stopped (session start + played time).
 * @param progress 0..1 through the collection when its position was known (the queue was the
 *   collection), else null.
 * @param remainingMinutes minutes left in the collection when known, else null.
 *
 * Liked songs are reported as [RecentType.Playlist] with id [SAVED_TRACKS_COLLECTION_ID].
 */
data class RecentCollection(
    val id: String,
    val type: RecentType,
    val title: String,
    val coverUrl: String?,
    val lastPlayedAtMillis: Long,
    val progress: Float?,
    val remainingMinutes: Int?
)

@Serializable
enum class RecentType { Playlist, Album, Artist, Track }

/** Read side of the local listening history. Registered in Koin as `single<ListeningHistoryRepository>`. */
interface ListeningHistoryRepository {
    /** Latest session per collection, newest first. */
    val recentCollections: Flow<List<RecentCollection>>

    /** Minutes listened this week (Monday–Sunday, local time). */
    val minutesThisWeek: Flow<Int>
}

/** [RecentCollection.id] used for the "Liked songs" collection. */
const val SAVED_TRACKS_COLLECTION_ID = "saved_tracks"
