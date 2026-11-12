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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryAggregationTest {

    private val bangkok = TimeZone.of("Asia/Bangkok") // UTC+7, no DST
    private val utc = TimeZone.UTC

    private var nextId = 0

    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0): Long =
        LocalDateTime(y, m, d, h, min).toInstant(zone).toEpochMilliseconds()

    private fun event(
        startedAt: Long,
        playedMs: Long = 120_000,
        trackId: String = "t${nextId}",
        artists: List<HistoryArtist> = listOf(HistoryArtist(name = "Artist A", id = "a")),
        genres: List<String> = emptyList(),
        durationMs: Long = 200_000,
        collection: HistoryCollection? = null,
        sessionId: String = "s${nextId++}"
    ) = ListeningEvent(
        sessionId = sessionId,
        startedAtMillis = startedAt,
        playedMs = playedMs,
        trackId = trackId,
        title = trackId,
        artists = artists,
        genres = genres,
        durationMs = durationMs,
        collection = collection
    )

    // ---- week bucketing ------------------------------------------------------------------

    @Test
    fun weekStartIsMonday() {
        // 2026-11-09 is a Monday, 2026-11-15 a Sunday.
        assertEquals(LocalDate(2026, 11, 9), weekStart(LocalDate(2026, 11, 9)))
        assertEquals(LocalDate(2026, 11, 9), weekStart(LocalDate(2026, 11, 12)))
        assertEquals(LocalDate(2026, 11, 9), weekStart(LocalDate(2026, 11, 15)))
        assertEquals(LocalDate(2026, 11, 16), weekStart(LocalDate(2026, 11, 16)))
    }

    @Test
    fun minutesThisWeekIncludesMondayAndSundayButNotNeighbours() {
        val events = listOf(
            event(at(bangkok, 2026, 11, 8, 23, 50), playedMs = 5 * 60_000),  // previous Sunday
            event(at(bangkok, 2026, 11, 9, 0, 10), playedMs = 10 * 60_000),  // Monday
            event(at(bangkok, 2026, 11, 15, 23, 0), playedMs = 20 * 60_000), // Sunday
            event(at(bangkok, 2026, 11, 16, 0, 5), playedMs = 40 * 60_000)   // next Monday
        )
        assertEquals(30, aggregateMinutesThisWeek(events, today = LocalDate(2026, 11, 12), zone = bangkok))
    }

    @Test
    fun weekBucketingFollowsTheGivenTimeZone() {
        // 2026-11-08 20:00 UTC is already Monday 03:00 in Bangkok.
        val events = listOf(event(at(utc, 2026, 11, 8, 20, 0), playedMs = 15 * 60_000))
        assertEquals(15, aggregateMinutesThisWeek(events, today = LocalDate(2026, 11, 9), zone = bangkok))
        assertEquals(0, aggregateMinutesThisWeek(events, today = LocalDate(2026, 11, 9), zone = utc))
    }

    @Test
    fun dailyMinutesCoversEveryDayOldestFirst() {
        val events = listOf(
            event(at(bangkok, 2026, 11, 10), playedMs = 3 * 60_000),
            event(at(bangkok, 2026, 11, 10, 18), playedMs = 2 * 60_000),
            event(at(bangkok, 2026, 11, 12), playedMs = 61_000)
        )
        val days = dailyMinutes(events, lastDay = LocalDate(2026, 11, 12), days = 7, zone = bangkok)
        assertEquals(7, days.size)
        assertEquals(LocalDate(2026, 11, 6), days.first().date)
        assertEquals(LocalDate(2026, 11, 12), days.last().date)
        assertEquals(5, days.first { it.date == LocalDate(2026, 11, 10) }.minutes)
        assertEquals(1, days.last().minutes)
        assertEquals(0, days.first().minutes)
    }

    // ---- streaks -------------------------------------------------------------------------

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val events = listOf(10, 11, 12).map { d -> event(at(bangkok, 2026, 11, d)) }
        assertEquals(Streaks(current = 3, best = 3), streaks(events, LocalDate(2026, 11, 12), bangkok))
    }

    @Test
    fun streakStaysAliveUntilTodayIsOver() {
        // Nothing yet today: yesterday's run still counts.
        val events = listOf(10, 11).map { d -> event(at(bangkok, 2026, 11, d)) }
        assertEquals(2, streaks(events, LocalDate(2026, 11, 12), bangkok).current)
        // A gap of a full day breaks it.
        assertEquals(0, streaks(events, LocalDate(2026, 11, 13), bangkok).current)
    }

    @Test
    fun streakIgnoresDaysUnderOneMinuteAndTracksBest() {
        val events = listOf(1, 2, 3, 4).map { d -> event(at(bangkok, 2026, 11, d)) } +
            event(at(bangkok, 2026, 11, 5), playedMs = 59_000) + // under a minute: not a streak day
            listOf(6, 7).map { d -> event(at(bangkok, 2026, 11, d)) }
        assertEquals(Streaks(current = 2, best = 4), streaks(events, LocalDate(2026, 11, 7), bangkok))
    }

    @Test
    fun streakOfEmptyHistoryIsZero() {
        assertEquals(Streaks(0, 0), streaks(emptyList(), LocalDate(2026, 11, 7), bangkok))
    }

    // ---- top artists / genres ------------------------------------------------------------

    @Test
    fun topArtistsRankByPlaysThenName() {
        val a = HistoryArtist(name = "Alpha", id = "a")
        val b = HistoryArtist(name = "Beta", id = "b", imageUrl = "https://img/b")
        val c = HistoryArtist(name = "Cee", id = "c")
        val events = listOf(
            event(0, artists = listOf(b)),
            event(0, artists = listOf(b, a)),
            event(0, artists = listOf(c)),
            event(0, artists = listOf(a)),
            event(0, artists = listOf(b)),
            event(0, artists = listOf(c), playedMs = 5_000, durationMs = 200_000) // too short to count
        )
        val top = topArtists(events, limit = 5)
        assertEquals(listOf("Beta" to 3, "Alpha" to 2, "Cee" to 1), top.map { it.name to it.plays })
        assertEquals("https://img/b", top.first().imageUrl)
        assertEquals(1, topArtists(events, limit = 1).size)
    }

    @Test
    fun artistsWithoutIdsGroupByName() {
        val events = listOf(
            event(0, artists = listOf(HistoryArtist(name = "Local Band"))),
            event(0, artists = listOf(HistoryArtist(name = "local band")))
        )
        assertEquals(2, topArtists(events).single().plays)
    }

    @Test
    fun genreSharesAreEmptyWithoutGenreData() {
        assertTrue(genreShares(listOf(event(0), event(0))).isEmpty())
        val shares = genreShares(
            listOf(
                event(0, genres = listOf("Indie")),
                event(0, genres = listOf("Indie")),
                event(0, genres = listOf("Pop")),
                event(0, genres = listOf("Indie"))
            )
        )
        assertEquals(listOf("Indie" to 75, "Pop" to 25), shares.map { it.name to it.percent })
    }

    // ---- storage cap ---------------------------------------------------------------------

    @Test
    fun trimDropsEventsOlderThanTheWindow() {
        val now = at(utc, 2026, 11, 12)
        val old = event(now - 91L * DAY_MS)
        val kept = event(now - 89L * DAY_MS)
        assertEquals(listOf(kept), trimHistory(listOf(old, kept), nowMillis = now, maxAgeDays = 90, maxEvents = 100))
    }

    @Test
    fun trimKeepsTheNewestEventsUpToTheCap() {
        val now = at(utc, 2026, 11, 12)
        val events = (1..10).map { i -> event(now - i * 60_000L) }
        val trimmed = trimHistory(events.shuffled(), nowMillis = now, maxAgeDays = 90, maxEvents = 3)
        assertEquals(events.take(3).reversed(), trimmed) // oldest first, newest three only
    }

    @Test
    fun dedupeKeepsTheLastCheckpointOfASession() {
        val first = event(1_000, playedMs = 30_000, sessionId = "x")
        val other = event(2_000, sessionId = "y")
        val later = first.copy(playedMs = 90_000)
        val result = dedupeSessions(listOf(first, other, later))
        assertEquals(listOf(later, other), result)
    }

    @Test
    fun dedupeIgnoresAShorterCheckpointWrittenLate() {
        val longer = event(1_000, playedMs = 90_000, sessionId = "x")
        val staleCheckpoint = longer.copy(playedMs = 30_000)
        assertEquals(listOf(longer), dedupeSessions(listOf(longer, staleCheckpoint)))
    }

    // ---- recent collections --------------------------------------------------------------

    @Test
    fun recentCollectionsKeepTheLatestEventPerCollectionNewestFirst() {
        val album = HistoryCollection(id = "al", type = RecentType.Album, title = "Album")
        val playlist = HistoryCollection(id = "pl", type = RecentType.Playlist, title = "Mix")
        val events = listOf(
            event(1_000, playedMs = 30_000, collection = album.copy(progress = 0.1f, remainingMs = 600_000)),
            event(5_000, playedMs = 60_000, collection = playlist),
            event(9_000, playedMs = 60_000, collection = album.copy(progress = 0.5f, remainingMs = 90_001)),
            event(12_000) // no collection
        )
        val recent = aggregateRecentCollections(events)
        assertEquals(listOf("al", "pl"), recent.map { it.id })
        val first = recent.first()
        assertEquals(RecentType.Album, first.type)
        assertEquals(0.5f, first.progress)
        assertEquals(2, first.remainingMinutes) // rounded up
        assertEquals(69_000L, first.lastPlayedAtMillis)
        assertNull(recent.last().progress)
        assertNull(recent.last().remainingMinutes)
    }

    // ---- stats ---------------------------------------------------------------------------

    @Test
    fun statsCompareWithThePreviousPeriod() {
        val today = LocalDate(2026, 11, 12)
        val events = listOf(
            event(at(bangkok, 2026, 11, 1), playedMs = 10 * 60_000, trackId = "old"),  // previous week
            event(at(bangkok, 2026, 11, 10), playedMs = 9 * 60_000, trackId = "old"),
            event(at(bangkok, 2026, 11, 12), playedMs = 6 * 60_000, trackId = "new")
        )
        val stats = listeningStats(events, StatsRange.Week, today, bangkok)
        assertEquals(15, stats.totalMinutes)
        assertEquals(10, stats.previousTotalMinutes)
        assertEquals(50, stats.deltaPercent)
        assertEquals(2, stats.tracksPlayed)
        assertEquals(1, stats.newTracks)
        assertEquals(7, stats.days.size)
        assertTrue(stats.hasHistory)
    }

    @Test
    fun statsHaveNoDeltaWithoutAPreviousPeriod() {
        val today = LocalDate(2026, 11, 12)
        val stats = listeningStats(listOf(event(at(bangkok, 2026, 11, 12))), StatsRange.Month, today, bangkok)
        assertNull(stats.deltaPercent)
        assertEquals(30, stats.days.size)
        assertEquals(false, listeningStats(emptyList(), StatsRange.Week, today, bangkok).hasHistory)
    }
}
