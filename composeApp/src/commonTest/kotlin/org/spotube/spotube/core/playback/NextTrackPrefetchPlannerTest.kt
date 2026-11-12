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

package org.spotube.spotube.core.playback

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NextTrackPrefetchPlannerTest {

    // --- nextTrackToPrefetch --------------------------------------------------------------

    @Test
    fun nextIsTheFollowingStreamingTrack() {
        val queue = listOf(entry("a"), entry("b"), entry("c"))
        assertEquals("b", nextTrackToPrefetch(queue, queue[0], LoopState.NONE)?.id)
    }

    @Test
    fun lastTrackHasNoNextUnlessLoopingAll() {
        val queue = listOf(entry("a"), entry("b"))
        assertNull(nextTrackToPrefetch(queue, queue[1], LoopState.NONE))
        assertEquals("a", nextTrackToPrefetch(queue, queue[1], LoopState.ALL)?.id)
    }

    @Test
    fun loopOneRepeatsTheCurrentTrackSoNothingToPrefetch() {
        val queue = listOf(entry("a"), entry("b"))
        assertNull(nextTrackToPrefetch(queue, queue[0], LoopState.ONE))
    }

    @Test
    fun localNextEntryNeedsNoPrefetch() {
        val local = QueueEntry.LocalTrack(
            name = "Local", artists = listOf("Me"), duration = 1000L, album = null,
            coverBytes = null, url = "file:///music/local.mp3")
        val queue = listOf(entry("a"), local)
        assertNull(nextTrackToPrefetch(queue, queue[0], LoopState.NONE))
    }

    @Test
    fun currentNotInQueueMeansNoNext() {
        val queue = listOf(entry("a"), entry("b"))
        assertNull(nextTrackToPrefetch(queue, entry("zzz"), LoopState.NONE))
        assertNull(nextTrackToPrefetch(queue, null, LoopState.NONE))
    }

    // --- NextTrackPrefetchPlanner ---------------------------------------------------------

    @Test
    fun noWorkBeforeTheCurrentTrackHasSettled() {
        val planner = NextTrackPrefetchPlanner()
        val steps = planner.plan(snapshot(positionMs = 2_000))
        assertTrue(steps.isEmpty(), "must not compete with the current track's own start-up")
    }

    @Test
    fun warmsTheNextMatchOnceAfterSettling() {
        val planner = NextTrackPrefetchPlanner()
        assertEquals(listOf<PrefetchStep>(PrefetchStep.Match(track("b"))), planner.plan(snapshot(positionMs = 9_000)))
        assertTrue(planner.plan(snapshot(positionMs = 15_000)).isEmpty(), "match is warmed once per next track")
    }

    @Test
    fun warmsTheNextUrlInTheFinalWindowOnlyWhilePlaying() {
        val planner = NextTrackPrefetchPlanner(urlWindowMs = 20_000)
        planner.plan(snapshot(positionMs = 9_000)) // match step

        assertTrue(planner.plan(snapshot(positionMs = 150_000)).isEmpty(), "too early for a 30s-lived URL")
        assertTrue(
            planner.plan(snapshot(positionMs = 185_000, playing = false)).isEmpty(),
            "paused: the URL would expire before it is used")
        assertEquals(
            listOf<PrefetchStep>(PrefetchStep.Url(track("b"))),
            planner.plan(snapshot(positionMs = 185_000)))
        assertTrue(planner.plan(snapshot(positionMs = 190_000)).isEmpty(), "URL is warmed once")
    }

    @Test
    fun newCurrentTrackTargetsItsOwnNext() {
        val planner = NextTrackPrefetchPlanner()
        planner.plan(snapshot(current = "a", positionMs = 9_000))
        assertEquals(
            listOf<PrefetchStep>(PrefetchStep.Match(track("c"))),
            planner.plan(snapshot(current = "b", positionMs = 9_000)))
    }

    @Test
    fun unknownDurationNeverTriggersTheUrlStep() {
        val planner = NextTrackPrefetchPlanner()
        planner.plan(snapshot(positionMs = 9_000))
        assertTrue(planner.plan(snapshot(positionMs = 9_500, durationMs = 0)).isEmpty())
    }

    @Test
    fun positionPastTheEndIsAStaleReadingAndIgnored() {
        val planner = NextTrackPrefetchPlanner()
        // e.g. the previous track's position replayed right after a track change
        assertTrue(planner.plan(snapshot(positionMs = 250_000, durationMs = 200_000)).isEmpty())
        // the real position for the new track still triggers the match later
        assertEquals(listOf<PrefetchStep>(PrefetchStep.Match(track("b"))), planner.plan(snapshot(positionMs = 9_000)))
    }

    // --- fixtures -------------------------------------------------------------------------

    private val queue = listOf(entry("a"), entry("b"), entry("c"))

    private fun snapshot(
        current: String = "a",
        positionMs: Long,
        durationMs: Long = 200_000,
        playing: Boolean = true,
        loop: LoopState = LoopState.NONE
    ) = PrefetchSnapshot(
        queue = queue,
        current = queue.first { (it as QueueEntry.StreamingTrack).track.id == current },
        loop = loop,
        isPlaying = playing,
        positionMs = positionMs,
        durationMs = durationMs)

    private fun entry(id: String) = QueueEntry.StreamingTrack(track = track(id), url = "")

    private fun track(id: String): MetadataTrack {
        val artist = MetadataArtist.Basic(
            id = "artist-$id", name = "Artist $id", thumbnails = emptyList(), externalUri = null)
        return MetadataTrack(
            id = id,
            title = "Track $id",
            durationMs = 200_000,
            trackNumber = 1,
            discNumber = 1,
            artists = listOf(artist),
            album = MetadataAlbum.Detailed(
                releaseDate = "2024",
                genres = emptyList(),
                trackCount = 1,
                id = "album-$id",
                title = "Album $id",
                description = null,
                thumbnails = emptyList(),
                albumType = MetadataAlbumType.Album,
                artists = listOf(artist),
                externalUri = null),
            thumbnails = null,
            explicit = false,
            popularity = null,
            isrcCode = null,
            externalUri = null)
    }
}
