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

package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.spotube.spotube.isDesktop
import org.spotube.spotube.getPlatform
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.spotube.millennium.composables.MillenniumButtonMotion
import org.spotube.millennium.composables.MillenniumButtonThemeProperties
import org.spotube.millennium.composables.MillenniumIconButtonThemeProperties
import org.spotube.millennium.composables.MillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.MillenniumModalBottomSheetThemeProperties
import org.spotube.millennium.composables.MillenniumTextTheme
import org.spotube.millennium.composables.MillenniumTooltipHost
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.defaultMillenniumModalBottomSheetTheme
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeComponents
import org.spotube.millennium.theme.MillenniumThemeConfig
import org.spotube.millennium.theme.defaultMillenniumThemeComponents
import org.spotube.spotube.modules.settings.UserSettings
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.anuphan_bold
import spotube.composeapp.generated.resources.anuphan_medium
import spotube.composeapp.generated.resources.anuphan_regular
import spotube.composeapp.generated.resources.anuphan_semibold

// ---------------------------------------------------------------------------------------------
// Tokens
// ---------------------------------------------------------------------------------------------

/**
 * The colour ramp the whole app is drawn from: "Glass Night".
 *
 * A cool blue-black canvas, panels and cards as faint white glass over it (so the artwork-tinted
 * light behind headers shows through), cool white text and one ice-blue accent. Depth comes from
 * the ladder `canvas < panel < card < hover` and hairline [border]s; colour comes from the
 * artwork, not the chrome. [accentStart] and [accentEnd] are the same colour: no gradients.
 *
 * - [canvas]: the page behind scrolling content.
 * - [panel]: fixed chrome (sidebar, player panel, sheets, menus).
 * - [card]: anything on a panel or canvas that needs its own plane (cards, wells, selection).
 * - [hover]: pointer feedback and the brightest neutral.
 * - [accent]: accent for text and icons drawn on neutrals (contrast-checked on [canvas]).
 * - [accentFill]: accent for filled controls; [onAccentFill] is at least 4.5:1 on it.
 * - [accentStart]/[accentEnd]: the stops of [accentBrush]; kept equal so accent fills stay flat.
 */
@Immutable
data class SpotubeTokens(
    val canvas: Color,
    val panel: Color,
    val card: Color,
    val hover: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentFill: Color,
    val onAccentFill: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val success: Color,
    val danger: Color,
    val scrim: Color,
    /** The cool light artwork tints are pulled toward, so header glows never turn warm or muddy. */
    val ambient: Color = Color(0xFF3F7FBF)
)

/**
 * Resting edge for text fields: ~3.8:1 on [SpotubeTokens.canvas], so the field boundary meets
 * WCAG 1.4.11 (the glass fill alone is ~1.1:1).
 */
private val FieldEdge = Color(0xFF646D7B)

// Glass Night. Text is ~17:1 on the canvas, muted ~7.5:1; the ice accent ~11:1 on the canvas and
// the navy ink on the accent fill ~11:1. Card/hover/border are white at low alpha (glass).
private val DarkNeutrals = SpotubeTokens(
    canvas = Color(0xFF080B10),
    panel = Color(0xFF0E1218),
    card = Color(0x10FFFFFF),
    hover = Color(0x18FFFFFF),
    border = Color(0x17FFFFFF),
    text = Color(0xFFEEF2F7),
    muted = Color(0xFF97A1B0),
    accent = Color(0xFF9CC2FF),
    accentFill = Color(0xFF9CC2FF),
    onAccentFill = Color(0xFF0A1222),
    accentStart = Color(0xFF9CC2FF),
    accentEnd = Color(0xFF9CC2FF),
    success = Color(0xFF6EE7B7),
    danger = Color(0xFFFF7A85),
    scrim = Color(0xCC000000)
)

/**
 * Resolves the token set for one accent family ([MillenniumPalette]). Spotube has a single,
 * dark appearance; the palette only swaps the accent.
 */
fun spotubeTokens(palette: MillenniumPalette): SpotubeTokens {
    val base = DarkNeutrals
    // Palette slots keep Millennium's enum names; the looks are Ice (default slot), Aqua and
    // Silver. All cool, all flat, all with dark ink on the fill.
    return when (palette) {
        MillenniumPalette.Blue -> base
        MillenniumPalette.Orange -> base.copy(
            accent = Color(0xFF7DE8DA),
            accentFill = Color(0xFF6FE3D6),
            onAccentFill = Color(0xFF04201C),
            accentStart = Color(0xFF6FE3D6),
            accentEnd = Color(0xFF6FE3D6),
            ambient = Color(0xFF2C8F9A)
        )
        MillenniumPalette.Lime -> base.copy(
            accent = Color(0xFFDDE3EA),
            accentFill = Color(0xFFDDE3EA),
            onAccentFill = Color(0xFF0B0E12),
            accentStart = Color(0xFFDDE3EA),
            accentEnd = Color(0xFFDDE3EA),
            ambient = Color(0xFF5A6B80)
        )
    }
}

/**
 * Millennium draws its gel/bevel/panel gradients between palette roles. Collapsing those roles onto
 * one colour ([SpotubeTokens.panel] for every neutral, [SpotubeTokens.accentFill] for both gel
 * endpoints, the panel colour for specular highlights) turns every gradient into a solid fill.
 */
fun SpotubeTokens.toMillenniumColors(): MillenniumColors = MillenniumColors(
    background = canvas,
    surface = panel,
    raised = panel,
    inset = panel,
    text = text,
    muted = muted,
    border = border,
    highlight = panel,
    accent = accent,
    accentTop = accentFill,
    accentBottom = accentFill,
    onAccent = onAccentFill,
    success = success,
    danger = danger,
    scrim = scrim
)

// Millennium keeps a light and a dark role set per palette; both slots get the one dark ramp so
// nothing can ever resolve to a light surface.
private val SpotubeColorSchemes = MillenniumColorSchemes(
    light = spotubeTokens(MillenniumPalette.Blue).toMillenniumColors(),
    dark = spotubeTokens(MillenniumPalette.Blue).toMillenniumColors(),
    orangeLight = spotubeTokens(MillenniumPalette.Orange).toMillenniumColors(),
    orangeDark = spotubeTokens(MillenniumPalette.Orange).toMillenniumColors(),
    limeLight = spotubeTokens(MillenniumPalette.Lime).toMillenniumColors(),
    limeDark = spotubeTokens(MillenniumPalette.Lime).toMillenniumColors(),
)

// ---------------------------------------------------------------------------------------------
// App-level semantic roles
// ---------------------------------------------------------------------------------------------

/** Semantic colour roles used across the app, derived from the active [SpotubeTokens]. */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color,
    /** Filled accent surface (primary buttons, the player's control deck). */
    val accentFill: Color,
    /** Content drawn on [accentFill]; 4.5:1 or better. */
    val onAccentFill: Color,
    /** Pointer-hover / pressed neutral. */
    val hover: Color,
    /** The flat accent fill as a [Brush] (its stops are equal), for filled accent controls. */
    val accentBrush: Brush,
    /** [accentBrush] one notch brighter, for hover. */
    val accentBrushHover: Brush,
    /** [accentBrush] one notch darker, for press. */
    val accentBrushPressed: Brush,
    /** The cool light that artwork-tinted glows are pulled toward. */
    val ambient: Color
)

/** The accent fill as a brush (flat: [SpotubeTokens.accentStart] == [SpotubeTokens.accentEnd]), optionally nudged lighter/darker. */
fun SpotubeTokens.accentBrush(lighten: Float = 0f, darken: Float = 0f): Brush {
    fun Color.adjust() = when {
        lighten > 0f -> lerp(this, Color.White, lighten)
        darken > 0f -> lerp(this, Color.Black, darken)
        else -> this
    }
    return Brush.horizontalGradient(listOf(accentStart.adjust(), accentEnd.adjust()))
}

/**
 * An artwork tint made safe for this theme: pulled part-way toward the cool [ambient] light so a
 * warm or muddy cover still lights the header in the app's cool register.
 */
fun AppColors.coolGlow(artworkTint: Color, amount: Float = 0.4f): Color = lerp(artworkTint, ambient, amount)

/** Maps the flat token ramp onto the app's role names. */
fun appColorsFrom(t: SpotubeTokens): AppColors {
    val onAccent = if (t.accent.luminance() > 0.4f) Color(0xFF0B0B14) else Color.White
    return AppColors(
        primary = t.accent,
        onPrimary = onAccent,
        primaryContainer = t.card,
        onPrimaryContainer = t.text,
        secondary = t.accent,
        onSecondary = onAccent,
        secondaryContainer = t.card,
        onSecondaryContainer = t.text,
        tertiaryContainer = t.card,
        onTertiaryContainer = t.text,
        background = t.canvas,
        onBackground = t.text,
        surface = t.panel,
        onSurface = t.text,
        surfaceVariant = t.card,
        onSurfaceVariant = t.muted,
        surfaceContainer = t.panel,
        surfaceContainerHigh = t.card,
        surfaceContainerHighest = t.hover,
        inverseSurface = t.text,
        inverseOnSurface = t.canvas,
        outline = t.border,
        outlineVariant = t.border,
        error = t.danger,
        onError = t.canvas,
        errorContainer = t.danger.copy(alpha = 0.16f),
        onErrorContainer = t.danger,
        scrim = t.scrim,
        accentFill = t.accentFill,
        onAccentFill = t.onAccentFill,
        hover = t.hover,
        accentBrush = t.accentBrush(),
        accentBrushHover = t.accentBrush(lighten = 0.12f),
        accentBrushPressed = t.accentBrush(darken = 0.12f),
        ambient = t.ambient
    )
}

// ---------------------------------------------------------------------------------------------
// Typography
// ---------------------------------------------------------------------------------------------

/** Named text styles used by the app, independent of Material typography. */
@Immutable
data class AppTypography(
    /** Hero titles only (playlist/album/artist name): big, tight, one per screen. */
    val displayLarge: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

private fun style(
    fontFamily: FontFamily,
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight
)

/**
 * The app type scale. Thai stacks vowels and tone marks above and below the baseline, so leading is
 * roughly 1.5x at body sizes (vs. ~1.3x for Latin-only scales) to keep marks from colliding.
 */
fun appTypography(fontFamily: FontFamily) = AppTypography(
    displayLarge = style(fontFamily, 64, 76, FontWeight.Bold).copy(letterSpacing = (-1.8).sp),
    displaySmall = style(fontFamily, 36, 48, FontWeight.Bold).copy(letterSpacing = (-0.6).sp),
    headlineLarge = style(fontFamily, 32, 44, FontWeight.Bold),
    headlineMedium = style(fontFamily, 28, 38, FontWeight.Bold),
    headlineSmall = style(fontFamily, 24, 34, FontWeight.SemiBold),
    titleLarge = style(fontFamily, 20, 28, FontWeight.SemiBold).copy(letterSpacing = (-0.2).sp),
    titleMedium = style(fontFamily, 16, 24, FontWeight.SemiBold),
    titleSmall = style(fontFamily, 14, 21, FontWeight.Medium),
    bodyLarge = style(fontFamily, 15, 23),
    bodyMedium = style(fontFamily, 14, 22),
    bodySmall = style(fontFamily, 12, 18),
    labelLarge = style(fontFamily, 14, 21, FontWeight.Medium),
    labelMedium = style(fontFamily, 12, 18, FontWeight.Medium),
    labelSmall = style(fontFamily, 11, 16, FontWeight.Medium)
)

val AppDefaultTypography = appTypography(FontFamily.SansSerif)

/**
 * Anuphan (Cadson Demak, SIL OFL 1.1): loopless Thai with a matching Latin, so Thai and English
 * titles share metrics. Scripts it lacks (CJK, Arabic, ...) fall back to the platform.
 */
@Composable
fun rememberAppFontFamily(): FontFamily {
    val regular = Font(Res.font.anuphan_regular, FontWeight.Normal)
    val medium = Font(Res.font.anuphan_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.anuphan_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.anuphan_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) {
        FontFamily(regular, medium, semiBold, bold)
    }
}

// ---------------------------------------------------------------------------------------------
// Shapes
// ---------------------------------------------------------------------------------------------

/** Corner shapes used by the app. */
@Immutable
data class AppShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape
)

val AppDefaultShapes = AppShapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

val LocalAppColors =
    staticCompositionLocalOf { appColorsFrom(spotubeTokens(MillenniumPalette.Blue)) }
val LocalAppTypography = staticCompositionLocalOf { AppDefaultTypography }
val LocalAppShapes = staticCompositionLocalOf { AppDefaultShapes }

/** Reads the active app theme tokens. */
object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val typography: AppTypography @Composable get() = LocalAppTypography.current
    val shapes: AppShapes @Composable get() = LocalAppShapes.current
}

// ---------------------------------------------------------------------------------------------
// Theme root
// ---------------------------------------------------------------------------------------------

/**
 * The app's single source of truth for colour and type.
 *
 * Installs [MillenniumTheme] (always dark) with flattened palettes ([SpotubeColorSchemes]) and component themes
 * stripped of shadows, orbs, glows and rims ([flatComponents]), then provides the app's
 * [AppColors], [AppTypography] and [AppShapes].
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val palette: MillenniumPalette = settings.palette
    val fontFamily = rememberAppFontFamily()

    // MillenniumTheme remembers its unstyled theme per config instance, so keep it stable.
    val config = remember(palette, fontFamily) { spotubeMillenniumConfig(palette, fontFamily) }
    val tokens = remember(palette) { spotubeTokens(palette) }
    val colors = remember(tokens) { appColorsFrom(tokens) }
    val typography = remember(fontFamily) { appTypography(fontFamily) }

    MillenniumTheme(dark = true, config = config) {
        MillenniumTooltipHost {
            CompositionLocalProvider(
                LocalAppColors provides colors,
                LocalAppTypography provides typography,
                LocalAppShapes provides AppDefaultShapes,
                content = content
            )
        }
    }
}

/**
 * The Millennium configuration Spotube runs with for [palette]: flat dark role sets and the
 * flattened component themes. Also used by previews (e.g. the palette picker) so they show the
 * real look rather than Millennium's stock gel styling.
 */
fun spotubeMillenniumConfig(
    palette: MillenniumPalette,
    fontFamily: FontFamily,
    desktop: Boolean = getPlatform().isDesktop()
): MillenniumThemeConfig {
    val components = flatComponents(spotubeTokens(palette), fontFamily, desktop)
    return MillenniumThemeConfig(
        palette = palette,
        colorSchemes = SpotubeColorSchemes,
        dark = components,
        light = components
    )
}

// ---------------------------------------------------------------------------------------------
// Flat component themes
// ---------------------------------------------------------------------------------------------

private inline fun <T> MillenniumControlStateProperty<T>.mapStates(
    transform: (MillenniumControlState, T) -> T
): MillenniumControlStateProperty<T> = MillenniumControlStateProperty(
    default = transform(MillenniumControlState.Default, default),
    hovered = transform(MillenniumControlState.Hovered, hovered),
    focused = transform(MillenniumControlState.Focused, focused),
    pressed = transform(MillenniumControlState.Pressed, pressed),
    disabled = transform(MillenniumControlState.Disabled, disabled),
    selected = transform(MillenniumControlState.Selected, selected),
    error = transform(MillenniumControlState.Error, error)
)

private fun MillenniumControlStateProperty<MillenniumButtonThemeProperties>.flatButton() =
    mapStates { _, s ->
        s.copy(
            shadows = emptyList(),
            shadowElevation = 0.dp,
            innerGlowColor = Color.Transparent,
            bottomRimColor = Color.Transparent
        )
    }

/** Desktop button metrics: 36dp pill, 14sp semibold label. */
private fun MillenniumButtonThemeProperties.compactButton(shape: androidx.compose.ui.graphics.Shape) = copy(
    minSize = DpSize(64.dp, 36.dp),
    padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    shape = shape,
    textStyle = textStyle.copy(fontSize = 14.sp, lineHeight = 20.sp)
)

private fun MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>.flatIconButton() =
    mapStates { _, s ->
        s.copy(
            orb = false,
            shadows = emptyList(),
            shadowElevation = 0.dp,
            innerGlowColor = Color.Transparent,
            bottomRimColor = Color.Transparent
        )
    }

/**
 * Millennium's defaults with every skeuomorphic layer removed: drop shadows, glass orbs, inner
 * glows and bottom rims. Focus rings are kept (they are the keyboard affordance).
 */
private fun flatComponents(t: SpotubeTokens, fontFamily: FontFamily, desktop: Boolean): MillenniumThemeComponents {
    val base = defaultMillenniumThemeComponents()
    val clear = Color.Transparent

    val text = base.textTheme.states.default.let { s ->
        s.copy(
            body = s.body.copy(fontFamily = fontFamily, lineHeight = 22.sp),
            label = s.label.copy(fontFamily = fontFamily),
            button = s.button.copy(fontFamily = fontFamily),
            title = s.title.copy(fontFamily = fontFamily, lineHeight = 30.sp),
            headline = s.headline.copy(fontFamily = fontFamily, lineHeight = 44.sp),
            caption = s.caption.copy(fontFamily = fontFamily, lineHeight = 18.sp),
            avatar = s.avatar.copy(fontFamily = fontFamily)
        )
    }

    // Hover language, shared by every control: the fill steps one notch (lighter on hover,
    // darker on press), nothing grows, glows or gains a coloured ring. Keyboard focus is a cool
    // white ring ([SpotubeTokens.text]) everywhere: an accent ring vanishes on accent fills.
    val borderHover = t.text.copy(alpha = 0.22f)
    val still = MillenniumButtonMotion(hoverScale = 1f, pressedScale = 0.98f)

    val button = base.buttonTheme.copy(
        primary = base.buttonTheme.primary.flatButton().mapStates { state, s ->
            s.copy(
                background = when (state) {
                    MillenniumControlState.Hovered -> t.accentBrush(lighten = 0.12f)
                    MillenniumControlState.Pressed -> t.accentBrush(darken = 0.12f)
                    else -> t.accentBrush()
                },
                foreground = t.onAccentFill,
                borderColor = if (state == MillenniumControlState.Focused) t.text else clear,
                borderWidth = if (state == MillenniumControlState.Focused) 2.dp else 0.dp,
                motion = still
            )
        },
        secondary = base.buttonTheme.secondary.flatButton().mapStates { state, s ->
            s.copy(
                background = SolidColor(
                    when (state) {
                        MillenniumControlState.Hovered -> t.hover
                        else -> t.card
                    }
                ),
                borderColor = when (state) {
                    MillenniumControlState.Focused -> t.text
                    MillenniumControlState.Hovered -> borderHover
                    else -> t.border
                },
                borderWidth = if (state == MillenniumControlState.Focused) 2.dp else 1.dp,
                motion = still
            )
        },
        quiet = base.buttonTheme.quiet.flatButton().mapStates { state, s ->
            when (state) {
                MillenniumControlState.Hovered -> s.copy(background = SolidColor(t.hover), borderWidth = 0.dp, motion = still)
                MillenniumControlState.Pressed -> s.copy(background = SolidColor(t.card), borderWidth = 0.dp, motion = still)
                MillenniumControlState.Focused -> s.copy(borderColor = t.text, motion = still)
                else -> s.copy(motion = still)
            }
        },
        danger = base.buttonTheme.danger.flatButton().mapStates { state, s ->
            if (state == MillenniumControlState.Focused) s.copy(borderColor = t.text) else s
        }
    )

    val iconStill = MillenniumButtonMotion(hoverScale = 1f, pressedScale = 0.94f)
    val iconButton = base.iconButtonTheme.copy(
        primary = base.iconButtonTheme.primary.flatIconButton().mapStates { state, s ->
            s.copy(
                background = when (state) {
                    MillenniumControlState.Hovered -> t.accentBrush(lighten = 0.12f)
                    MillenniumControlState.Pressed -> t.accentBrush(darken = 0.12f)
                    else -> t.accentBrush()
                },
                foreground = t.onAccentFill,
                borderWidth = if (state == MillenniumControlState.Focused) 2.dp else 0.dp,
                borderColor = if (state == MillenniumControlState.Focused) t.text else clear,
                motion = iconStill
            )
        },
        secondary = base.iconButtonTheme.secondary.flatIconButton().mapStates { state, s ->
            s.copy(
                background = SolidColor(if (state == MillenniumControlState.Hovered) t.hover else t.card),
                borderColor = when (state) {
                    MillenniumControlState.Focused -> t.text
                    MillenniumControlState.Hovered -> borderHover
                    else -> t.border
                },
                motion = iconStill
            )
        },
        // Quiet = toolbar buttons: a flat neutral plate on hover/press, no ring or tint.
        // Keyboard focus keeps its ring.
        quiet = base.iconButtonTheme.quiet.flatIconButton().mapStates { state, s ->
            when (state) {
                MillenniumControlState.Hovered -> s.copy(background = SolidColor(t.hover), borderWidth = 0.dp, motion = iconStill)
                MillenniumControlState.Pressed -> s.copy(background = SolidColor(t.card), borderWidth = 0.dp, motion = iconStill)
                MillenniumControlState.Selected -> s.copy(background = SolidColor(t.card), borderWidth = 0.dp, motion = iconStill)
                MillenniumControlState.Focused -> s.copy(background = SolidColor(t.hover), borderColor = t.text, motion = iconStill)
                else -> s.copy(borderWidth = 0.dp, motion = iconStill)
            }
        },
        danger = base.iconButtonTheme.danger.flatIconButton().mapStates { state, s ->
            if (state == MillenniumControlState.Focused) s.copy(borderColor = t.text) else s
        }
    )

    val card = base.cardTheme.copy(
        states = base.cardTheme.states.mapStates { _, s ->
            s.copy(shadowElevation = 0.dp, innerGlowColor = clear, bottomRimColor = clear)
        }
    )

    val slider = base.sliderTheme.copy(
        states = base.sliderTheme.states.mapStates { state, s ->
            // Thin track + small round thumb (Millennium default is a 10dp gel track with a
            // 24dp rounded-square thumb). The 44dp touch target is kept.
            val flat = s.copy(
                trackShape = CircleShape,
                thumbShape = CircleShape,
                trackHeight = 4.dp,
                thumbSize = 12.dp,
                activeThumbSize = 16.dp,
                trackBackground = SolidColor(t.hover),
                activeTrackBackground = t.accentBrush(),
                thumbBackground = SolidColor(t.text),
                trackBorderWidth = 0.dp,
                thumbBorderWidth = 0.dp,
                thumbElevation = 0.dp,
                thumbGlowColor = clear,
                thumbBottomRimColor = clear,
                trackInsetShadeColor = clear
            )
            // Keyboard focus: the solid dot becomes a cool-white ring around a dark centre, which
            // reads on both the accent fill and the empty track. (MillenniumSlider lays the thumb
            // out in a box of the Default state's thumbSize, so a bigger focused thumb would be
            // clipped to 12dp; the ring is the visible change.)
            if (state == MillenniumControlState.Focused) {
                flat.copy(
                    thumbBackground = SolidColor(t.canvas),
                    thumbBorderColor = t.text,
                    thumbBorderWidth = 2.dp
                )
            } else {
                flat
            }
        }
    )

    // Checkboxes: Millennium's focused face border is the accent, which disappears on a checked
    // (accent-filled) face; use the cool white focus ring instead.
    val checkbox = base.checkboxTheme.copy(
        states = base.checkboxTheme.states.mapStates { state, s ->
            if (state == MillenniumControlState.Focused) s.copy(faceBorderColor = t.text) else s
        }
    )
    val triStateCheckbox = base.triStateCheckboxTheme.copy(
        states = base.triStateCheckboxTheme.states.mapStates { state, s ->
            if (state == MillenniumControlState.Focused) s.copy(faceBorderColor = t.text) else s
        }
    )

    // List rows (track lists, queue, menus): flat hover plate instead of an accent-tinted
    // gradient + border; the current/selected row sits on the card colour.
    val listTile = base.listTileTheme.copy(
        states = base.listTileTheme.states.mapStates { state, s ->
            val plate = when (state) {
                MillenniumControlState.Hovered, MillenniumControlState.Focused -> t.hover
                MillenniumControlState.Pressed -> t.card
                MillenniumControlState.Error -> t.danger.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            s.copy(
                background = SolidColor(plate),
                selectedBackground = SolidColor(
                    if (state == MillenniumControlState.Hovered || state == MillenniumControlState.Pressed) t.hover
                    else t.card
                ),
                shape = RoundedCornerShape(10.dp),
                borderColor = if (state == MillenniumControlState.Focused) t.text else clear,
                innerGlowColor = clear
            )
        }
    )

    // Menus: flat bordered panel. Millennium paints the active row with its accent gel and
    // tints the row icon onAccent; the desktop menu (AdaptiveDropdownBottomSheet) re-maps those
    // roles to the hover plate and text colour so a hovered row is a neutral plate, not an
    // ice-blue bar. Foreground is left to Millennium so destructive rows keep their red label.
    val dropdown = base.dropdownMenuTheme.copy(
        states = base.dropdownMenuTheme.states.mapStates { state, s ->
            s.copy(
                panelBackground = SolidColor(t.panel),
                panelBorderColor = t.border,
                panelBorderWidth = 1.dp,
                panelShadowElevation = 0.dp,
                panelGlowColor = clear,
                itemBorderColor = if (state == MillenniumControlState.Focused) t.text else clear,
                itemGlowColor = clear
            )
        }
    )

    // Segmented buttons: Millennium resolves Hovered before Selected, so the fill is left to its
    // own selected/unselected logic (now flat) and hover shows as a brighter edge only.
    val segmented = base.segmentedButtonTheme.copy(
        states = base.segmentedButtonTheme.states.mapStates { state, s ->
            s.copy(
                borderColor = when (state) {
                    MillenniumControlState.Focused -> t.text
                    MillenniumControlState.Hovered -> borderHover
                    else -> t.border
                },
                innerGlowColor = clear,
                motion = still
            )
        }
    )

    val tabs = base.tabGroupTheme.copy(
        states = base.tabGroupTheme.states.mapStates { state, s ->
            s.copy(
                listBackground = SolidColor(t.panel),
                listBorderColor = t.border,
                selectedTabBackground = SolidColor(t.hover),
                unselectedTabBackground = SolidColor(
                    when (state) {
                        MillenniumControlState.Hovered, MillenniumControlState.Pressed -> t.card
                        else -> Color.Transparent
                    }
                ),
                selectedTabForeground = t.text,
                unselectedTabForeground = when (state) {
                    MillenniumControlState.Hovered, MillenniumControlState.Pressed -> t.text
                    else -> t.muted
                },
                tabBorderWidth = 0.dp,
                tabElevation = 0.dp,
                tabGlowColor = clear
            )
        }
    )

    val sidebarBase = base.sidebarTheme
    val sidebar = sidebarBase.copy(
        root = sidebarBase.root.mapStates { _, s ->
            s.copy(
                background = SolidColor(t.panel),
                width = 240.dp,
                borderWidth = 0.dp,
                shadowElevation = 0.dp,
                innerGlowColor = clear
            )
        },
        // The nav list is short; an always-visible scrollbar track just reads as a stray line.
        // It still scrolls with the wheel/trackpad on short windows.
        content = sidebarBase.content.mapStates { _, s -> s.copy(showScrollbar = false) },
        header = sidebarBase.header.mapStates { _, s -> s.copy(dividerWidth = 0.dp) },
        // Groups: quiet 12sp labels, no bordered label row, a little air above each group.
        group = sidebarBase.group.mapStates { state, s ->
            s.copy(
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 2.dp),
                labelStyle = s.labelStyle.copy(
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                labelPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                labelMinimumHeight = 28.dp,
                labelBorderWidth = 0.dp,
                labelHoveredBackground = SolidColor(t.hover),
                labelForeground = if (state == MillenniumControlState.Hovered || state == MillenniumControlState.Focused) {
                    t.text
                } else {
                    t.muted
                }
            )
        },
        // Items: resting labels a step softer than the selected one, outline icons in the
        // muted colour; selection = the hover plate with a white label and icon, no edge bar.
        item = sidebarBase.item.mapStates { state, s ->
            val flat = s.copy(
                labelStyle = s.labelStyle.copy(fontFamily = fontFamily),
                shadowElevation = 0.dp,
                innerGlowColor = clear,
                minimumHeight = if (desktop) 36.dp else 44.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                iconSize = 19.dp,
                iconLabelSpacing = 12.dp,
                borderColor = clear,
                selectedIndicatorColor = clear,
                selectedIndicatorWidth = 0.dp
            )
            val resting = t.text.copy(alpha = 0.78f)
            when (state) {
                MillenniumControlState.Hovered -> flat.copy(
                    background = SolidColor(t.hover),
                    foreground = t.text,
                    iconTint = t.text
                )
                MillenniumControlState.Pressed -> flat.copy(
                    background = SolidColor(t.card),
                    foreground = t.text,
                    iconTint = t.text
                )
                MillenniumControlState.Focused -> flat.copy(
                    background = SolidColor(t.hover),
                    foreground = t.text,
                    iconTint = t.text,
                    borderColor = t.text
                )
                MillenniumControlState.Selected -> flat.copy(
                    background = SolidColor(t.hover),
                    foreground = t.text,
                    iconTint = t.text
                )
                MillenniumControlState.Disabled -> flat.copy(foreground = t.muted, iconTint = t.muted)
                else -> flat.copy(foreground = resting, iconTint = t.muted)
            }
        }
    )

    // Desktop scale (mouse, not thumbs): Millennium's 44–56dp touch metrics read as chunky toys
    // on a desktop window. Phones keep the touch-friendly defaults.
    val pill = RoundedCornerShape(percent = 50)
    val buttonM = if (!desktop) button else button.copy(
        primary = button.primary.mapStates { _, s -> s.compactButton(pill) },
        secondary = button.secondary.mapStates { _, s -> s.compactButton(pill) },
        quiet = button.quiet.mapStates { _, s -> s.compactButton(pill) },
        danger = button.danger.mapStates { _, s -> s.compactButton(pill) }
    )
    val iconButtonM = if (!desktop) iconButton else iconButton.copy(
        primary = iconButton.primary.mapStates { _, s -> s.copy(size = DpSize(36.dp, 36.dp), iconSize = 20.dp) },
        secondary = iconButton.secondary.mapStates { _, s -> s.copy(size = DpSize(36.dp, 36.dp), iconSize = 20.dp) },
        quiet = iconButton.quiet.mapStates { _, s -> s.copy(size = DpSize(36.dp, 36.dp), iconSize = 20.dp) },
        danger = iconButton.danger.mapStates { _, s -> s.copy(size = DpSize(36.dp, 36.dp), iconSize = 20.dp) }
    )
    val textField = base.textFieldTheme.copy(
        states = base.textFieldTheme.states.mapStates { state, s ->
            val flat = s.copy(
                background = SolidColor(t.card),
                // A visible resting edge (WCAG 1.4.11): the glass fill alone is ~1.1:1.
                borderColor = when (state) {
                    MillenniumControlState.Focused -> t.accent
                    MillenniumControlState.Error -> t.danger
                    else -> FieldEdge
                },
                insetShadeColor = clear,
                innerGlowColor = clear,
                bottomRimColor = clear
            )
            if (desktop) {
                flat.copy(
                    minimumHeight = 36.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                flat
            }
        }
    )
    val listTileM = if (!desktop) listTile else listTile.copy(
        states = listTile.states.mapStates { _, s ->
            s.copy(minimumHeight = 52.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
        }
    )
    val segmentedM = if (!desktop) segmented else segmented.copy(
        states = segmented.states.mapStates { _, s ->
            s.copy(minimumHeight = 32.dp, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp))
        }
    )

    return base.copy(
        textTheme = MillenniumTextTheme(MillenniumControlStateProperty.all(text)),
        buttonTheme = buttonM,
        iconButtonTheme = iconButtonM,
        textFieldTheme = textField,
        cardTheme = card,
        sliderTheme = slider,
        checkboxTheme = checkbox,
        triStateCheckboxTheme = triStateCheckbox,
        sidebarTheme = sidebar,
        listTileTheme = listTileM,
        dropdownMenuTheme = dropdown,
        segmentedButtonTheme = segmentedM,
        tabGroupTheme = tabs,
        // No band behind the top bar: Millennium falls back to its opaque panel brush when
        // background is null, so set it explicitly transparent (the theme has one state only).
        topAppBarTheme = MillenniumTopAppBarTheme(
            properties = base.topAppBarTheme.properties.copy(
                background = SolidColor(clear),
                separatorColor = clear
            )
        ),
        modalBottomSheetTheme = compactModalBottomSheetTheme()
    )
}

/** Library default is 24dp padding + 16dp spacing, which is far too roomy on phones. */
private fun compactModalBottomSheetTheme(): MillenniumModalBottomSheetTheme {
    val padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
    fun MillenniumModalBottomSheetThemeProperties.compact() =
        copy(contentPadding = padding, contentSpacing = 8.dp)
    val states = defaultMillenniumModalBottomSheetTheme().states
    return MillenniumModalBottomSheetTheme(
        states = MillenniumControlStateProperty(
            default = states.default.compact(),
            hovered = states.hovered.compact(),
            focused = states.focused.compact(),
            pressed = states.pressed.compact(),
            disabled = states.disabled.compact(),
            selected = states.selected.compact(),
            error = states.error.compact()))
}
