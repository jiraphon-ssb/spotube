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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.album.AlbumRepository
import org.spotube.spotube.modules.playlist.PlaylistRepository
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Watches the player and writes listening sessions to [LocalListeningHistoryRepository].
 *
 * A session is one track from the moment it starts playing until the current entry changes or
 * the track completes. Its played time is wall-clock time spent in [PlayerState.PLAYING] (pauses
 * and buffering don't count). Sessions are saved every [CHECKPOINT_MS] while playing (so a crash
 * or quit loses at most that much) and once more when they end; sessions under [MIN_RECORDED_MS]
 * are never saved.
 *
 * Collection progress is taken from the queue (index of the track in it, plus the position in
 * the track) whenever the queue was started from a playlist or album.
 */
class ListeningHistoryRecorder(
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val repository: LocalListeningHistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val logger = Logger.withTag("ListeningHistory")
    private val mutex = Mutex()
    private var session: Session? = null

    // Collection titles/covers resolved once per collection (plugin round trip).
    private val collectionInfo = HashMap<String, CollectionInfo>()
    private val infoMutex = Mutex()

    init {
        scope.launch {
            combine(
                audioPlayerQueue.currentQueueEntryFlow,
                audioPlayer.playerStateFlow
            ) { entry, state -> entry to (state == PlayerState.PLAYING) }
                .distinctUntilChanged { old, new ->
                    old.first?.historyKey() == new.first?.historyKey() && old.second == new.second
                }
                .collect { (entry, playing) -> onPlaybackChanged(entry, playing) }
        }
        scope.launch {
            audioPlayer.completionFlow.collect { onCompleted() }
        }
        scope.launch {
            while (isActive) {
                delay(CHECKPOINT_MS)
                checkpoint()
            }
        }
    }

    private suspend fun onPlaybackChanged(entry: QueueEntry?, playing: Boolean) {
        val now = nowMillis()
        mutex.withLock {
            val current = session
            if (current != null && current.key != entry?.historyKey()) {
                current.pause(now)
                save(current, now, isCurrent = false)
                session = null
            }
            if (entry == null) return
            val active = session
            if (playing) {
                (active ?: startSession(entry, now)).resume(now)
            } else {
                active?.pause(now)
            }
        }
    }

    /** The track ended: close its session; a repeat of the same track starts a fresh one. */
    private suspend fun onCompleted() {
        val now = nowMillis()
        mutex.withLock {
            val current = session ?: return
            current.pause(now)
            save(current, now, isCurrent = false)
            session = null
            val entry = audioPlayerQueue.currentQueueEntryFlow.value
            if (entry != null && audioPlayer.playerStateFlow.value == PlayerState.PLAYING) {
                startSession(entry, now).resume(now)
            }
        }
    }

    private suspend fun checkpoint() {
        val now = nowMillis()
        mutex.withLock {
            val current = session ?: return
            if (current.isPlaying) save(current, now, isCurrent = true)
        }
    }

    private fun startSession(entry: QueueEntry, now: Long): Session = Session(
        id = "${now.toString(36)}-${Random.nextLong().toString(36)}",
        entry = entry,
        key = entry.historyKey(),
        collection = audioPlayerQueue.currentCollectionEntryFlow.value,
        startedAt = now
    ).also { session = it }

    /** Snapshots [s] now (cheap, under the lock) and persists it in the background. */
    private fun save(s: Session, now: Long, isCurrent: Boolean) {
        val playedMs = s.playedAt(now)
        if (playedMs < MIN_RECORDED_MS) return
        val position = collectionPosition(s, isCurrent)
        val collectionEntry = s.collection
        scope.launch {
            try {
                val info = collectionEntry?.let { describe(it, s.entry) }
                val collection = if (collectionEntry != null && info != null) {
                    HistoryCollection(
                        id = collectionEntry.id,
                        type = info.type,
                        title = info.title,
                        coverUrl = info.coverUrl,
                        progress = position?.progress,
                        remainingMs = position?.remainingMs
                    )
                } else {
                    null
                }
                repository.record(s.entry.toEvent(s.id, s.startedAt, playedMs, collection))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(e) { "Could not record listening session" }
            }
        }
    }

    private fun collectionPosition(s: Session, isCurrent: Boolean): CollectionPosition? {
        if (s.collection == null) return null
        val queue = audioPlayerQueue.queueFlow.value
        val index = queue.indexOfFirst { it.historyKey() == s.key }
        if (index < 0 || queue.isEmpty()) return null
        val duration = s.entry.durationMs()
        val position = when {
            duration <= 0L -> 0L
            isCurrent -> audioPlayer.positionFlow.value.inWholeMilliseconds.coerceIn(0L, duration)
            else -> duration // moved on: count this track as done
        }
        val fraction = if (duration > 0L) position.toFloat() / duration else 0f
        val rest = queue.drop(index + 1).sumOf { it.durationMs() }
        return CollectionPosition(
            progress = ((index + fraction) / queue.size).coerceIn(0f, 1f),
            remainingMs = rest + (duration - position).coerceAtLeast(0L)
        )
    }

    private suspend fun describe(collection: QueueCollectionEntry, entry: QueueEntry): CollectionInfo? {
        val cacheKey = when (collection) {
            is QueueCollectionEntry.Playlist -> "playlist:${collection.id}"
            is QueueCollectionEntry.Album -> "album:${collection.id}"
            QueueCollectionEntry.SavedTracks -> return CollectionInfo(RecentType.Playlist, "", null)
        }
        infoMutex.withLock { collectionInfo[cacheKey] }?.let { return it }

        val info = when (collection) {
            is QueueCollectionEntry.Album -> {
                val trackAlbum = (entry as? QueueEntry.StreamingTrack)?.track?.album
                    ?.takeIf { it.id == collection.id }
                if (trackAlbum != null) {
                    CollectionInfo(RecentType.Album, trackAlbum.title, trackAlbum.thumbnails.bestUrl())
                } else {
                    lookup { albumRepository.getAlbumInfo(collection.id)?.first }?.let { album ->
                        CollectionInfo(RecentType.Album, album.title, album.thumbnails.bestUrl())
                    }
                }
            }
            is QueueCollectionEntry.Playlist ->
                lookup { playlistRepository.getPlaylistInfo(collection.id)?.first }?.let { playlist ->
                    CollectionInfo(RecentType.Playlist, playlist.title, playlist.thumbnails.bestUrl())
                }
            QueueCollectionEntry.SavedTracks -> null
        }
        // Failed lookups are not cached so the next session retries; the session is still saved
        // (with a blank title) so its listening time counts.
        if (info != null) infoMutex.withLock { collectionInfo[cacheKey] = info }
        return info ?: CollectionInfo(
            type = if (collection is QueueCollectionEntry.Album) RecentType.Album else RecentType.Playlist,
            title = "",
            coverUrl = null
        )
    }

    private suspend fun <T> lookup(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.d { "Collection lookup failed: ${e.message}" }
        null
    }

    private class Session(
        val id: String,
        val entry: QueueEntry,
        val key: String,
        val collection: QueueCollectionEntry?,
        val startedAt: Long
    ) {
        private var playedMs = 0L
        private var playingSince: Long? = null

        val isPlaying: Boolean get() = playingSince != null

        fun resume(now: Long) {
            if (playingSince == null) playingSince = now
        }

        fun pause(now: Long) {
            playingSince?.let { playedMs += (now - it).coerceAtLeast(0L) }
            playingSince = null
        }

        fun playedAt(now: Long): Long = playedMs + (playingSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)
    }

    private data class CollectionInfo(val type: RecentType, val title: String, val coverUrl: String?)

    private data class CollectionPosition(val progress: Float, val remainingMs: Long)

    private companion object {
        const val CHECKPOINT_MS = 30_000L
    }
}

/** Identity of a queue entry for history purposes (track id for streams, file URL for local). */
internal fun QueueEntry.historyKey(): String = when (this) {
    is QueueEntry.StreamingTrack -> "track:${track.id}"
    is QueueEntry.LocalTrack -> "local:$url"
}

internal fun QueueEntry.durationMs(): Long = when (this) {
    is QueueEntry.StreamingTrack -> track.durationMs
    is QueueEntry.LocalTrack -> duration
}

private fun List<Thumbnail>?.bestUrl(): String? = this?.maxByOrNull { it.width * it.height }?.url

internal fun QueueEntry.toEvent(
    sessionId: String,
    startedAt: Long,
    playedMs: Long,
    collection: HistoryCollection?
): ListeningEvent = when (this) {
    is QueueEntry.StreamingTrack -> ListeningEvent(
        sessionId = sessionId,
        startedAtMillis = startedAt,
        playedMs = playedMs,
        trackId = track.id,
        title = track.title,
        artists = track.artists.map { artist ->
            HistoryArtist(name = artist.name, id = artist.id, imageUrl = artist.thumbnails.bestUrl())
        },
        albumId = track.album?.id,
        albumTitle = track.album?.title,
        coverUrl = artworkUrl,
        genres = track.album?.genres.orEmpty(),
        durationMs = track.durationMs,
        collection = collection
    )
    is QueueEntry.LocalTrack -> ListeningEvent(
        sessionId = sessionId,
        startedAtMillis = startedAt,
        playedMs = playedMs,
        trackId = "local:$url",
        title = name,
        artists = artists.map { HistoryArtist(name = it) },
        albumTitle = album,
        durationMs = duration,
        collection = collection
    )
}
