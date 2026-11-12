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

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

// Pure aggregation over listening sessions. No I/O, no clocks: callers pass "today" and the zone,
// so every rule here is unit-tested (HistoryAggregationTest).

internal const val DAY_MS = 86_400_000L
private const val MINUTE_MS = 60_000L

/** Sessions older than this are dropped when the history is loaded. */
internal const val HISTORY_MAX_AGE_DAYS = 90

/** Hard cap on stored sessions (newest kept). ~20k lines is a few MB of JSON. */
internal const val HISTORY_MAX_EVENTS = 20_000

/** Sessions shorter than this are not stored at all (skips, scrubbing through a queue). */
internal const val MIN_RECORDED_MS = 10_000L

/** A session counts as a "play" after this much listening (or half the track, for short ones). */
internal const val PLAY_THRESHOLD_MS = 30_000L

/** A day keeps the streak alive with at least this much listening. */
internal const val STREAK_DAY_MS = 60_000L

internal fun ListeningEvent.countsAsPlay(): Boolean =
    playedMs >= PLAY_THRESHOLD_MS || (durationMs > 0 && playedMs * 2 >= durationMs)

internal fun localDateOf(epochMillis: Long, zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

/** The Monday of [date]'s week (ISO weeks: Monday–Sunday). */
internal fun weekStart(date: LocalDate): LocalDate =
    date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

private fun Long.toWholeMinutes(): Int = (this / MINUTE_MS).toInt()

/** Played milliseconds per local day (a session counts on the day it started). */
internal fun playedMsByDay(events: List<ListeningEvent>, zone: TimeZone): Map<LocalDate, Long> {
    val byDay = HashMap<LocalDate, Long>()
    events.forEach { event ->
        val day = localDateOf(event.startedAtMillis, zone)
        byDay[day] = (byDay[day] ?: 0L) + event.playedMs
    }
    return byDay
}

private fun playedMsBetween(
    byDay: Map<LocalDate, Long>,
    first: LocalDate,
    last: LocalDate
): Long = byDay.entries.sumOf { (day, ms) -> if (day >= first && day <= last) ms else 0L }

/** Minutes listened in the Monday–Sunday week containing [today], in [zone]. */
internal fun aggregateMinutesThisWeek(events: List<ListeningEvent>, today: LocalDate, zone: TimeZone): Int {
    val start = weekStart(today)
    val end = start.plus(6, DateTimeUnit.DAY)
    return playedMsBetween(playedMsByDay(events, zone), start, end).toWholeMinutes()
}

/** [days] entries ending at [lastDay], oldest first; days without listening are 0. */
internal fun dailyMinutes(
    events: List<ListeningEvent>,
    lastDay: LocalDate,
    days: Int,
    zone: TimeZone
): List<DayMinutes> {
    val byDay = playedMsByDay(events, zone)
    return (days - 1 downTo 0).map { back ->
        val date = lastDay.minus(back, DateTimeUnit.DAY)
        DayMinutes(date, (byDay[date] ?: 0L).toWholeMinutes())
    }
}

/**
 * Current and best run of consecutive days with at least [STREAK_DAY_MS] of listening. The
 * current run still counts while today has nothing yet (it ends at yesterday), and is 0 once a
 * whole day was missed.
 */
internal fun streaks(events: List<ListeningEvent>, today: LocalDate, zone: TimeZone): Streaks {
    val activeDays = playedMsByDay(events, zone).filterValues { it >= STREAK_DAY_MS }.keys
    if (activeDays.isEmpty()) return Streaks(0, 0)

    var current = 0
    var day = if (today in activeDays) today else today.minus(1, DateTimeUnit.DAY)
    while (day in activeDays) {
        current++
        day = day.minus(1, DateTimeUnit.DAY)
    }

    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (d in activeDays.sorted()) {
        val prev = previous
        run = if (prev != null && prev.daysUntil(d) == 1) run + 1 else 1
        if (run > best) best = run
        previous = d
    }
    return Streaks(current = current, best = maxOf(best, current))
}

/** Artists ranked by plays (every credited artist counts), ties by name. */
internal fun topArtists(events: List<ListeningEvent>, limit: Int = 5): List<ArtistCount> {
    data class Acc(val name: String, val id: String?, var imageUrl: String?, var plays: Int)

    val byKey = LinkedHashMap<String, Acc>()
    events.filter { it.countsAsPlay() }.forEach { event ->
        event.artists.distinctBy { it.key() }.forEach { artist ->
            val acc = byKey.getOrPut(artist.key()) { Acc(artist.name, artist.id, artist.imageUrl, 0) }
            acc.plays++
            if (acc.imageUrl == null) acc.imageUrl = artist.imageUrl
        }
    }
    return byKey.values
        .sortedWith(compareByDescending<Acc> { it.plays }.thenBy { it.name.lowercase() })
        .take(limit)
        .map { ArtistCount(it.name, it.id, it.imageUrl, it.plays) }
}

private fun HistoryArtist.key(): String = id?.takeIf { it.isNotBlank() } ?: "name:${name.trim().lowercase()}"

/** Share of genre tags across counted plays, largest first. Empty without genre data. */
internal fun genreShares(events: List<ListeningEvent>, limit: Int = 5): List<GenreShare> {
    val counts = LinkedHashMap<String, Int>()
    events.filter { it.countsAsPlay() }.forEach { event ->
        event.genres.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { genre ->
            counts[genre] = (counts[genre] ?: 0) + 1
        }
    }
    val total = counts.values.sum()
    if (total == 0) return emptyList()
    return counts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
        .take(limit)
        .map { GenreShare(it.key, (it.value * 100.0 / total).roundToInt()) }
}

/**
 * One event per session: the checkpoint with the most played time (played time only grows, and
 * checkpoints may be written out of order). Ties keep the later line. Ordered by start time.
 */
internal fun dedupeSessions(events: List<ListeningEvent>): List<ListeningEvent> {
    val bySession = LinkedHashMap<String, ListeningEvent>()
    events.forEach { event ->
        val existing = bySession[event.sessionId]
        if (existing == null || event.playedMs >= existing.playedMs) bySession[event.sessionId] = event
    }
    return bySession.values.sortedBy { it.startedAtMillis }
}

/** Drops sessions older than [maxAgeDays] and keeps at most the newest [maxEvents]; oldest first. */
internal fun trimHistory(
    events: List<ListeningEvent>,
    nowMillis: Long,
    maxAgeDays: Int = HISTORY_MAX_AGE_DAYS,
    maxEvents: Int = HISTORY_MAX_EVENTS
): List<ListeningEvent> {
    val cutoff = nowMillis - maxAgeDays * DAY_MS
    return events
        .filter { it.startedAtMillis >= cutoff }
        .sortedBy { it.startedAtMillis }
        .takeLast(maxEvents)
}

/** Latest session per collection, newest first. */
internal fun aggregateRecentCollections(events: List<ListeningEvent>, limit: Int = 20): List<RecentCollection> =
    events
        .filter { it.collection != null }
        .groupBy { event -> event.collection!!.let { it.type to it.id } }
        .values
        .map { sessions -> sessions.maxBy { it.startedAtMillis + it.playedMs } }
        .sortedByDescending { it.startedAtMillis + it.playedMs }
        .take(limit)
        .map { event ->
            val collection = event.collection!!
            RecentCollection(
                id = collection.id,
                type = collection.type,
                title = collection.title,
                coverUrl = collection.coverUrl,
                lastPlayedAtMillis = event.startedAtMillis + event.playedMs,
                progress = collection.progress?.coerceIn(0f, 1f),
                remainingMinutes = collection.remainingMs?.let { ((it + MINUTE_MS - 1) / MINUTE_MS).toInt() }
            )
        }

/** All Stats-screen numbers for [range] ending [today]. */
internal fun listeningStats(
    events: List<ListeningEvent>,
    range: StatsRange,
    today: LocalDate,
    zone: TimeZone
): ListeningStats {
    val first = today.minus(range.days - 1, DateTimeUnit.DAY)
    val previousLast = first.minus(1, DateTimeUnit.DAY)
    val previousFirst = first.minus(range.days, DateTimeUnit.DAY)

    val inRange = ArrayList<ListeningEvent>()
    val before = ArrayList<ListeningEvent>()
    events.forEach { event ->
        val day = localDateOf(event.startedAtMillis, zone)
        when {
            day < first -> before += event
            day <= today -> inRange += event
        }
    }
    val byDay = playedMsByDay(events, zone)
    val currentMs = inRange.sumOf { it.playedMs }
    val previousMs = playedMsBetween(byDay, previousFirst, previousLast)

    val played = inRange.filter { it.countsAsPlay() }
    val knownBefore = before.mapTo(HashSet()) { it.trackId }
    val streak = streaks(events, today, zone)

    return ListeningStats(
        range = range,
        days = dailyMinutes(events, today, range.days, zone),
        totalMinutes = currentMs.toWholeMinutes(),
        previousTotalMinutes = previousMs.toWholeMinutes(),
        deltaPercent = if (previousMs > 0) ((currentMs - previousMs) * 100.0 / previousMs).roundToInt() else null,
        tracksPlayed = played.size,
        newTracks = played.map { it.trackId }.distinct().count { it !in knownBefore },
        topArtists = topArtists(inRange),
        genres = genreShares(inRange),
        currentStreak = streak.current,
        bestStreak = streak.best,
        hasHistory = events.isNotEmpty()
    )
}
