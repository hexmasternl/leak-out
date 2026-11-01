package nl.hexmaster.leakout.scores.online

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/** Tells when the device gets a network connection, so queued scores can be sent. */
fun interface NetworkMonitor {
    /** Emits whenever a network that can reach the internet becomes available (also once at start when online). */
    fun availability(): Flow<Unit>

    /**
     * Whether the device can reach the internet: the current answer first, then every change. A
     * monitor that cannot tell says online, so nothing is disabled by mistake.
     */
    fun online(): Flow<Boolean> = flowOf(true)
}

/**
 * A [NetworkMonitor] over callback registrations: [register] starts listening, calls its argument
 * whenever a network is available, and returns the function that stops listening. [registerOnline]
 * does the same for [online]: it calls its argument with the current answer and on every change.
 */
class CallbackNetworkMonitor(
    private val registerOnline: ((onChange: (Boolean) -> Unit) -> (() -> Unit))? = null,
    // Last, so `CallbackNetworkMonitor { onAvailable -> ... }` keeps registering availability.
    private val register: (onAvailable: () -> Unit) -> (() -> Unit),
) : NetworkMonitor {
    override fun availability(): Flow<Unit> = callbackFlow {
        val unregister = register { trySend(Unit) }
        awaitClose { unregister() }
    }

    override fun online(): Flow<Boolean> {
        val registration = registerOnline ?: return flowOf(true)
        return callbackFlow {
            val unregister = registration { trySend(it) }
            awaitClose { unregister() }
        }.distinctUntilChanged()
    }

    companion object {
        /** Watches the default network through [ConnectivityManager]. */
        fun android(context: Context): NetworkMonitor {
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            return CallbackNetworkMonitor(
                register = { onAvailable ->
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
                },
                registerOnline = { onChange ->
                    // The callback only reports changes, and nothing at all without a network: start from now.
                    val current = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                    onChange(current?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
                    val callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                            onChange(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                        }

                        override fun onLost(network: Network) {
                            onChange(false)
                        }
                    }
                    connectivity.registerDefaultNetworkCallback(callback)
                    ({ connectivity.unregisterNetworkCallback(callback) })
                },
            )
        }
    }
}
