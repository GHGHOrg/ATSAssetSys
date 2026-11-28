package app.ghghorg.atsassets

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.labelRes
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.AsapCondensed
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.TokenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bottom bar's look (44a step 5; spec 6, Visual design (5); AC-61 (e); D-37): each tab draws its Lucide icon and its
 * name in Asap Condensed 14, gold and bold when selected; the bar is at least 72 dp tall in the bar color; in light the
 * selected tab's pill is behind its icon, in dark a gold line is under it. Also the first proof that the AC-61 (e)
 * helpers (text layout, icon key, captured pixels) work on the runner's emulator.
 */
@RunWith(AndroidJUnit4::class)
class BottomBarDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val selected = Tab.OVERALL

    private fun show(dark: Boolean) {
        composeRule.setContent { ATSAssetSystemTheme(darkTheme = dark) { BottomBar(DEFAULT_TAB_ORDER, selected, {}) } }
        composeRule.waitForIdle()
    }

    private fun palette(dark: Boolean): TokenPalette = if (dark) TokenColors.dark else TokenColors.light

    @Test
    fun eachTab_drawsItsLucideIcon_inTheTabOrder() {
        show(dark = false)
        val expected = DEFAULT_TAB_ORDER.map {
            when (it) {
                Tab.MORE -> "ellipsis"
                Tab.CASH -> "wallet"
                Tab.PORTFOLIOS -> "briefcase"
                Tab.OVERALL -> "chart-pie"
            }
        }
        assertEquals(expected, composeRule.drawnIcons().map { it.first })
    }

    private fun checkNames(dark: Boolean) {
        show(dark)
        val colors = palette(dark)
        for (tab in DEFAULT_TAB_ORDER) {
            val style = composeRule.onNode(hasText(string(tab.labelRes)), useUnmergedTree = true).laidOutStyle()
            val isSelected = tab == selected
            assertEquals("$tab family", AsapCondensed, style.fontFamily)
            assertEquals("$tab size", 14.sp, style.fontSize)
            assertEquals("$tab weight", if (isSelected) FontWeight.W700 else FontWeight.W500, style.fontWeight)
            val ink = if (isSelected) colors.barSelected else colors.barLabel
            assertTrue("$tab color ${describe(style.color)}, expected ${describe(ink)}", sameColor(style.color, ink, tolerance = 0))
        }
    }

    @Test
    fun tabNames_areAsapCondensed14_goldAndBoldWhenSelected_light() = checkNames(dark = false)

    @Test
    fun tabNames_areAsapCondensed14_goldAndBoldWhenSelected_dark() = checkNames(dark = true)

    private fun checkSurfaces(dark: Boolean) {
        show(dark)
        val colors = palette(dark)
        val density = composeRule.density
        val root = composeRule.onRoot()
        val bar = root.fetchSemanticsNode().boundsInRoot
        with(density) { assertTrue("The bar is at least 72 dp tall: ${bar.height.toDp()}", bar.height >= 72.dp.toPx() - 0.5f) }

        // A flat area of a tab that is not selected: the bar's color.
        val other = composeRule.onNode(isSelectable() and hasText(string(Tab.MORE.labelRes))).fetchSemanticsNode().boundsInRoot
        val barColor = root.colorAt(other.left + 3f, other.top + 3f)
        assertTrue("Bar ${describe(barColor)}, expected ${describe(colors.bar)}", sameColor(barColor, colors.bar))

        // Just above the selected tab's icon, inside its pill's top padding: the pill in light, the bar in dark.
        val icon = composeRule.drawnIcons().first { it.first == "chart-pie" }.second
        val abovePill = root.colorAt(icon.center.x, icon.top - with(density) { 3.dp.toPx() })
        val pill = if (dark) colors.bar else colors.barPill
        assertTrue("Above the icon ${describe(abovePill)}, expected ${describe(pill)}", sameColor(abovePill, pill))

        // The middle of the line under the selected tab: gold in dark, the bar in light.
        val name = composeRule.onNode(hasText(string(selected.labelRes)), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val lineMiddle = name.bottom + with(density) { (4.dp + 3.dp + 1.5.dp).toPx() }
        val line = root.colorAt(icon.center.x, lineMiddle)
        val expectedLine = if (dark) colors.barLine else colors.bar
        assertTrue("The line ${describe(line)}, expected ${describe(expectedLine)}", sameColor(line, expectedLine))
    }

    @Test
    fun barAndPill_areTheTokens_light() = checkSurfaces(dark = false)

    @Test
    fun barAndLine_areTheTokens_dark() = checkSurfaces(dark = true)
}
