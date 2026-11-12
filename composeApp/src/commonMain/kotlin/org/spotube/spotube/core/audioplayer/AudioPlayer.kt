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

package org.spotube.spotube.core.audioplayer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface AudioPlayerInterface {
    //    // Playback
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(position: Duration)
    suspend fun loop(state: LoopState)
    suspend fun shuffle(enabled: Boolean)

    // Playlist management
    suspend fun load(playlist: List<MediaItem>, autoPlay: Boolean, startPosition: Int)
    suspend fun addMediaItem(mediaItem: MediaItem)
    suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem)
    suspend fun removeMediaItem(mediaItem: MediaItem)
    suspend fun moveMediaItem(fromIndex: Int, toIndex: Int)
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun jumpTo(index: Int)

    // State as flows StateFlow

    val playerStateFlow : StateFlow<PlayerState>
    val currentMediaItemFlow : StateFlow<MediaItem?>
    val playlistFlow : StateFlow<List<MediaItem>>
    val durationFlow : StateFlow<Duration>
    val positionFlow : StateFlow<Duration>
    val bufferingPositionFlow : StateFlow<Duration>
    val loopStateFlow : StateFlow<LoopState>
    val shuffleModeFlow : StateFlow<Boolean>
    val playbackSpeedFlow : StateFlow<Float>
    val volumeFlow : StateFlow<Float>
    val completionFlow : Flow<Unit>
    val errorFlow : Flow<Throwable>

    suspend fun setVolume(volume: Float)
    suspend fun setPlaybackSpeed(speed: Float)

    // Sound options. Defaults are no-ops so platforms without the capability need no code; the
    // UI hides a control when its `supports…` flag is false.

    /** Whether [setEqualizer] has an audible effect on this platform. */
    val supportsEqualizer: Boolean get() = false

    /**
     * Applies a 10-band graphic EQ ([EqualizerBandsHz]), gains in dB (about -12..+12).
     * `null` turns the EQ off. [preampDb] is applied before the bands (negative = headroom).
     */
    suspend fun setEqualizer(bandsDb: List<Float>?, preampDb: Float = 0f) {}

    /** Whether [outputDevices] / [setOutputDevice] work on this platform. */
    val supportsOutputDevices: Boolean get() = false

    /** Audio outputs the player can route to. Empty when unsupported or none are reported. */
    suspend fun outputDevices(): List<AudioOutputDevice> = emptyList()

    /** The id of the output currently in use, if the platform reports it. */
    suspend fun currentOutputDevice(): String? = null

    /** Routes playback to the device with [id]; `null` leaves the current routing unchanged. */
    suspend fun setOutputDevice(id: String?) {}

    /** Whether [setVolumeNormalization] is honoured on this platform. */
    val supportsVolumeNormalization: Boolean get() = false

    /**
     * Evens out loudness between tracks. Desktop (VLC `normvol`) can only add the filter when the
     * player is created, so the change is stored and takes effect on the next app start.
     */
    suspend fun setVolumeNormalization(enabled: Boolean) {}

    fun isDisposed(): Boolean
    fun dispose() // Clean up resources when done. The player should not be used after this is called.
}

/** Centre frequencies of the [AudioPlayerInterface.setEqualizer] bands (ISO, as VLC uses). */
val EqualizerBandsHz: List<Int> = listOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

/** An audio output the player can route to. [id] is platform specific and opaque. */
data class AudioOutputDevice(val id: String, val name: String)

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class AudioPlayer(context: Any) : AudioPlayerInterface {
    val context: Any // Optional context for platform-specific implementations (e.g., Android Context)
    override suspend fun play()
    override suspend fun pause()
    override suspend fun stop()
    override suspend fun seekTo(position: Duration)
    override suspend fun loop(state: LoopState)
    override suspend fun shuffle(enabled: Boolean)
    override suspend fun load(
        playlist: List<MediaItem>,
        autoPlay: Boolean,
        startPosition: Int
    )

    override suspend fun addMediaItem(mediaItem: MediaItem)
    override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem)
    override suspend fun removeMediaItem(mediaItem: MediaItem)
    override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int)
    override suspend fun skipToNext()
    override suspend fun skipToPrevious()
    override suspend fun jumpTo(index: Int)
    override val playerStateFlow: StateFlow<PlayerState>
    override val currentMediaItemFlow: StateFlow<MediaItem?>
    override val playlistFlow: StateFlow<List<MediaItem>>
    override val durationFlow: StateFlow<Duration>
    override val positionFlow: StateFlow<Duration>
    override val bufferingPositionFlow: StateFlow<Duration>
    override val loopStateFlow: StateFlow<LoopState>
    override val shuffleModeFlow: StateFlow<Boolean>
    override val playbackSpeedFlow: StateFlow<Float>
    override val volumeFlow: StateFlow<Float>
    override val completionFlow: Flow<Unit>
    override val errorFlow: Flow<Throwable>
    override suspend fun setVolume(volume: Float)
    override suspend fun setPlaybackSpeed(speed: Float)
    override fun isDisposed(): Boolean
    override fun dispose()
}
