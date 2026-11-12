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

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.di.injectLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/** Built-in EQ curves (dB per [EqualizerBandsHz] band). */
enum class EqPreset(val bandsDb: List<Float>) {
    Flat(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    Bass(listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
    Vocal(listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, 0f)),
    Acoustic(listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f)),
    Night(listOf(2f, 1f, 0f, -1f, -2f, -2f, -1f, 0f, 1f, 1f))
}

/** Sleep timer options offered by the sound sheet and the command palette. */
enum class SleepTimerChoice(val duration: Duration?) {
    Off(null),
    Minutes15(15.minutes),
    Minutes30(30.minutes),
    Minutes60(60.minutes),

    /** Pauses when the track that is playing now ends. */
    EndOfTrack(null)
}

data class SoundSettingsState(
    /** One gain per [EqualizerBandsHz] band, -[SoundSettings.MAX_GAIN_DB]..+[SoundSettings.MAX_GAIN_DB]. */
    val bandsDb: List<Float> = EqPreset.Flat.bandsDb,
    /** The preset [bandsDb] matches, or null for a custom curve. */
    val preset: EqPreset? = EqPreset.Flat,
    val normalization: Boolean = false,
    /** Normalization as it was when this session started; the engine only applies it at start. */
    val normalizationAtStart: Boolean = false,
    val sleepTimer: SleepTimerChoice = SleepTimerChoice.Off,
    /** When a timed sleep timer fires, for a "stops in N min" hint. */
    val sleepTimerDeadline: TimeSource.Monotonic.ValueTimeMark? = null,
    /** False until the saved settings were read; the UI shows defaults until then. */
    val loaded: Boolean = false
) {
    val normalizationNeedsRestart: Boolean get() = normalization != normalizationAtStart
}

/**
 * Sound options shared by the sound sheet and the command palette: EQ curve, volume
 * normalization and the sleep timer. A process-wide singleton so a running sleep timer outlives
 * the sheet. EQ and normalization are saved in the settings DataStore (the desktop player also
 * keeps its own copy so it can restore them before any UI exists); the sleep timer is
 * session-only. Playback speed lives on the player itself ([AudioPlayerInterface.playbackSpeedFlow]).
 */
object SoundSettings : KoinComponent {
    const val MAX_GAIN_DB = 12f

    private val SOUND_SETTINGS_KEY = stringPreferencesKey("sound_settings")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val logger by injectLogger<SoundSettings>()
    private val database: Database by inject()
    private val player: AudioPlayerInterface by inject()
    private val queue: AudioPlayerQueue by inject()

    private val SAVE_DELAY = 400.milliseconds
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateFlow = MutableStateFlow(SoundSettingsState())
    val state: StateFlow<SoundSettingsState> = stateFlow.asStateFlow()

    private var loadJob: Job? = null
    private var eqJob: Job? = null
    private var saveJob: Job? = null
    private var sleepJob: Job? = null

    /** Reads the saved settings once per process. Safe to call from every composition. */
    fun ensureLoaded() {
        if (loadJob != null) return
        loadJob = scope.launch {
            val saved = runCatching {
                database.settingsDataStore.data.first()[SOUND_SETTINGS_KEY]
                    ?.let { json.decodeFromString<PersistedSound>(it) }
            }.onFailure { logger.w(it) { "Couldn't read sound settings" } }.getOrNull()
            stateFlow.update { current ->
                if (saved == null) {
                    current.copy(loaded = true)
                } else {
                    val bands = normalizeBands(saved.bandsDb)
                    current.copy(
                        bandsDb = bands,
                        preset = presetFor(bands),
                        normalization = saved.normalization,
                        normalizationAtStart = saved.normalization,
                        loaded = true
                    )
                }
            }
        }
    }

    // --- Equalizer --------------------------------------------------------------------------

    fun setBand(index: Int, gainDb: Float) {
        val current = stateFlow.value.bandsDb
        if (index !in current.indices) return
        // `+ 0f` turns -0f into 0f so preset matching (List.equals) sees a flat band as flat.
        val gain = gainDb.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) + 0f
        if (current[index] == gain) return
        val bands = current.toMutableList().also { it[index] = gain }
        stateFlow.update { it.copy(bandsDb = bands, preset = presetFor(bands)) }
        applyEqualizer(bands)
    }

    fun selectPreset(preset: EqPreset) {
        stateFlow.update { it.copy(bandsDb = preset.bandsDb, preset = preset) }
        applyEqualizer(preset.bandsDb)
    }

    private fun applyEqualizer(bands: List<Float>) {
        // Conflated: a drag fires many updates; only the newest one that hasn't started runs.
        eqJob?.cancel()
        eqJob = scope.launch {
            val flat = bands.all { it == 0f }
            // Boosts can clip; give back half of the largest boost as preamp headroom.
            val preamp = -(bands.maxOrNull() ?: 0f).coerceAtLeast(0f) / 2f
            runCatching { player.setEqualizer(if (flat) null else bands, preamp) }
                .onFailure { logger.w(it) { "Couldn't apply the equalizer" } }
        }
        saveLater()
    }

    // --- Normalization ----------------------------------------------------------------------

    fun setNormalization(enabled: Boolean) {
        stateFlow.update { it.copy(normalization = enabled) }
        scope.launch {
            runCatching { player.setVolumeNormalization(enabled) }
                .onFailure { logger.w(it) { "Couldn't change volume normalization" } }
        }
        saveLater()
    }

    // --- Sleep timer ------------------------------------------------------------------------

    fun setSleepTimer(choice: SleepTimerChoice) {
        sleepJob?.cancel()
        sleepJob = null
        val duration = choice.duration
        stateFlow.update {
            it.copy(
                sleepTimer = choice,
                sleepTimerDeadline = duration?.let { d -> TimeSource.Monotonic.markNow() + d }
            )
        }
        if (duration != null) {
            sleepJob = scope.launch {
                delay(duration)
                pauseForSleep(rewindToStart = false)
            }
        } else if (choice == SleepTimerChoice.EndOfTrack) {
            sleepJob = scope.launch {
                val startKey = queue.currentQueueEntryFlow.value?.trackKey()
                if (startKey == null) {
                    resetSleepTimer()
                    return@launch
                }
                // Fires when the queue moves on to another track, or playback runs out.
                combine(queue.currentQueueEntryFlow, player.playerStateFlow) { entry, state ->
                    state == PlayerState.COMPLETED || (entry != null && entry.trackKey() != startKey)
                }.first { it }
                // Unless the queue simply ended, the next track has started: stop it at its start.
                pauseForSleep(rewindToStart = player.playerStateFlow.value != PlayerState.COMPLETED)
            }
        }
    }

    private suspend fun pauseForSleep(rewindToStart: Boolean) {
        runCatching {
            player.pause()
            if (rewindToStart) player.seekTo(Duration.ZERO)
        }.onFailure { logger.w(it) { "Sleep timer couldn't pause playback" } }
        resetSleepTimer()
    }

    private fun resetSleepTimer() {
        stateFlow.update { it.copy(sleepTimer = SleepTimerChoice.Off, sleepTimerDeadline = null) }
    }

    // --- Persistence ------------------------------------------------------------------------

    private fun saveLater() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY)
            val snapshot = stateFlow.value
            val encoded = json.encodeToString(
                PersistedSound.serializer(),
                PersistedSound(bandsDb = snapshot.bandsDb, normalization = snapshot.normalization)
            )
            runCatching {
                database.settingsDataStore.edit { prefs -> prefs[SOUND_SETTINGS_KEY] = encoded }
            }.onFailure { logger.w(it) { "Couldn't save sound settings" } }
        }
    }

    private fun normalizeBands(bands: List<Float>): List<Float> =
        List(EqualizerBandsHz.size) { index ->
            bands.getOrElse(index) { 0f }.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
        }

    private fun presetFor(bands: List<Float>): EqPreset? =
        EqPreset.entries.firstOrNull { it.bandsDb == bands }

    /** Identifies "the same track" even if its stream URL is re-resolved. */
    private fun QueueEntry.trackKey(): String = when (this) {
        is QueueEntry.StreamingTrack -> "s:${track.id}"
        is QueueEntry.LocalTrack -> "l:$url"
    }
}

@Serializable
private data class PersistedSound(
    val bandsDb: List<Float> = EqPreset.Flat.bandsDb,
    val normalization: Boolean = false
)
