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

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

/**
 * The history file: JSON lines (one [ListeningEvent] per line) in the app data dir. Appending is
 * cheap and crash-safe (a torn last line is skipped on read); [rewrite] compacts the file
 * atomically via a temp file.
 */
internal class ListeningHistoryStorage(
    private val file: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun readAll(): List<ListeningEvent> {
        if (!fileSystem.exists(file)) return emptyList()
        val text = fileSystem.read(file) { readUtf8() }
        return text.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> runCatching { json.decodeFromString<ListeningEvent>(line) }.getOrNull() }
            .toList()
    }

    fun append(event: ListeningEvent) {
        file.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.appendingSink(file).buffer().use { sink ->
            sink.writeUtf8(json.encodeToString(event))
            sink.writeUtf8("\n")
        }
    }

    fun rewrite(events: List<ListeningEvent>) {
        file.parent?.let { fileSystem.createDirectories(it) }
        val temp = "$file.tmp".toPath()
        fileSystem.sink(temp).buffer().use { sink ->
            events.forEach { event ->
                sink.writeUtf8(json.encodeToString(event))
                sink.writeUtf8("\n")
            }
        }
        fileSystem.atomicMove(temp, file)
    }
}
