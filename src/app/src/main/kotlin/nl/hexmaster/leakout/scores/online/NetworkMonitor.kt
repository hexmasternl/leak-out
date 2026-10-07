package nl.hexmaster.leakout.scores.online

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Tells when the device gets a network connection, so queued scores can be sent. */
fun interface NetworkMonitor {
    /** Emits whenever a network that can reach the internet becomes available (also once at start when online). */
    fun availability(): Flow<Unit>
}

/**
 * A [NetworkMonitor] over a callback registration: [register] starts listening, calls its argument
 * whenever a network is available, and returns the function that stops listening.
 */
class CallbackNetworkMonitor(private val register: (onAvailable: () -> Unit) -> (() -> Unit)) : NetworkMonitor {
    override fun availability(): Flow<Unit> = callbackFlow {
        val unregister = register { trySend(Unit) }
        awaitClose { unregister() }
    }

    companion object {
        /** Watches the default network through [ConnectivityManager]. */
        fun android(context: Context): NetworkMonitor = CallbackNetworkMonitor { onAvailable ->
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            // Capabilities change often (signal strength); only the step to a validated network counts.
            val callback = object : ConnectivityManager.NetworkCallback() {
                private var validated = false

                override fun onAvailable(network: Network) {
                    validated = false
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val now = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (now && !validated) onAvailable()
                    validated = now
                }

                override fun onLost(network: Network) {
                    validated = false
                }
            }
            connectivity.registerDefaultNetworkCallback(callback)
            ({ connectivity.unregisterNetworkCallback(callback) })
        }
    }
}
