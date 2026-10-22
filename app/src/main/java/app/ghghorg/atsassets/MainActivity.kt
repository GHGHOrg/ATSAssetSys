package app.ghghorg.atsassets

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.ghghorg.atsassets.lock.AppLock
import app.ghghorg.atsassets.lock.LockState
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme

class MainActivity : FragmentActivity() {
    private val lock = AppLock()
    private var lockState by mutableStateOf(LockState.Locked)
    private var windowFocused by mutableStateOf(false)
    private lateinit var prompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    lock.onAuthSucceeded()
                    lockState = lock.state
                }

                // Dismissed or error. One wrong attempt is onAuthenticationFailed, not this.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    lock.onAuthCancelledOrFailed()
                    lockState = lock.state
                }
            },
        )
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.prompt_title))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        setContent {
            ATSAssetSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (lockState) {
                        // Cover while the window is not focused (for example recents), so the
                        // live card shows no content. Experiment after phone check 7.
                        LockState.Unlocked -> if (windowFocused) {
                            Greeting(
                                name = "Android",
                                modifier = Modifier.padding(innerPadding),
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
        lockState = lock.state
        showPromptIfNeeded()
    }

    override fun onStop() {
        prompt.cancelAuthentication()
        lock.onAppLeft()
        lockState = lock.state
        super.onStop()
    }

    private fun showPromptIfNeeded() {
        if (lock.shouldShowPrompt()) {
            lock.onPromptShown()
            prompt.authenticate(promptInfo)
        }
    }
}

@Composable
fun LockedScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(
            onClick = onUnlock,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(text = stringResource(R.string.unlock_button))
        }
    }
}

@Composable
fun CoverScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
fun NoScreenLockScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_screen_lock_message),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ATSAssetSystemTheme {
        Greeting("Android")
    }
}
