package app.ghghorg.atsassets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.tokenDp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 44a step 9 (phone check, 2026-11-28): Transaction history of a portfolio with many transactions crashed ATS Dev with
 * "Canvas: trying to draw too large(159692736bytes) bitmap". The card's shadow (Compose's dropShadow) was drawn into one
 * bitmap the size of the card, about 37,000 px tall there, and Android refuses a bitmap over 100 MB. Here a card 80,000 px
 * tall, in a scrolling column as on that screen, must draw without the crash, and still with its shadow: one pixel just
 * outside its left edge is the shadow, not the page (D-08).
 */
@RunWith(AndroidJUnit4::class)
class TallCardShadowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aCardTallerThanTheBitmapLimit_drawsWithItsShadow() {
        composeRule.setContent {
            ATSAssetSystemTheme {
                val tall = with(LocalDensity.current) { TALL_PX.toDp() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AtsTheme.colors.page)
                        .verticalScroll(rememberScrollState())
                        .padding(tokenDp("spaceMargin")),
                ) {
                    AtsCard(modifier = Modifier.testTag(CARD)) { Box(modifier = Modifier.fillMaxWidth().height(tall)) }
                }
            }
        }
        composeRule.waitForIdle()
        val card = composeRule.onNodeWithTag(CARD).fetchSemanticsNode().boundsInRoot
        val outside = composeRule.onRoot().colorAt(card.left - with(composeRule.density) { 1.dp.toPx() }, 200f)
        assertTrue(
            "1 dp left of the card ${describe(outside)}: the shadow, not the page ${describe(TokenColors.light.page)}",
            !sameColor(outside, TokenColors.light.page),
        )
    }

    private companion object {
        // Taller than Transaction history's card on the phone (about 37,000 px); at any emulator width, its bitmap would
        // pass Android's 100 MB limit.
        const val TALL_PX = 80_000
        const val CARD = "tall card"
    }
}
