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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
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
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.ibm_plex_sans_thai_bold
import spotube.composeapp.generated.resources.ibm_plex_sans_thai_medium
import spotube.composeapp.generated.resources.ibm_plex_sans_thai_regular
import spotube.composeapp.generated.resources.ibm_plex_sans_thai_semibold

// ---------------------------------------------------------------------------------------------
// Tokens
// ---------------------------------------------------------------------------------------------

/**
 * The flat colour ramp the whole app is drawn from.
 *
 * Spotube's look is deliberately flat: no gel, bevel or glass gradients. Depth comes only from
 * the neutral ladder `canvas < panel < card < hover` and hairline [border]s.
 *
 * - [canvas]: the page behind scrolling content.
 * - [panel]: fixed chrome (sidebar, player panel, sheets, menus).
 * - [card]: anything on a panel or canvas that needs its own plane (cards, wells, selection).
 * - [hover]: pointer feedback and the brightest neutral.
 * - [accent]: accent for text and icons drawn on neutrals (contrast-checked on [canvas]).
 * - [accentFill]: accent for filled controls; [onAccentFill] is at least 4.5:1 on it.
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
    val success: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean
)

private val DarkNeutrals = SpotubeTokens(
    canvas = Color(0xFF111114),
    panel = Color(0xFF18181C),
    card = Color(0xFF1F1F24),
    hover = Color(0xFF27272D),
    border = Color(0xFF2A2A31),
    text = Color(0xFFEDEDF0),
    muted = Color(0xFF9696A0),
    accent = Color(0xFF8B94FF),
    accentFill = Color(0xFF525DEB),
    onAccentFill = Color.White,
    success = Color(0xFF6EE7A0),
    danger = Color(0xFFFF8A9B),
    scrim = Color(0xCC000000),
    isDark = true
)

private val LightNeutrals = SpotubeTokens(
    canvas = Color(0xFFF4F4F7),
    panel = Color(0xFFFFFFFF),
    card = Color(0xFFEFEFF3),
    hover = Color(0xFFE6E6EC),
    border = Color(0xFFDCDCE3),
    text = Color(0xFF17171C),
    muted = Color(0xFF5A5A66),
    accent = Color(0xFF3F4BD6),
    accentFill = Color(0xFF4F5AE8),
    onAccentFill = Color.White,
    success = Color(0xFF157F46),
    danger = Color(0xFFC0263D),
    scrim = Color(0x66000000),
    isDark = false
)

/** Resolves the token set for one accent family ([MillenniumPalette]) and appearance. */
fun spotubeTokens(palette: MillenniumPalette, dark: Boolean): SpotubeTokens {
    val base = if (dark) DarkNeutrals else LightNeutrals
    return when (palette) {
        MillenniumPalette.Blue -> base
        MillenniumPalette.Orange -> base.copy(
            accent = if (dark) Color(0xFFFDA769) else Color(0xFFB04A0A),
            accentFill = Color(0xFFFB923C),
            onAccentFill = Color(0xFF2A1204)
        )
        MillenniumPalette.Lime -> base.copy(
            accent = if (dark) Color(0xFFBEF264) else Color(0xFF4D7C0F),
            accentFill = Color(0xFFA3E635),
            onAccentFill = Color(0xFF172506)
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

private val SpotubeColorSchemes = MillenniumColorSchemes(
    light = spotubeTokens(MillenniumPalette.Blue, dark = false).toMillenniumColors(),
    dark = spotubeTokens(MillenniumPalette.Blue, dark = true).toMillenniumColors(),
    orangeLight = spotubeTokens(MillenniumPalette.Orange, dark = false).toMillenniumColors(),
    orangeDark = spotubeTokens(MillenniumPalette.Orange, dark = true).toMillenniumColors(),
    limeLight = spotubeTokens(MillenniumPalette.Lime, dark = false).toMillenniumColors(),
    limeDark = spotubeTokens(MillenniumPalette.Lime, dark = true).toMillenniumColors(),
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
    val hover: Color
)

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
        hover = t.hover
    )
}

/**
 * Kept for source compatibility: callers that still hold a [MillenniumColors] get the flat Blue
 * ramp of the same appearance.
 */
fun appColorsFrom(m: MillenniumColors): AppColors =
    appColorsFrom(spotubeTokens(MillenniumPalette.Blue, dark = m.background.luminance() < 0.5f))

// ---------------------------------------------------------------------------------------------
// Typography
// ---------------------------------------------------------------------------------------------

/** Named text styles used by the app, independent of Material typography. */
@Immutable
data class AppTypography(
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
    displaySmall = style(fontFamily, 36, 48, FontWeight.Bold),
    headlineLarge = style(fontFamily, 32, 44, FontWeight.Bold),
    headlineMedium = style(fontFamily, 28, 38, FontWeight.Bold),
    headlineSmall = style(fontFamily, 24, 34, FontWeight.SemiBold),
    titleLarge = style(fontFamily, 20, 30, FontWeight.SemiBold),
    titleMedium = style(fontFamily, 16, 24, FontWeight.SemiBold),
    titleSmall = style(fontFamily, 14, 21, FontWeight.Medium),
    bodyLarge = style(fontFamily, 16, 25),
    bodyMedium = style(fontFamily, 14, 22),
    bodySmall = style(fontFamily, 12, 18),
    labelLarge = style(fontFamily, 14, 21, FontWeight.Medium),
    labelMedium = style(fontFamily, 12, 18, FontWeight.Medium),
    labelSmall = style(fontFamily, 11, 16, FontWeight.Medium)
)

val AppDefaultTypography = appTypography(FontFamily.SansSerif)

/**
 * IBM Plex Sans Thai (SIL OFL 1.1): loopless Thai with IBM Plex Sans Latin in one family, so Thai
 * and English titles share metrics. Scripts it lacks (CJK, Arabic, ...) fall back to the platform.
 */
@Composable
fun rememberAppFontFamily(): FontFamily {
    val regular = Font(Res.font.ibm_plex_sans_thai_regular, FontWeight.Normal)
    val medium = Font(Res.font.ibm_plex_sans_thai_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.ibm_plex_sans_thai_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.ibm_plex_sans_thai_bold, FontWeight.Bold)
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
    staticCompositionLocalOf { appColorsFrom(spotubeTokens(MillenniumPalette.Blue, dark = false)) }
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
 * Installs [MillenniumTheme] with flattened palettes ([SpotubeColorSchemes]) and component themes
 * stripped of shadows, orbs, glows and rims ([flatComponents]), then provides the app's
 * [AppColors], [AppTypography] and [AppShapes].
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemDarkTheme()
    }
    val palette: MillenniumPalette = settings.palette
    val fontFamily = rememberAppFontFamily()

    // MillenniumTheme remembers its unstyled theme per config instance, so keep it stable.
    val config = remember(palette, fontFamily) {
        MillenniumThemeConfig(
            palette = palette,
            colorSchemes = SpotubeColorSchemes,
            dark = flatComponents(spotubeTokens(palette, dark = true), fontFamily),
            light = flatComponents(spotubeTokens(palette, dark = false), fontFamily)
        )
    }
    val tokens = remember(palette, isDarkTheme) { spotubeTokens(palette, isDarkTheme) }
    val colors = remember(tokens) { appColorsFrom(tokens) }
    val typography = remember(fontFamily) { appTypography(fontFamily) }

    MillenniumTheme(dark = isDarkTheme, config = config) {
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

/** Whether the active Millennium appearance is light. */
@Composable
fun isLightAppearance(): Boolean = MillenniumTheme.colors.background.luminance() > 0.5f

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
private fun flatComponents(t: SpotubeTokens, fontFamily: FontFamily): MillenniumThemeComponents {
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

    val button = base.buttonTheme.copy(
        primary = base.buttonTheme.primary.flatButton(),
        secondary = base.buttonTheme.secondary.flatButton(),
        quiet = base.buttonTheme.quiet.flatButton(),
        danger = base.buttonTheme.danger.flatButton()
    )

    val iconButton = base.iconButtonTheme.copy(
        primary = base.iconButtonTheme.primary.flatIconButton(),
        secondary = base.iconButtonTheme.secondary.flatIconButton(),
        quiet = base.iconButtonTheme.quiet.flatIconButton(),
        danger = base.iconButtonTheme.danger.flatIconButton()
    )

    val card = base.cardTheme.copy(
        states = base.cardTheme.states.mapStates { _, s ->
            s.copy(shadowElevation = 0.dp, innerGlowColor = clear, bottomRimColor = clear)
        }
    )

    val slider = base.sliderTheme.copy(
        states = base.sliderTheme.states.mapStates { _, s ->
            s.copy(
                trackBackground = SolidColor(t.hover),
                activeTrackBackground = SolidColor(t.text),
                thumbBackground = SolidColor(t.text),
                trackBorderWidth = 0.dp,
                thumbBorderWidth = 0.dp,
                thumbElevation = 0.dp,
                thumbGlowColor = clear,
                thumbBottomRimColor = clear,
                trackInsetShadeColor = clear
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
        group = sidebarBase.group.mapStates { _, s ->
            s.copy(
                labelStyle = s.labelStyle.copy(
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            )
        },
        item = sidebarBase.item.mapStates { state, s ->
            val flat = s.copy(
                labelStyle = s.labelStyle.copy(fontFamily = fontFamily),
                shadowElevation = 0.dp,
                innerGlowColor = clear,
                minimumHeight = 40.dp
            )
            when (state) {
                MillenniumControlState.Hovered -> flat.copy(
                    background = SolidColor(t.hover),
                    foreground = t.text,
                    borderColor = clear
                )
                MillenniumControlState.Pressed -> flat.copy(
                    background = SolidColor(t.card),
                    foreground = t.text,
                    borderColor = clear
                )
                MillenniumControlState.Focused -> flat.copy(
                    background = SolidColor(t.hover),
                    foreground = t.text,
                    borderColor = t.accent
                )
                MillenniumControlState.Selected -> flat.copy(
                    background = SolidColor(t.card),
                    foreground = t.text,
                    iconTint = t.accent,
                    borderColor = clear,
                    selectedIndicatorColor = t.accent
                )
                else -> flat.copy(borderColor = clear)
            }
        }
    )

    return base.copy(
        textTheme = MillenniumTextTheme(MillenniumControlStateProperty.all(text)),
        buttonTheme = button,
        iconButtonTheme = iconButton,
        cardTheme = card,
        sliderTheme = slider,
        sidebarTheme = sidebar,
        topAppBarTheme = MillenniumTopAppBarTheme(
            properties = base.topAppBarTheme.properties.copy(separatorColor = clear)
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
