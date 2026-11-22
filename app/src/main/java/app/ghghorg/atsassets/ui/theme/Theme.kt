package app.ghghorg.atsassets.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
     */
)

/**
 * The color of a gain or loss shown with sign [signum] (section 6): green above 0, red below, and the text's own color
 * at 0. Pass the sign of the shown, rounded figure, so a value shown as $0.00 has no color (4.10).
 */
@Composable
fun gainLossColor(signum: Int): Color {
    val dark = isSystemInDarkTheme()
    return when {
        signum > 0 -> if (dark) GainDark else GainLight
        signum < 0 -> if (dark) LossDark else LossLight
        else -> LocalContentColor.current
    }
}

/**
 * The color of an allocation slice (18): cash's own, otherwise the [index]th of the others in turn, counted without cash,
 * so a portfolio or ticker keeps its color whether cash is listed or not. Every row names its slice, so meaning never
 * rests on the color (section 6, accessibility (5)).
 */
@Composable
fun sliceColor(
    cash: Boolean,
    index: Int,
): Color {
    val dark = isSystemInDarkTheme()
    if (cash) return if (dark) SliceCashDark else SliceCashLight
    val slices = if (dark) SlicesDark else SlicesLight
    return slices[index % slices.size]
}

/** The percentage drawn on a slice: white on the light theme's dark slices, black on the dark theme's light ones. */
@Composable
fun sliceLabelColor(): Color = if (isSystemInDarkTheme()) Color.Black else Color.White

@Composable
fun ATSAssetSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
