package app.ghghorg.atsassets

import android.os.ParcelFileDescriptor
import android.util.Log
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
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.tokenDp
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

/**
 * 44a step 9 (phone check, 2026-11-28): Transaction history of a portfolio with many transactions crashed ATS Dev with
 * "Canvas: trying to draw too large(159692736bytes) bitmap". Compose's dropShadow (ui-graphics 1.12.1,
 * DropShadowRenderer.createOuterShadowBitmap) draws a card's shadow into one ALPHA_8 bitmap, one byte a pixel, as large as
 * the card plus twice the blur: on the phone 1,344 x 118,819 px, past Android's limit (100 MB, or the device's
 * ro.hwui.max_texture_allocation_size if larger). This test reads the device's limit and width and draws a card tall
 * enough for such a bitmap to be twice the limit, in a scrolling column as on that screen. It must draw without the
 * crash, and still with its shadow: one pixel just outside its left edge is the shadow, not the page (D-08). A first
 * version with a fixed 80,000 px card passed on the emulator before the fix (run 46145458458), so the height comes from
 * the device now.
 */
@RunWith(AndroidJUnit4::class)
class TallCardShadowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aCardTallerThanTheBitmapLimit_drawsWithItsShadow() {
        val limit = maxOf(DEFAULT_LIMIT, deviceLimit())
        val width = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.widthPixels
        val tallPx = ceil(limit * 2.0 / width).toLong()
        Log.i(TAG, "bitmap limit $limit bytes (device property ${deviceLimit()}), screen width $width px, card $tallPx px")
        if (tallPx > MAX_LAYOUT_PX) fail("The card would need $tallPx px, more than Compose lays out ($MAX_LAYOUT_PX); limit $limit")
        composeRule.setContent {
            ATSAssetSystemTheme {
                val tall = with(LocalDensity.current) { tallPx.toFloat().toDp() }
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

    // The device's ro.hwui.max_texture_allocation_size in bytes, 0 when it is not set.
    private fun deviceLimit(): Long {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("getprop ro.hwui.max_texture_allocation_size")
        val text = ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }.trim()
        return text.toLongOrNull() ?: 0L
    }

    private companion object {
        const val TAG = "TallCardShadowTest"

        // Android's RecordingCanvas: 100 MB unless the device sets more.
        const val DEFAULT_LIMIT = 100L * 1024 * 1024

        // The tallest a node can be laid out next to a width under 8,192 px (Compose's Constraints).
        const val MAX_LAYOUT_PX = 262_143L
        const val CARD = "tall card"
    }
}
