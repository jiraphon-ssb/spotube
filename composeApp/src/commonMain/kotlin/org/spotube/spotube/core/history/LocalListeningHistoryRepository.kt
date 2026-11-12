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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.spotube.spotube.core.paths.Paths
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.history_liked_songs
import kotlin.time.Clock

/**
 * Local listening history, stored as JSON lines in `<app data dir>/spotube_listening_history.jsonl`
 * (see [ListeningHistoryStorage]). Written by [ListeningHistoryRecorder]; read by Home ("Continue",
 * the week chip) through [ListeningHistoryRepository] and by the Stats screen through [stats].
 *
 * Loading compacts the file: checkpoint duplicates collapse to one line per session, sessions
 * older than [HISTORY_MAX_AGE_DAYS] are dropped and at most [HISTORY_MAX_EVENTS] are kept.
 */
class LocalListeningHistoryRepository(
    paths: Paths,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ListeningHistoryRepository {

    private val logger = Logger.withTag("ListeningHistory")
    private val storage = ListeningHistoryStorage(
        "${paths.getApplicationDataDirPath()}/$HISTORY_FILE_NAME".toPath()
    )
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private var linesSinceCompaction = 0

    private val _events = MutableStateFlow<List<ListeningEvent>>(emptyList())

    /** Every stored session (one per session id), roughly oldest first. Empty until loaded. */
    val events: StateFlow<List<ListeningEvent>> = _events.asStateFlow()

    private val isLoaded = MutableStateFlow(false)

    /** [events], but only once the file has been read (so screens don't flash "no history"). */
    private val loadedEvents: Flow<List<ListeningEvent>> =
        combine(isLoaded, _events) { ready, list -> if (ready) list else null }.filterNotNull()

    init {
        scope.launch {
            mutex.withLock {
                val raw = runCatching { storage.readAll() }
                    .onFailure { logger.w(it) { "Could not read listening history" } }
                    .getOrDefault(emptyList())
                val compact = trimHistory(dedupeSessions(raw), nowMillis())
                if (compact.size != raw.size) {
                    runCatching { storage.rewrite(compact) }
                        .onFailure { logger.w(it) { "Could not compact listening history" } }
                }
                _events.value = compact
            }
            isLoaded.value = true
            loaded.complete(Unit)
        }
    }

    /**
     * Saves [event], replacing an earlier (shorter) checkpoint of the same session. Appends one line; the
     * file is compacted every [COMPACT_EVERY_LINES] appends.
     */
    suspend fun record(event: ListeningEvent) {
        loaded.await()
        mutex.withLock {
            // Checkpoints of a session can arrive out of order; never replace a longer one.
            val existing = _events.value.firstOrNull { it.sessionId == event.sessionId }
            if (existing != null && existing.playedMs > event.playedMs) return
            runCatching { storage.append(event) }
                .onFailure { logger.w(it) { "Could not save listening session" } }
            val updated = _events.value.filterNot { it.sessionId == event.sessionId } + event
            linesSinceCompaction++
            if (linesSinceCompaction >= COMPACT_EVERY_LINES) {
                linesSinceCompaction = 0
                val compact = trimHistory(updated, nowMillis())
                runCatching { storage.rewrite(compact) }
                    .onFailure { logger.w(it) { "Could not compact listening history" } }
                _events.value = compact
            } else {
                _events.value = updated
            }
        }
    }

    /** Ticks once a minute so date-relative values roll over at midnight / on Monday. */
    private val minuteTicker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    private fun today(): LocalDate = localDateOf(nowMillis(), timeZone())

    override val recentCollections: Flow<List<RecentCollection>> = loadedEvents
        .map { list ->
            val likedTitle = getString(Res.string.history_liked_songs)
            aggregateRecentCollections(list)
                .map { if (it.id == SAVED_TRACKS_COLLECTION_ID) it.copy(title = likedTitle) else it }
                // A collection whose name could not be looked up is not worth showing.
                .filter { it.title.isNotBlank() }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override val minutesThisWeek: Flow<Int> = combine(loadedEvents, minuteTicker) { list, _ ->
        aggregateMinutesThisWeek(list, today(), timeZone())
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    /** Stats-screen numbers for [range], refreshed on every new session and at day change. */
    fun stats(range: StatsRange): Flow<ListeningStats> = combine(loadedEvents, minuteTicker) { list, _ ->
        listeningStats(list, range, today(), timeZone())
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private companion object {
        const val HISTORY_FILE_NAME = "spotube_listening_history.jsonl"
        const val COMPACT_EVERY_LINES = 2_000
    }
}
