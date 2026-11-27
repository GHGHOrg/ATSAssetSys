package app.ghghorg.atsassets.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

// 44a step 4 (CR7; spec 6, Visual design and Theme; K9; 44a decision 6): one light and one dark theme, built from the
// design tokens. Android's dynamic color is not used, so the wallpaper never changes a color (AC-40).

internal val LocalAtsColors = staticCompositionLocalOf { TokenColors.light }

/** The design tokens of the current theme (light or dark). */
internal object AtsTheme {
    val colors: TokenPalette
        @Composable get() = LocalAtsColors.current
}

/** The color scheme Material's parts draw with, made of the tokens only (ThemeTokensTest). */
internal fun atsColorScheme(
    p: TokenPalette,
    dark: Boolean,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.gold,
        onPrimary = p.onGold,
        primaryContainer = p.chipOn,
        onPrimaryContainer = p.onChipOn,
        inversePrimary = p.text,
        secondary = p.buttonText,
        onSecondary = p.page,
        secondaryContainer = p.card,
        onSecondaryContainer = p.text,
        tertiary = p.accent,
        onTertiary = p.page,
        tertiaryContainer = p.card,
        onTertiaryContainer = p.text,
        background = p.page,
        onBackground = p.text,
        surface = p.page,
        onSurface = p.text,
        surfaceVariant = p.card,
        onSurfaceVariant = p.textSecondary,
        surfaceTint = p.page,
        inverseSurface = p.text,
        inverseOnSurface = p.page,
        error = p.error,
        onError = p.page,
        errorContainer = p.card,
        onErrorContainer = p.error,
        outline = p.fieldLine,
        outlineVariant = p.divider,
        scrim = p.scrim,
        surfaceBright = p.card,
        surfaceDim = p.page,
        surfaceContainer = p.card,
        surfaceContainerHigh = p.card,
        surfaceContainerHighest = p.float,
        surfaceContainerLow = p.card,
        surfaceContainerLowest = p.page,
    )
}

private fun atsShapes(): Shapes {
    val corner = RoundedCornerShape(TokenBase.sizes.getValue("radiusDefault").dp)
    return Shapes(
        extraSmall = RoundedCornerShape(TokenBase.sizes.getValue("radiusCheckbox").dp),
        small = corner,
        medium = corner,
        large = corner,
        extraLarge = corner,
    )
}

@Composable
fun ATSAssetSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) TokenColors.dark else TokenColors.light
    CompositionLocalProvider(LocalAtsColors provides palette) {
        MaterialTheme(
            colorScheme = atsColorScheme(palette, darkTheme),
            typography = Typography,
            shapes = atsShapes(),
            content = content,
        )
    }
}
