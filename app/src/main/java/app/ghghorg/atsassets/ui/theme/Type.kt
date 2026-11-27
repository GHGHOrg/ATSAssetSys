package app.ghghorg.atsassets.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.ghghorg.atsassets.R

// 44a step 4 (CR7; spec 6, Visual design (2); C27; design/decisions.md D-11 to D-13; 44a decision 2): Asap and Asap
// Condensed, shipped in res/font/ (design/fonts/fonts.json), never a system font. Asap is variable: each weight is set
// on its weight axis at the normal width (100). The sizes, line heights and weights come from the type tokens.

private val WEIGHTS = listOf(400, 500, 700)

private fun asap(
    weight: Int,
    italic: Boolean,
): Font {
    val style = if (italic) FontStyle.Italic else FontStyle.Normal
    return Font(
        resId = if (italic) R.font.asap_italic_variable else R.font.asap_variable,
        weight = FontWeight(weight),
        style = style,
        variationSettings = FontVariation.Settings(FontWeight(weight), style, FontVariation.width(100f)),
    )
}

internal val Asap = FontFamily(WEIGHTS.flatMap { listOf(asap(it, italic = false), asap(it, italic = true)) })

internal val AsapCondensed =
    FontFamily(
        Font(R.font.asap_condensed_regular, FontWeight.W400, FontStyle.Normal),
        Font(R.font.asap_condensed_italic, FontWeight.W400, FontStyle.Italic),
        Font(R.font.asap_condensed_medium, FontWeight.W500, FontStyle.Normal),
        Font(R.font.asap_condensed_medium_italic, FontWeight.W500, FontStyle.Italic),
        Font(R.font.asap_condensed_bold, FontWeight.W700, FontStyle.Normal),
        Font(R.font.asap_condensed_bold_italic, FontWeight.W700, FontStyle.Italic),
    )

private fun family(name: String): FontFamily = when (name) {
    "Asap" -> Asap
    "Asap Condensed" -> AsapCondensed
    else -> error("No font family $name in res/font/")
}

/** The text style of a type role from the tokens (base.tokens.json), with tabular digits so figures line up (D-12). */
internal fun typeStyle(role: String): TextStyle {
    val type = TokenBase.types.getValue(role)
    return TextStyle(
        fontFamily = family(type.family),
        fontWeight = FontWeight(type.weight),
        fontSize = type.size.sp,
        lineHeight = type.lineHeight.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )
}

// Material's styles, each set from a type role, so Material's own parts draw in Asap too.
val Typography =
    Typography(
        headlineMedium = typeStyle("bigFigure"),
        headlineSmall = typeStyle("bigFigureFallback"),
        titleLarge = typeStyle("screenTitle"),
        titleMedium = typeStyle("sectionHeading"),
        titleSmall = typeStyle("sectionHeading"),
        bodyLarge = typeStyle("body"),
        bodyMedium = typeStyle("small"),
        bodySmall = typeStyle("oneDay"),
        labelLarge = typeStyle("button"),
        labelMedium = typeStyle("tab"),
        labelSmall = typeStyle("oneDay"),
    )
