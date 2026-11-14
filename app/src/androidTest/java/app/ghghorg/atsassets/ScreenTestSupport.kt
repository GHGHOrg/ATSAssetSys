package app.ghghorg.atsassets

import android.app.KeyguardManager
import android.app.UiAutomation
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import org.junit.rules.ExternalResource
import java.util.concurrent.atomic.AtomicInteger

// Screen tests (AC-61; CR3, iteration 15b; measured in plan/spikes/screen-tests.md, section 11): what the screen tests share.

/** The fixed throwaway test PIN (plan/spikes/screen-tests.md 11.3, S10): it unlocks only the test emulator, never a phone. */
internal const val TEST_PIN = "135790"

internal typealias MainRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

internal fun string(
    @StringRes id: Int,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

/** Runs [command] in the emulator's shell and waits for it to finish. */
internal fun shell(command: String): String {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(output).use { it.bufferedReader().readText() }
}

/** True once [condition] holds, false if it does not within [timeoutMs]. */
internal fun poll(
    timeoutMs: Long,
    condition: () -> Boolean,
): Boolean {
    val end = SystemClock.uptimeMillis() + timeoutMs
    while (!condition()) {
        if (SystemClock.uptimeMillis() >= end) return false
        SystemClock.sleep(100)
    }
    return true
}

internal fun waitFor(
    what: String,
    timeoutMs: Long = 10_000,
    condition: () -> Boolean,
) {
    if (!poll(timeoutMs, condition)) fail("Timed out after $timeoutMs ms waiting for $what")
}

private fun typePin() {
    shell("input text $TEST_PIN")
    shell("input keyevent KEYCODE_ENTER")
}

/**
 * Keeps the emulator's screen on, wakes it and enters the test PIN on its lock screen, which Android asks for once after
 * each boot and again after the screen sleeps (plan/spikes/screen-tests.md 11.4, series 2). The screen setting is put
 * back afterwards.
 */
class DeviceUnlockedRule : ExternalResource() {
    private var previousStayOn = ""

    override fun before() {
        val keyguard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(KeyguardManager::class.java)
        previousStayOn = shell("settings get global stay_on_while_plugged_in").trim()
        shell("svc power stayon true")
        shell("input keyevent KEYCODE_WAKEUP")
        if (!keyguard.isKeyguardLocked) return
        shell("wm dismiss-keyguard")
        SystemClock.sleep(1_000)
        typePin()
        waitFor("the emulator's lock screen to take the test PIN") { !keyguard.isKeyguardLocked }
    }

    override fun after() {
        val value = if (previousStayOn.isEmpty() || previousStayOn == "null") "0" else previousStayOn
        shell("settings put global stay_on_while_plugged_in $value")
    }
}

/**
 * Sets the system font scale before the activity starts, so it opens at that scale, and afterwards puts it back and frees
 * the rotation [turn] fixed, also when the test fails. It waits for the scale to be back before the next test starts:
 * without the wait, the change reached the next test about 2 s in and Android re-created its activity (open item 53).
 */
class DeviceSettingsRule(
    private val fontScale: Float,
) : ExternalResource() {
    private var previousFontScale = ""

    override fun before() {
        previousFontScale = shell("settings get system font_scale").trim()
        shell("settings put system font_scale $fontScale")
        waitFor("font scale $fontScale") { Resources.getSystem().configuration.fontScale == fontScale }
    }

    override fun after() {
        // With no setting stored, Android uses the scale 1.0 (my reading).
        val restored = previousFontScale.toFloatOrNull() ?: 1.0f
        if (previousFontScale.isEmpty() || previousFontScale == "null") {
            shell("settings delete system font_scale")
        } else {
            shell("settings put system font_scale $previousFontScale")
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        waitFor("font scale $restored again") { Resources.getSystem().configuration.fontScale == restored }
    }
}

/**
 * Turns the emulator once the test's own activity is on top: before it, the launcher was on top and held the screen
 * upright (plan/spikes/screen-tests.md 11.4, series 3). Waits until the display and the activity both have the
 * orientation. Any activity's rule, MainActivity's included (29 step 10).
 */
internal fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.turn(landscape: Boolean) {
    InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(
        if (landscape) UiAutomation.ROTATION_FREEZE_90 else UiAutomation.ROTATION_FREEZE_0,
    )
    val orientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
    val rotation = if (landscape) Surface.ROTATION_90 else Surface.ROTATION_0
    val display =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
    waitFor("display rotation $rotation and the activity in orientation $orientation") {
        display.rotation == rotation && runOnUiThread { activity.resources.configuration.orientation } == orientation
    }
}

internal fun MainRule.windowFocused(): Boolean = runOnUiThread { activity.hasWindowFocus() }

private fun MainRule.unlocked(): Boolean = windowFocused() &&
    onAllNodes(isSelectable() and hasText(string(R.string.tab_portfolios))).fetchSemanticsNodes().isNotEmpty()

/** The Enter key of the system's PIN pad in MainActivity's lock prompt (BiometricPrompt, Android 17). */
private const val PIN_PAD_ENTER = "com.android.systemui:id/key_enter"

/**
 * Walks the active window's nodes: findAccessibilityNodeInfosByViewId did not find the key in the Compose PIN pad
 * (plan/spikes/screen-tests.md 11.6, the change after run 3, found by a local run).
 */
private fun pinPadUp(): Boolean = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let { hasViewId(it, PIN_PAD_ENTER) } == true

private fun hasViewId(
    node: AccessibilityNodeInfo,
    id: String,
): Boolean = node.viewIdResourceName == id || (0 until node.childCount).any { i -> node.getChild(i)?.let { hasViewId(it, id) } == true }

/**
 * Types the test PIN into MainActivity's lock prompt once its PIN pad is up, after clearing what an earlier attempt left;
 * retried. On GitHub's runner a PIN typed before the pad was ready was lost (plan/spikes/screen-tests.md 11.6, run 3).
 */
internal fun MainRule.unlockApp() {
    repeat(3) { attempt ->
        waitFor("the lock prompt's PIN pad ($PIN_PAD_ENTER)", 15_000) { pinPadUp() }
        shell("input keyevent " + List(8) { "KEYCODE_DEL" }.joinToString(" "))
        typePin()
        if (poll(5_000) { unlocked() }) return
        saveScreen("unlock-${SystemClock.uptimeMillis()}-$attempt")
    }
    fail("MainActivity did not unlock with the test PIN")
}

/**
 * Passes a check that is not the unlock, such as the screenshot window's (21): types the test PIN once the PIN pad is up
 * and waits until it closes; retried as [unlockApp] is.
 */
internal fun passCheck() {
    repeat(3) { attempt ->
        waitFor("the check's PIN pad ($PIN_PAD_ENTER)", 15_000) { pinPadUp() }
        shell("input keyevent " + List(8) { "KEYCODE_DEL" }.joinToString(" "))
        typePin()
        if (poll(5_000) { !pinPadUp() }) return
        saveScreen("check-${SystemClock.uptimeMillis()}-$attempt")
    }
    fail("The check did not take the test PIN")
}

/**
 * After [turn] on MainActivity: the turn relaunches it, and under the emulator's auto-lock choice (none stored, so
 * every leave) its onStop locks the app, so the lock prompt comes up (29 step 10, red run 43477719319). Passes it if
 * its PIN pad shows within 5 s; the screen under it keeps its state (the ViewModel).
 */
internal fun passLockAfterTurn() {
    if (poll(5_000) { pinPadUp() }) passCheck()
}

/**
 * What the screen showed when the unlock failed (plan/spikes/screen-tests.md 11.6, run 2): a screenshot in
 * /data/local/tmp, which the workflow pulls, and the focused window and the active window's nodes in logcat under
 * ScreenTestUnlock.
 */
private fun saveScreen(name: String) {
    shell("screencap -p /data/local/tmp/screen-test-$name.png")
    shell("dumpsys window").lines()
        .filter { it.contains("mCurrentFocus") || it.contains("mFocusedApp") || it.contains("Keyguard", ignoreCase = true) }
        .forEach { Log.i("ScreenTestUnlock", "$name window: ${it.trim()}") }
    val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
    if (root == null) Log.i("ScreenTestUnlock", "$name: no active window") else logNode(name, root, 0)
}

private fun logNode(
    name: String,
    node: AccessibilityNodeInfo,
    depth: Int,
) {
    val text = listOfNotNull(node.className, node.viewIdResourceName, node.text, node.contentDescription).joinToString(" | ")
    Log.i("ScreenTestUnlock", "$name node: ${"  ".repeat(depth)}$text${if (node.isFocused) " [focused]" else ""}")
    for (i in 0 until node.childCount) node.getChild(i)?.let { logNode(name, it, depth + 1) }
}

/**
 * Runs [action], then waits [ms]: MainActivity's window never loses the focus meanwhile, so the cover does not replace
 * [what] (07). A listener, not polling: a popup that took the focus lived about 150 ms (plan/spikes/screen-tests.md 11.4,
 * the 07b defect runs). The wait lets Compose draw (waitForIdle), or a popup opened by [action] would appear only in the
 * next step.
 */
internal fun MainRule.assertFocusHeld(
    what: String,
    ms: Long = 1_500,
    action: () -> Unit,
) {
    val losses = AtomicInteger(0)
    val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (!focused) losses.incrementAndGet() }
    val observer = runOnUiThread { activity.window.decorView.viewTreeObserver.also { it.addOnWindowFocusChangeListener(listener) } }
    val failure = runCatching(action).exceptionOrNull() ?: runCatching { idleFor(ms) }.exceptionOrNull()
    runOnUiThread { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    if (losses.get() > 0) {
        val message = "MainActivity's window lost the focus ${losses.get()} time(s) while $what was open, so the cover screen replaces it (07)"
        throw AssertionError(message, failure)
    }
    failure?.let { throw it }
}

private fun MainRule.idleFor(ms: Long) {
    val end = SystemClock.uptimeMillis() + ms
    while (SystemClock.uptimeMillis() < end) {
        waitForIdle()
        SystemClock.sleep(100)
    }
}

internal fun nameField(): SemanticsMatcher = hasSetTextAction() and hasText(string(R.string.portfolio_name_label))

internal fun button(
    @StringRes id: Int,
): SemanticsMatcher = hasText(string(id)) and hasClickAction()

/** From a fresh or used app: the opening balance if it is missing, then New portfolio's layer from the Portfolios tab. */
internal fun MainRule.openNewPortfolioLayer() {
    val setOpening = string(R.string.guided_set_opening_balance)
    if (onAllNodesWithText(setOpening).fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText(setOpening).performClick()
        onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
        onNode(button(R.string.review_button)).performScrollTo().performClick()
        onNode(button(R.string.confirm_button)).performScrollTo().performClick()
        waitUntil(10_000) { onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
    }
    onNode(isSelectable() and hasText(string(R.string.tab_portfolios))).performClick()
    val create = button(R.string.create_portfolio) or button(R.string.new_portfolio)
    waitUntil(5_000) { onAllNodes(create).fetchSemanticsNodes().isNotEmpty() }
    onNode(create).performScrollTo().performClick()
    waitUntil(5_000) { onAllNodes(nameField()).fetchSemanticsNodes().isNotEmpty() }
}

private fun MainRule.imeBottom(): Int = runOnUiThread {
    val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
    if (insets?.isVisible(WindowInsetsCompat.Type.ime()) == true) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
}

/** The keyboard's top edge in window pixels, once it is open and has stopped moving. */
internal fun MainRule.keyboardTop(): Float {
    var previous = -1
    waitFor("the soft keyboard to open and stop moving") {
        val current = imeBottom()
        val settled = current > 0 && current == previous
        previous = current
        if (!settled) SystemClock.sleep(300)
        settled
    }
    val windowHeight = runOnUiThread { activity.window.decorView.height }
    return (windowHeight - previous).toFloat()
}

/**
 * As [unlockApp], for AppStartTimingTest (iteration 15c): types the test PIN and returns the uptime at which the lock
 * prompt closed. MainActivity starts loading its data then, so the time to the first screen is measured from here.
 * [unlockApp] returns only once the bottom bar shows, after the data has loaded.
 */
internal fun MainRule.unlockAppTimed(): Long {
    repeat(3) { attempt ->
        waitFor("the lock prompt's PIN pad ($PIN_PAD_ENTER)", 15_000) { pinPadUp() }
        shell("input keyevent " + List(8) { "KEYCODE_DEL" }.joinToString(" "))
        typePin()
        val end = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < end) {
            if (!pinPadUp()) return SystemClock.uptimeMillis()
            SystemClock.sleep(20)
        }
        saveScreen("unlock-timed-${SystemClock.uptimeMillis()}-$attempt")
    }
    throw AssertionError("MainActivity's lock prompt did not take the test PIN")
}
