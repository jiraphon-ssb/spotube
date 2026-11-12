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

package org.spotube.spotube.modules.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.plugin.PluginManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
enum class LyricType {
    STATIC,
    SYNCED
}

/** Why there are no lyrics to fetch at all (as opposed to a fetch that found none or failed). */
enum class LyricsUnavailableReason {
    /** No lyrics plugin is installed/selected. */
    NoPlugin,
    /** The current entry is a local file; lyrics plugins only resolve streaming tracks. */
    LocalFile,
    /** The queue is empty. */
    NothingPlaying
}

data class LyricsUiState(
    val mode: LyricType = LyricType.SYNCED,
    val syncedLyrics: List<LyricsLine>? = null,
    val plainLyrics: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = -1,
    val trackTitle: String = "",
    val trackArtists: String = "",
    val coverUrl: String = "",
    val unavailable: LyricsUnavailableReason? = null)

class LyricsViewModel(
    private val pluginManager: PluginManager,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())

    // Only re-emits when the active line changes, not on every position tick.
    private val currentIndexFlow = combine(
        _uiState.map { it.syncedLyrics }.distinctUntilChanged(),
        audioPlayer.positionFlow
    ) { lyrics, position -> activeLineIndex(lyrics, position.inWholeMilliseconds) }
        .distinctUntilChanged()

    val uiState: StateFlow<LyricsUiState> = combine(_uiState, currentIndexFlow) { state, index ->
        state.copy(currentIndex = index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsUiState())

    private var currentTrackId: String? = null

    /** The in-flight fetch; replaced (and cancelled) whenever another track's lyrics are requested. */
    private var loadJob: Job? = null

    fun setMode(mode: LyricType) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun loadLyrics(forceReload: Boolean = false) {
        val track = when (val entry = audioPlayerQueue.currentQueueEntryFlow.value) {
            null -> {
                markUnavailable(LyricsUnavailableReason.NothingPlaying)
                return
            }
            is QueueEntry.LocalTrack -> {
                markUnavailable(LyricsUnavailableReason.LocalFile)
                return
            }
            is QueueEntry.StreamingTrack -> entry.track
        }

        // Already showing (or already fetching) this track's lyrics.
        if (!forceReload && currentTrackId == track.id &&
            (_uiState.value.syncedLyrics != null || loadJob?.isActive == true)
        ) {
            return
        }

        loadJob?.cancel()
        currentTrackId = track.id
        val pluginService = pluginManager.selectedLyricsPlugin.value
        if (pluginService == null) {
            markUnavailable(LyricsUnavailableReason.NoPlugin)
            return
        }

        // Drop the previous track's lines up front so they never show under the new title.
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                unavailable = null,
                syncedLyrics = null,
                plainLyrics = null)
        }
        val requestedTrackId = track.id
        loadJob = viewModelScope.launch {
            try {
                val lyrics = pluginService.use { lyricsAPI.getLyrics(track) }
                // The track changed while this was in flight: these lines belong to another song.
                if (!isStillCurrent(requestedTrackId)) return@launch
                _uiState.update {
                    it.copy(
                        syncedLyrics = lyrics?.syncedLyrics,
                        plainLyrics = lyrics?.plainLyrics,
                        isLoading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isStillCurrent(requestedTrackId)) return@launch
                _uiState.update { it.copy(isLoading = false, error = e.message.orEmpty()) }
            }
        }
    }

    private fun isStillCurrent(trackId: String): Boolean {
        val entry = audioPlayerQueue.currentQueueEntryFlow.value
        return currentTrackId == trackId &&
            entry is QueueEntry.StreamingTrack && entry.track.id == trackId
    }

    /** Clears any lyrics left over from the previous track and records why none can load. */
    private fun markUnavailable(reason: LyricsUnavailableReason) {
        // Whatever was being fetched no longer applies.
        loadJob?.cancel()
        if (reason != LyricsUnavailableReason.NoPlugin) currentTrackId = null
        _uiState.update {
            it.copy(
                syncedLyrics = null,
                plainLyrics = null,
                isLoading = false,
                error = null,
                unavailable = reason)
        }
    }

    init {
        viewModelScope.launch {
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                when (entry) {
                    is QueueEntry.StreamingTrack -> {
                        val track = entry.track
                        _uiState.update {
                            it.copy(
                                trackTitle = track.title,
                                trackArtists = track.artists.joinToString(", ") { artist -> artist.name },
                                coverUrl = (track.album?.thumbnails ?: track.thumbnails)
                                    ?.firstOrNull()?.url.orEmpty()
                            )
                        }
                        if (track.id != currentTrackId) loadLyrics()
                    }

                    is QueueEntry.LocalTrack -> {
                        _uiState.update {
                            it.copy(
                                trackTitle = entry.name,
                                trackArtists = entry.artists.joinToString(", "),
                                coverUrl = entry.artworkUrl.orEmpty()
                            )
                        }
                        markUnavailable(LyricsUnavailableReason.LocalFile)
                    }

                    null -> {
                        _uiState.update { it.copy(trackTitle = "", trackArtists = "", coverUrl = "") }
                        markUnavailable(LyricsUnavailableReason.NothingPlaying)
                    }
                }
            }
        }
        // The selected lyrics plugin resolves asynchronously at startup (and changes when the
        // user installs/switches one): retry so a "no plugin" state doesn't stick.
        viewModelScope.launch {
            pluginManager.selectedLyricsPlugin.drop(1).collect { loadLyrics(forceReload = true) }
        }
    }

    fun seekToLine(line: LyricsLine) {
        viewModelScope.launch {
            audioPlayer.seekTo(line.time.milliseconds)
        }
    }

    private fun activeLineIndex(lyrics: List<LyricsLine>?, positionMillis: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        val index = lyrics.indexOfLast { it.time <= positionMillis }
        return index.coerceAtLeast(0)
    }
}
