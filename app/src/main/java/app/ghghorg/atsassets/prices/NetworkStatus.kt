package app.ghghorg.atsassets.prices

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the device has a network that offers internet (09 decision 8: offline is told apart from a source that does
 * not answer). Needs ACCESS_NETWORK_STATE. Checked on the phone only (step 10); a JVM test cannot run it.
 */
class NetworkStatus(
    private val context: Context,
) {
    fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
