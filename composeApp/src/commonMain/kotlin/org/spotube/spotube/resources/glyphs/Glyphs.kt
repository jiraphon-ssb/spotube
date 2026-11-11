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

package org.spotube.spotube.resources.glyphs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Single-colour playback and action glyphs for the redesigned chrome.
 *
 * The Iconsax set the app ships is duotone (a second, translucent layer), which turns muddy once a
 * single tint is applied. These are one-layer glyphs on a 24-unit grid: outline glyphs use a 1.8
 * stroke with round caps/joins; transport glyphs (play, pause, skip) are solid so the primary
 * action reads at a glance.
 *
 * Outline path data adapted from Lucide (https://lucide.dev), ISC License,
 * Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT);
 * all other copyright (c) for Lucide are held by Lucide Contributors 2022.
 */
object Glyphs {

    private const val STROKE = 1.8f

    private fun glyph(
        name: String,
        strokes: List<String> = emptyList(),
        fills: List<String> = emptyList()
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        fills.forEach { data ->
            addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black))
        }
        strokes.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

    // --- Transport (solid) ------------------------------------------------------------------

    val Play: ImageVector by lazy {
        glyph(
            "Play",
            fills = listOf("M7 4.8v14.4c0 .8.86 1.28 1.54.86l11.5-7.2a1 1 0 0 0 0-1.72L8.54 3.94C7.86 3.52 7 4 7 4.8z")
        )
    }

    val Pause: ImageVector by lazy {
        glyph(
            "Pause",
            fills = listOf(
                "M7 4h2a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
                "M15 4h2a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"
            )
        )
    }

    val SkipPrevious: ImageVector by lazy {
        glyph(
            "SkipPrevious",
            fills = listOf("M18.4 5.2l-8.8 6a1 1 0 0 0 0 1.6l8.8 6A1 1 0 0 0 20 18V6a1 1 0 0 0-1.6-.8z"),
            strokes = listOf("M5.5 5v14")
        )
    }

    val SkipNext: ImageVector by lazy {
        glyph(
            "SkipNext",
            fills = listOf("M5.6 5.2l8.8 6a1 1 0 0 1 0 1.6l-8.8 6A1 1 0 0 1 4 18V6a1 1 0 0 1 1.6-.8z"),
            strokes = listOf("M18.5 5v14")
        )
    }

    // --- Playback modes (outline) -----------------------------------------------------------

    val Shuffle: ImageVector by lazy {
        glyph(
            "Shuffle",
            strokes = listOf(
                "M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.7-1.1 2-1.7 3.3-1.7H22",
                "m18 2 4 4-4 4",
                "M2 6h1.9c1.5 0 2.9.9 3.6 2.2",
                "M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8",
                "m18 14 4 4-4 4"
            )
        )
    }

    val Repeat: ImageVector by lazy {
        glyph(
            "Repeat",
            strokes = listOf(
                "m17 2 4 4-4 4",
                "M3 11v-1a4 4 0 0 1 4-4h14",
                "m7 22-4-4 4-4",
                "M21 13v1a4 4 0 0 1-4 4H3"
            )
        )
    }

    val RepeatOne: ImageVector by lazy {
        glyph(
            "RepeatOne",
            strokes = listOf(
                "m17 2 4 4-4 4",
                "M3 11v-1a4 4 0 0 1 4-4h14",
                "m7 22-4-4 4-4",
                "M21 13v1a4 4 0 0 1-4 4H3",
                "M11 10h1v4"
            )
        )
    }

    // --- Actions (outline) ------------------------------------------------------------------

    val Lyrics: ImageVector by lazy {
        glyph(
            "Lyrics",
            strokes = listOf(
                "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
                "M8 12a2 2 0 0 0 2-2V8H8",
                "M14 12a2 2 0 0 0 2-2V8h-2"
            )
        )
    }

    val Download: ImageVector by lazy {
        glyph("Download", strokes = listOf("M12 15V3", "m7 10 5 5 5-5", "M5 21h14"))
    }

    val SwapSource: ImageVector by lazy {
        glyph(
            "SwapSource",
            strokes = listOf("M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")
        )
    }

    val More: ImageVector by lazy {
        glyph(
            "More",
            fills = listOf(
                "M5 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
                "M12 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
                "M19 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"
            )
        )
    }

    val Queue: ImageVector by lazy {
        glyph(
            "Queue",
            strokes = listOf(
                "M21 15V6",
                "M18.5 18a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
                "M12 12H3",
                "M16 6H3",
                "M12 18H3"
            )
        )
    }

    val Expand: ImageVector by lazy {
        glyph("Expand", strokes = listOf("M15 3h6v6", "m21 3-7 7", "m3 21 7-7", "M9 21H3v-6"))
    }

    val Collapse: ImageVector by lazy {
        glyph("Collapse", strokes = listOf("m14 10 7-7", "M20 10h-6V4", "m3 21 7-7", "M4 14h6v6"))
    }

    val AddTo: ImageVector by lazy {
        glyph(
            "AddTo",
            strokes = listOf("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M8 12h8", "M12 8v8")
        )
    }

    val UserBlock: ImageVector by lazy {
        glyph(
            "UserBlock",
            strokes = listOf(
                "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
                "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
                "m17 8 5 5",
                "m22 8-5 5"
            )
        )
    }

    val Sort: ImageVector by lazy {
        glyph("Sort", strokes = listOf("m3 16 4 4 4-4", "M7 20V4", "m21 8-4-4-4 4", "M17 4v16"))
    }

    val ChevronLeft: ImageVector by lazy { glyph("ChevronLeft", strokes = listOf("m15 18-6-6 6-6")) }

    val ChevronRight: ImageVector by lazy { glyph("ChevronRight", strokes = listOf("m9 18 6-6-6-6")) }

    // --- Navigation (outline) ---------------------------------------------------------------

    val Home: ImageVector by lazy {
        glyph(
            "Home",
            strokes = listOf(
                "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
                "M3 10a2 2 0 0 1 .7-1.5l7-6a2 2 0 0 1 2.6 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
            )
        )
    }

    val Search: ImageVector by lazy {
        glyph("Search", strokes = listOf("M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z", "m21 21-4.3-4.3"))
    }

    val Playlists: ImageVector by lazy {
        glyph(
            "Playlists",
            strokes = listOf(
                "M21 15V6",
                "M18.5 18a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
                "M12 12H3",
                "M16 6H3",
                "M12 18H3"
            )
        )
    }

    val Albums: ImageVector by lazy {
        glyph(
            "Albums",
            strokes = listOf(
                "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z",
                "M6 12c0-1.7.7-3.2 1.8-4.2",
                "M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z",
                "M18 12c0 1.7-.7 3.2-1.8 4.2"
            )
        )
    }

    val Artists: ImageVector by lazy {
        glyph(
            "Artists",
            strokes = listOf(
                "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
                "M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"
            )
        )
    }

    val LocalFiles: ImageVector by lazy {
        glyph(
            "LocalFiles",
            strokes = listOf(
                "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.7-.9l-.8-1.2A2 2 0 0 0 7.9 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2z",
                "M11 17.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z",
                "M11 17.5V11l4 1.2"
            )
        )
    }

    val Devices: ImageVector by lazy {
        glyph(
            "Devices",
            strokes = listOf(
                "M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6",
                "M2 12a9 9 0 0 1 8 8",
                "M2 16a5 5 0 0 1 4 4",
                "M2 20h.01"
            )
        )
    }

    val GroupJam: ImageVector by lazy {
        glyph(
            "GroupJam",
            strokes = listOf(
                "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
                "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
                "M22 21v-2a4 4 0 0 0-3-3.9",
                "M16 3.1a4 4 0 0 1 0 7.8"
            )
        )
    }

    val Heart: ImageVector by lazy {
        glyph(
            "Heart",
            strokes = listOf(
                "M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.8 0-3 .5-4.5 2-1.5-1.5-2.7-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7z"
            )
        )
    }

    val Settings: ImageVector by lazy {
        glyph(
            "Settings",
            strokes = listOf(
                "M12.2 2h-.4a2 2 0 0 0-2 2v.2a2 2 0 0 1-1 1.7l-.4.3a2 2 0 0 1-2 0l-.2-.1a2 2 0 0 0-2.7.7l-.2.4a2 2 0 0 0 .7 2.7l.2.1a2 2 0 0 1 1 1.7v.5a2 2 0 0 1-1 1.7l-.2.1a2 2 0 0 0-.7 2.7l.2.4a2 2 0 0 0 2.7.7l.2-.1a2 2 0 0 1 2 0l.4.3a2 2 0 0 1 1 1.7v.2a2 2 0 0 0 2 2h.4a2 2 0 0 0 2-2v-.2a2 2 0 0 1 1-1.7l.4-.3a2 2 0 0 1 2 0l.2.1a2 2 0 0 0 2.7-.7l.2-.4a2 2 0 0 0-.7-2.7l-.2-.1a2 2 0 0 1-1-1.7v-.5a2 2 0 0 1 1-1.7l.2-.1a2 2 0 0 0 .7-2.7l-.2-.4a2 2 0 0 0-2.7-.7l-.2.1a2 2 0 0 1-2 0l-.4-.3a2 2 0 0 1-1-1.7V4a2 2 0 0 0-2-2z",
                "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
            )
        )
    }

    val SidebarCollapse: ImageVector by lazy {
        glyph(
            "SidebarCollapse",
            strokes = listOf(
                "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
                "M9 3v18",
                "m16 15-3-3 3-3"
            )
        )
    }

    val SidebarExpand: ImageVector by lazy {
        glyph(
            "SidebarExpand",
            strokes = listOf(
                "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
                "M9 3v18",
                "m14 9 3 3-3 3"
            )
        )
    }

    // --- Volume (outline) -------------------------------------------------------------------

    private const val SPEAKER =
        "M11 4.7a.7.7 0 0 0-1.2-.5L6.4 7.6A1.4 1.4 0 0 1 5.4 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.4a1.4 1.4 0 0 1 1 .4l3.4 3.4a.7.7 0 0 0 1.2-.5z"

    val VolumeHigh: ImageVector by lazy {
        glyph("VolumeHigh", strokes = listOf(SPEAKER, "M16 9a5 5 0 0 1 0 6", "M19.4 18.4a9 9 0 0 0 0-12.8"))
    }

    val VolumeLow: ImageVector by lazy {
        glyph("VolumeLow", strokes = listOf(SPEAKER, "M16 9a5 5 0 0 1 0 6"))
    }

    val VolumeMute: ImageVector by lazy {
        glyph("VolumeMute", strokes = listOf(SPEAKER, "m22 9-6 6", "m16 9 6 6"))
    }
}
