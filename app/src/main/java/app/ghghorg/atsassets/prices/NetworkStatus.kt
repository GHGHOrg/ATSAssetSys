package app.ghghorg.atsassets.prices

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler

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

    /**
     * Calls [onAvailable] on [handler]'s thread each time a network that offers internet becomes the default (16a
     * decision 2: the phone comes back online). Returns the call that stops watching. Checked on the phone only.
     */
    fun watch(
        handler: Handler,
        onAvailable: () -> Unit,
    ): () -> Unit {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return {}
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onAvailable()
                }
            }
        manager.registerDefaultNetworkCallback(callback, handler)
        return { manager.unregisterNetworkCallback(callback) }
    }
}
