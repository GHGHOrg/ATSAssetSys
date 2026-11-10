package app.ghghorg.atsassets.lock

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

/**
 * Keeps the [AppLock] and the [ScreenshotWindow] while MainActivity is re-created, as when the phone is turned, but not
 * past the process: a restart always locks and the window is then off (spec section 9; 20, 21). While it lives it hears
 * the screen go off, for "only when the phone locks" with the app in view or not.
 *
 * The receiver is on the application context and exported, as the broadcasts guide says for system broadcasts (20
 * section 10.1); a screen-off sent by another app could only lock the app. Android may defer it for a cached app (risk 7).
 */
class LockHolder(
    application: Application,
) : AndroidViewModel(application) {
    val lock = AppLock()
    val screenshots = ScreenshotWindow()

    private val screenOff =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) lock.onPhoneLocked()
            }
        }

    init {
        ContextCompat.registerReceiver(application, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(screenOff)
    }
}
