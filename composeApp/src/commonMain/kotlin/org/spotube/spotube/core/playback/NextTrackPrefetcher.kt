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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.core.server.TrackSourceRepository
import org.spotube.spotube.modules.plugin.AudioPluginSource

/**
 * Warms the *next* queue entry so moving on doesn't wait on the audio plugin.
 *
 * Starting a streamed track costs two plugin round trips: matching it to a source (a search, a few
 * seconds, persisted by [TrackSourceRepository]) and resolving a playable URL (~1–4 s, cached by
 * [StreamingUrlRepository] for only ~30 s). So the work is split to fit those lifetimes:
 *
 * 1. [PrefetchStep.Match] — once the current track has settled, make sure the next track is
 *    already matched. Skipping ahead mid-song then only pays for the URL.
 * 2. [PrefetchStep.Url] — in the last [NextTrackPrefetchPlanner.urlWindowMs] of a playing track,
 *    resolve the next URL so the automatic advance finds it still in the cache.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NextTrackPrefetcher(
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val streamingUrlRepository: StreamingUrlRepository,
    private val trackSourceRepository: TrackSourceRepository,
    private val audioPluginSource: AudioPluginSource,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : KoinComponent {

    private val logger by injectLogger<NextTrackPrefetcher>()
    private val planner = NextTrackPrefetchPlanner()
    // One prefetch at a time: they are background work and must not pile up on the plugin.
    private val runMutex = Mutex()

    // Prefetches started for the current track. Only touched from the collector below (one
    // coroutine), and cancelled when the current track changes so they never compete with the
    // track the user just started.
    private val inFlight = mutableListOf<Job>()
    private var inFlightFor: QueueEntry? = null

    init {
        scope.launch {
            audioPlayerQueue.currentQueueEntryFlow.flatMapLatest { current ->
                // positionFlow still holds the *previous* track's position when the current entry
                // changes; skip that replayed value so a fresh track never looks "almost over".
                combine(
                    audioPlayerQueue.queueFlow,
                    audioPlayer.loopStateFlow,
                    audioPlayer.playerStateFlow,
                    audioPlayer.positionFlow.drop(1)
                ) { queue, loop, state, position ->
                    // Metadata duration first: the player's duration is also stale right after a
                    // track change.
                    val metadataDuration = (current as? QueueEntry.StreamingTrack)?.track?.durationMs ?: 0L
                    val playerDuration = audioPlayer.durationFlow.value.inWholeMilliseconds
                    PrefetchSnapshot(
                        queue = queue,
                        current = current,
                        loop = loop,
                        isPlaying = state == PlayerState.PLAYING,
                        positionMs = position.inWholeMilliseconds,
                        durationMs = if (metadataDuration > 0) metadataDuration else playerDuration
                    )
                }
            }.collect { snapshot ->
                if (snapshot.current != inFlightFor) {
                    inFlightFor = snapshot.current
                    inFlight.forEach { it.cancel() }
                    inFlight.clear()
                }
                planner.plan(snapshot).forEach { step ->
                    inFlight.removeAll { !it.isActive }
                    inFlight += scope.launch { run(step) }
                }
            }
        }
    }

    private suspend fun run(step: PrefetchStep) {
        runMutex.withLock {
            try {
                when (step) {
                    is PrefetchStep.Match -> {
                        val pluginId = audioPluginSource.selectedAudioPlugin.value?.pluginId
                        val alreadyMatched = pluginId != null &&
                            trackSourceRepository.getTrackSource(step.track, pluginId) != null
                        if (pluginId != null && !alreadyMatched) {
                            logger.d { "[Prefetch] matching next track ${step.track.id} '${step.track.title}'" }
                            streamingUrlRepository.resolveStreamInfo(step.track)
                        }
                    }
                    is PrefetchStep.Url -> {
                        logger.d { "[Prefetch] resolving URL for next track ${step.track.id} '${step.track.title}'" }
                        streamingUrlRepository.resolveStreamInfo(step.track)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(e) { "[Prefetch] failed for ${step.track.id}; playback will resolve it on demand" }
            }
        }
    }
}

/** What the planner needs to know about playback at one instant. */
internal data class PrefetchSnapshot(
    val queue: List<QueueEntry>,
    val current: QueueEntry?,
    val loop: LoopState,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long
)

internal sealed interface PrefetchStep {
    val track: MetadataTrack

    data class Match(override val track: MetadataTrack) : PrefetchStep
    data class Url(override val track: MetadataTrack) : PrefetchStep
}

/**
 * Pure decision logic for [NextTrackPrefetcher]: given successive playback snapshots, emits each
 * step at most once per (current, next) pair.
 *
 * @param settleMs Wait this far into the current track before matching the next one, so the
 *   prefetch never competes with the current track's own start-up.
 * @param urlWindowMs Resolve the next URL when this much (or less) of the current track is left.
 *   Must stay below the stream-URL cache lifetime (30 s) or the URL expires before use.
 */
internal class NextTrackPrefetchPlanner(
    private val settleMs: Long = 8_000,
    val urlWindowMs: Long = 20_000
) {
    private var matchedKey: String? = null
    private var urlKey: String? = null

    fun plan(snapshot: PrefetchSnapshot): List<PrefetchStep> {
        val next = nextTrackToPrefetch(snapshot.queue, snapshot.current, snapshot.loop) ?: return emptyList()
        val currentId = (snapshot.current as? QueueEntry.StreamingTrack)?.track?.id ?: snapshot.current?.url
        val key = "$currentId->${next.id}"
        val steps = mutableListOf<PrefetchStep>()

        // A position past the end is a stale reading from the previous track; ignore it.
        if (snapshot.durationMs > 0 && snapshot.positionMs > snapshot.durationMs) return steps
        if (snapshot.positionMs >= settleMs && matchedKey != key) {
            matchedKey = key
            steps += PrefetchStep.Match(next)
        }

        val remaining = snapshot.durationMs - snapshot.positionMs
        if (snapshot.isPlaying && snapshot.durationMs > 0 && remaining in 1..urlWindowMs && urlKey != key) {
            urlKey = key
            steps += PrefetchStep.Url(next)
        }
        return steps
    }
}

/**
 * The streamed track that will play after [current], or null when there is nothing to warm:
 * repeat-one, end of a non-looping queue, a local file next, or [current] not in [queue].
 */
internal fun nextTrackToPrefetch(
    queue: List<QueueEntry>,
    current: QueueEntry?,
    loop: LoopState
): MetadataTrack? {
    if (current == null || loop == LoopState.ONE) return null
    val index = queue.indexOf(current)
    if (index < 0) return null
    val nextEntry = queue.getOrNull(index + 1)
        ?: if (loop == LoopState.ALL) queue.firstOrNull() else null
    val next = (nextEntry as? QueueEntry.StreamingTrack)?.track ?: return null
    val currentId = (current as? QueueEntry.StreamingTrack)?.track?.id
    return next.takeIf { it.id != currentId }
}
