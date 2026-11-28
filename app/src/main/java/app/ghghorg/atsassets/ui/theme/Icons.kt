package app.ghghorg.atsassets.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

// 44a step 5 (CR7; spec 6, Visual design (4); design/decisions.md D-19, D-46, D-47): the Lucide icons the app draws, one
// per file in design/icons/icons.json (IconFilesTest keeps the two the same).
internal enum class LucideIcon(
    val file: String,
    @param:DrawableRes val res: Int,
) {
    CHART_PIE("chart-pie", R.drawable.ic_lucide_chart_pie),
    BRIEFCASE("briefcase", R.drawable.ic_lucide_briefcase),
    WALLET("wallet", R.drawable.ic_lucide_wallet),
    ELLIPSIS("ellipsis", R.drawable.ic_lucide_ellipsis),
    CHEVRON_LEFT("chevron-left", R.drawable.ic_lucide_chevron_left),
    CHEVRON_RIGHT("chevron-right", R.drawable.ic_lucide_chevron_right),
    CHEVRON_DOWN("chevron-down", R.drawable.ic_lucide_chevron_down),
    CHEVRON_UP("chevron-up", R.drawable.ic_lucide_chevron_up),
    ELLIPSIS_VERTICAL("ellipsis-vertical", R.drawable.ic_lucide_ellipsis_vertical),
    PLUS("plus", R.drawable.ic_lucide_plus),
    X("x", R.drawable.ic_lucide_x),
    CALENDAR("calendar", R.drawable.ic_lucide_calendar),
    CHECK("check", R.drawable.ic_lucide_check),
    CIRCLE_EXCLAMATION_POINT("circle-exclamation-point", R.drawable.ic_lucide_circle_exclamation_point),
    CIRCLE_CHECK("circle-check", R.drawable.ic_lucide_circle_check),
    TRIANGLE_EXCLAMATION_POINT("triangle-exclamation-point", R.drawable.ic_lucide_triangle_exclamation_point),
    INFO("info", R.drawable.ic_lucide_info),
    LOADER_CIRCLE("loader-circle", R.drawable.ic_lucide_loader_circle),
    LOCK("lock", R.drawable.ic_lucide_lock),
    EYE("eye", R.drawable.ic_lucide_eye),
    EYE_OFF("eye-off", R.drawable.ic_lucide_eye_off),
    SETTINGS("settings", R.drawable.ic_lucide_settings),
    ARROW_UP("arrow-up", R.drawable.ic_lucide_arrow_up),
    ARROW_DOWN("arrow-down", R.drawable.ic_lucide_arrow_down),
}

/**
 * The Lucide file an icon draws, for the screen tests (AC-61 (e)) only. Accessibility services do not read this key; an
 * icon's meaning reaches TalkBack through its control's own label.
 */
internal val LucideIconKey = SemanticsPropertyKey<String>("LucideIcon")

internal var SemanticsPropertyReceiver.lucideIcon by LucideIconKey

/** The size of an icon from the tokens: 24 dp (D-19), 20 or 16 where the catalog makes it smaller. */
internal val iconSize: Dp get() = TokenBase.sizes.getValue("iconSize").dp

/** A Lucide icon in [tint] (one of D-46's icon colors), with no TalkBack text of its own. */
@Composable
internal fun AtsIcon(
    icon: LucideIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = iconSize,
) {
    Icon(
        painter = painterResource(icon.res),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size).semantics { lucideIcon = icon.file },
    )
}
