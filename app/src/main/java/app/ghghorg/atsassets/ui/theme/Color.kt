package app.ghghorg.atsassets.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Gains green and losses red (section 6), one pair per theme (11 wording 3). Contrast worked out on Material's surface
// tones: at least 5:1 on the lightest and on the darkest container in each theme; checked by eye on the phone.
val GainLight = Color(0xFF1B6B1F)
val LossLight = Color(0xFFB3261E)
val GainDark = Color(0xFF7FD47A)
val LossDark = Color(0xFFFFB4AB)

// Allocation slices (18): cash its own color, the others in turn, no green or red (those mean gain and loss). Worked out
// with WCAG's formula on Material's baseline tones, not measured: light at least 4.5:1 on #FFFFFF and #E6E0E9, with a
// white label at least 5.8:1; dark at least 5.1:1 on #141218 and #36343B, with a black label at least 8.8:1.
val SliceCashLight = Color(0xFF00696F)
val SlicesLight = listOf(Color(0xFF3949AB), Color(0xFFA04F00), Color(0xFF7B1FA2), Color(0xFF01579B), Color(0xFF6D4C41), Color(0xFF5D6B00))
val SliceCashDark = Color(0xFF4DD0E1)
val SlicesDark = listOf(Color(0xFF9FA8DA), Color(0xFFFFB74D), Color(0xFFCE93D8), Color(0xFF81D4FA), Color(0xFFBCAAA4), Color(0xFFDCE775))
