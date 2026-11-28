package app.ghghorg.atsassets

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.ghghorg.atsassets.backup.DocumentBackupTarget
import app.ghghorg.atsassets.backup.DocumentRestoreFile
import app.ghghorg.atsassets.core.BackupReminder
import app.ghghorg.atsassets.data.DatabaseKey
import app.ghghorg.atsassets.data.RoomRecordStore
import app.ghghorg.atsassets.data.SqlCipherOpener
import app.ghghorg.atsassets.lock.AppLock
import app.ghghorg.atsassets.lock.LockHolder
import app.ghghorg.atsassets.lock.LockState
import app.ghghorg.atsassets.lock.ScreenshotWindow
import app.ghghorg.atsassets.prices.NetworkStatus
import app.ghghorg.atsassets.prices.finnhub.FinnhubSource
import app.ghghorg.atsassets.ui.AppContent
import app.ghghorg.atsassets.ui.AppViewModel
import app.ghghorg.atsassets.ui.RestoreControls
import app.ghghorg.atsassets.ui.ScreenshotControls
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID

// 20: how often the idle time is checked while the app is in view; a touch also checks it first.
private const val IDLE_CHECK_MS = 1_000L

// 31: the type asked of Android's file screen for the backup file (spec section 8: one CSV text file).
private const val BACKUP_FILE_TYPE = "text/csv"

// 31: this activity's requests to another screen; any number in the lower 16 bits will do. 33b: the restore file's.
private const val BACKUP_FILE_REQUEST = 31
private const val RESTORE_FILE_REQUEST = 33

// 33b: the restore file may be any file (section 8: its name and extension do not matter).
private const val RESTORE_FILE_TYPE = "*/*"

class MainActivity : FragmentActivity() {
    // Kept across a re-creation by LockHolder; set first in onCreate.
    private lateinit var lock: AppLock
    private lateinit var screenshots: ScreenshotWindow
    private var lockState by mutableStateOf(LockState.Locked)
    private var windowFocused by mutableStateOf(false)

    // 21 decision 2: one prompt for the lock and the screenshot window, each with its own title.
    private lateinit var prompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var screenshotPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var viewModel: AppViewModel

    // Stops watching the network; set while the app is in view (16a decision 2).
    private var stopWatchingNetwork: () -> Unit = {}

    // 20: the idle check while the app is in view, every IDLE_CHECK_MS from onStart to onStop (risk 4).
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleCheck =
        object : Runnable {
            override fun run() {
                lock.onIdleCheck(SystemClock.elapsedRealtime())
                showLockIfLocked()
                idleHandler.postDelayed(this, IDLE_CHECK_MS)
            }
        }

    // 21: while the screenshot window is on, the time left on each second, and the window's end at exactly 0 (risk 4).
    private val screenshotTick = Runnable { applyScreenshotWindow() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val holder = ViewModelProvider(this)[LockHolder::class.java]
        lock = holder.lock
        screenshots = holder.screenshots
        lockState = lock.state
        // Default privacy (spec section 9): blocked screenshots and recording, hidden recents preview.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setRecentsScreenshotEnabled(false)
        enableEdgeToEdge()

        // Created here, not later, so its callback is attached again after a recreation.
        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val now = SystemClock.elapsedRealtime()
                    // 21 decision 2: a check the window asked for is the window's, never the lock's.
                    if (screenshots.onCheckPassed(now, appUnlocked = lock.state == LockState.Unlocked)) {
                        applyScreenshotWindow()
                        return
                    }
                    lock.onAuthSucceeded(now)
                    lockState = lock.state
                    // Here, not on every composition: a turn of the phone that keeps the app unlocked (20) is no unlock.
                    if (lockState == LockState.Unlocked) {
                        viewModel.start()
                        viewModel.onUnlocked()
                    }
                }

                // Dismissed or error. One wrong attempt is onAuthenticationFailed, not this.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    screenshots.onCheckEnded()
                    lock.onAuthCancelledOrFailed()
                    lockState = lock.state
                }
            },
        )
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.prompt_title))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        screenshotPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.screenshot_prompt_title))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        viewModel = ViewModelProvider(this, viewModelFactory())[AppViewModel::class.java]
        // 20: the stored choice applies to the lock once loaded and at each change; until then the strictest rule holds.
        lifecycleScope.launch {
            viewModel.state.map { it.autoLock }.distinctUntilChanged().collect { choice -> choice?.let(lock::setAutoLock) }
        }

        setContent {
            ATSAssetSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (lockState) {
                        // Cover while the window is not focused (for example recents), so the
                        // live card shows no content. Experiment after phone check 7.
                        LockState.Unlocked -> if (windowFocused) {
                            AppContent(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding),
                                screenshots = ScreenshotControls(::allowScreenshots, ::turnOffScreenshots),
                                onBackUpNow = ::backUpNow,
                                restore = RestoreControls(onChooseFile = ::chooseRestoreFile, onCopyReport = ::copyReport),
                            )
                        } else {
                            CoverScreen(modifier = Modifier.padding(innerPadding))
                        }

                        LockState.Locked -> LockedScreen(
                            onUnlock = { showPromptIfNeeded() },
                            modifier = Modifier.padding(innerPadding),
                        )

                        LockState.NoScreenLock -> NoScreenLockScreen(
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        windowFocused = hasFocus
    }

    override fun onStart() {
        super.onStart()
        val canAuthenticate = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        lock.updateScreenLockAvailable(canAuthenticate)
        // 20: the time away counts as idle (decision 1).
        lock.onAppReturned(SystemClock.elapsedRealtime())
        lockState = lock.state
        // 31: the last backup is shown in the zone the phone is in now (C15).
        viewModel.onAppShown()
        showPromptIfNeeded()
        idleHandler.postDelayed(idleCheck, IDLE_CHECK_MS)
        // 21: a window kept across a turn of the phone goes on in the new activity; otherwise it is off.
        applyScreenshotWindow()
        // When the phone comes back online, a refresh that failed runs again (16a decision 2).
        // 33b: and when it goes offline, Restore is inactive (AC-08).
        stopWatchingNetwork =
            NetworkStatus(applicationContext).watch(Handler(Looper.getMainLooper()), onLost = { viewModel.onOffline() }) { viewModel.onOnline() }
    }

    override fun onStop() {
        idleHandler.removeCallbacks(idleCheck)
        idleHandler.removeCallbacks(screenshotTick)
        stopWatchingNetwork()
        stopWatchingNetwork = {}
        prompt.cancelAuthentication()
        // 20: the phone locked with the app in view when the screen is already off; no broadcast is needed for that.
        val screenOff = !getSystemService(PowerManager::class.java).isInteractive
        lock.onAppLeft(SystemClock.elapsedRealtime(), screenOff)
        lockState = lock.state
        // 34: a restore being written is abandoned by a lock (section 9, AC-57).
        if (lockState != LockState.Unlocked) viewModel.onLocked()
        // 21: leaving ends the screenshot window (spec 9), and so does any lock (decision 1); a turn of the phone that
        // keeps the app unlocked is no leaving, and the new activity goes on with it (onStart).
        if (!isChangingConfigurations || lock.state != LockState.Unlocked) screenshots.end()
        // 32: the next onStart is an opening of the app, which shows a dismissed reminder again; a turn of the phone is not.
        if (!isChangingConfigurations) viewModel.onAppHidden()
        if (screenshots.timeLeft(SystemClock.elapsedRealtime()) == null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            viewModel.onScreenshotWindow(null)
        }
        super.onStop()
    }

    /**
     * Back up now (31; wireframe 10e marker 2): when the ViewModel starts an export, Android's file screen opens with
     * the suggested name. That screen coming up is not a leaving under "every time you leave" (spec section 9, AC-57);
     * the lock is told just before, and again when the answer comes. With no app to show the file screen, the export
     * ends as a cancelled one.
     *
     * The file screen is asked for with startActivityForResult and answers in [onActivityResult], not with the newer
     * result launcher: lint refuses the launcher while the Fragment library is below 1.3.0 (the biometric library brings
     * 1.2.5), as that FragmentActivity does not take the launcher's request codes (31 file, section 14).
     */
    private fun backUpNow() {
        if (lock.state != LockState.Unlocked) return
        val suggestedName = viewModel.beginBackup() ?: return
        val request =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(BACKUP_FILE_TYPE)
                .putExtra(Intent.EXTRA_TITLE, suggestedName)
        lock.onFileScreenOpened()
        try {
            startActivityForResult(request, BACKUP_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            lock.onFileScreenClosed()
            viewModel.onBackupPlace(null)
        }
    }

    /**
     * The file screen's answer (31): a place, or none when it was cancelled. It also reaches an activity that Android
     * created again while the file screen was open. The ViewModel decides what is written, locked or not (decision 7).
     */
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESTORE_FILE_REQUEST) {
            lock.onFileScreenClosed()
            val chosen = if (resultCode == RESULT_OK) data?.data else null
            // Its name is asked off the main thread by the ViewModel (the Deploy review's finding 1).
            viewModel.onRestoreFile(chosen?.let { DocumentRestoreFile(applicationContext.contentResolver, it) })
            return
        }
        if (requestCode != BACKUP_FILE_REQUEST) return
        lock.onFileScreenClosed()
        val place = if (resultCode == RESULT_OK) data?.data else null
        viewModel.onBackupPlace(place?.let { DocumentBackupTarget(applicationContext.contentResolver, it) })
    }

    /**
     * Choose file in the restore wizard (33b; wireframe 10e marker 7): Android's file screen opens on any file, as the
     * file's name and type do not matter to the restore (section 8); the app reads only the file you pick. The screen
     * coming up is not a leaving under "every time you leave" (spec section 9, AC-57), as for Back up now. With no app to
     * show the file screen, the choice ends as a cancelled one.
     */
    private fun chooseRestoreFile() {
        if (lock.state != LockState.Unlocked || !viewModel.beginRestoreChoice()) return
        val request = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(RESTORE_FILE_TYPE)
        lock.onFileScreenOpened()
        try {
            startActivityForResult(request, RESTORE_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            lock.onFileScreenClosed()
            viewModel.onRestoreFile(null)
        }
    }

    /**
     * Copy report (C16; 33b decision 5): the report's text as shown, nothing else, marked sensitive so Android hides its
     * preview of copied text (ClipDescription.EXTRA_IS_SENSITIVE, API 33, checked with javap on the SDK's android.jar).
     * The clip stays on the clipboard, where other apps can read it (C16). Nothing of it is logged (spec section 9).
     */
    private fun copyReport(text: String) {
        if (lock.state != LockState.Unlocked) return
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(getString(R.string.restore_report_label), text)
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        clipboard.setPrimaryClip(clip)
    }

    /** Settings' "Allow screenshots for 3 minutes" (21): the check, only while unlocked and the window is off. */
    private fun allowScreenshots() {
        if (lock.state != LockState.Unlocked || !screenshots.requestCheck()) return
        prompt.authenticate(screenshotPromptInfo)
    }

    /** Settings' Turn off (21). */
    private fun turnOffScreenshots() = endScreenshotWindow()

    private fun endScreenshotWindow() {
        screenshots.end()
        applyScreenshotWindow()
    }

    /**
     * The flag follows the window (spec section 9; 21): cleared while it is on, set at once otherwise; the recents
     * preview stays off all along (setRecentsScreenshotEnabled in onCreate). While on, it runs again on the next whole
     * second of the time left, so the window ends at exactly 0, and passes the time left to the screen.
     */
    private fun applyScreenshotWindow() {
        idleHandler.removeCallbacks(screenshotTick)
        val left = screenshots.timeLeft(SystemClock.elapsedRealtime())
        if (left == null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            idleHandler.postDelayed(screenshotTick, (left - 1) % 1_000 + 1)
        }
        viewModel.onScreenshotWindow(left)
    }

    /**
     * A touch in the app is activity (20 decision 1; typing on the keyboard, Android's own window, is not: decision 5).
     * Idle is checked first, so a touch after the limit but before the next timed check cannot reach the screen.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val now = SystemClock.elapsedRealtime()
            lock.onIdleCheck(now)
            if (showLockIfLocked()) return true
            lock.onInteraction(now)
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * After a lock in view (20 decision 2): the lock screen with the prompt at once, and the screenshot window ended (21
     * decision 1). True if the app is now locked.
     */
    private fun showLockIfLocked(): Boolean {
        if (lockState != LockState.Unlocked || lock.state == LockState.Unlocked) return false
        endScreenshotWindow()
        lockState = lock.state
        // 34: a restore being written is abandoned by a lock (section 9, AC-57).
        viewModel.onLocked()
        showPromptIfNeeded()
        return true
    }

    private fun showPromptIfNeeded() {
        if (lock.shouldShowPrompt()) {
            lock.onPromptShown()
            prompt.authenticate(promptInfo)
        }
    }

    private fun viewModelFactory() = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = applicationContext
            val appViewModel = AppViewModel(
                storeProvider = {
                    RoomRecordStore(DatabaseKey(appContext.noBackupFilesDir), SqlCipherOpener(appContext)::open)
                },
                clock = Clock.systemUTC(),
                newId = { UUID.randomUUID().toString() },
                // 30: no key at start; the ViewModel gives the source the stored key once the records are loaded.
                priceSource = FinnhubSource(),
                isOnline = NetworkStatus(appContext)::isOnline,
                // 36: 0 (the 7 days) in every build but the check build (decisions 1 and 2).
                backupDueAfter = BackupReminder.after(BuildConfig.BACKUP_DUE_AFTER_MINUTES),
            )
            return modelClass.cast(appViewModel)!!
        }
    }
}

/**
 * The locked screen (44a step 8 group (e); drawing spec/wireframes/design/remaining-screens.html frame 4): on the page
 * color, the app's name in 22 and Unlock the large gold button. The fingerprint or PIN prompt it opens is the system's.
 */
@Composable
fun LockedScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    LockPage(modifier) {
        Text(text = stringResource(R.string.app_name), style = typeStyle("screenTitle"), color = AtsTheme.colors.text)
        AtsButton(
            stringResource(R.string.unlock_button),
            onUnlock,
            modifier = Modifier.padding(top = tokenDp("layerDialogSide")),
            kind = ButtonKind.FILLED,
            size = ButtonSize.LARGE,
        )
    }
}

/** The cover shown whenever the window loses focus (drawing frame 4): the app's name only, on the page color. */
@Composable
fun CoverScreen(modifier: Modifier = Modifier) {
    LockPage(modifier) {
        Text(text = stringResource(R.string.app_name), style = typeStyle("screenTitle"), color = AtsTheme.colors.text)
    }
}

/** No screen lock on the phone: the line saying to set one up, on the page color. */
@Composable
fun NoScreenLockScreen(modifier: Modifier = Modifier) {
    LockPage(modifier) {
        Text(text = stringResource(R.string.no_screen_lock_message), style = typeStyle("body"), color = AtsTheme.colors.text)
    }
}

// The lock, cover and no-lock screens' page: the page color, its content centered with the screen's margin.
@Composable
private fun LockPage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().background(AtsTheme.colors.page).padding(tokenDp("layerDialogSide")),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
