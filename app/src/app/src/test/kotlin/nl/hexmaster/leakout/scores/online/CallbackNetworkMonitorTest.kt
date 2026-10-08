package nl.hexmaster.leakout.scores.online

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Network callbacks become a flow that stops listening when collection ends. */
class CallbackNetworkMonitorTest {
    private var onAvailable: (() -> Unit)? = null
    private var unregistered = 0

    private val monitor = CallbackNetworkMonitor { callback ->
        onAvailable = callback
        ({ unregistered++ })
    }

    @Test
    fun `every available network is emitted and the callback is unregistered at the end`() = runTest {
        val events = mutableListOf<Unit>()
        val job = launch { monitor.availability().take(2).toList(events) }
        testScheduler.runCurrent()
        onAvailable!!()
        testScheduler.runCurrent()
        onAvailable!!()
        job.join()
        assertEquals(2, events.size)
        assertEquals(1, unregistered)
    }

    @Test
    fun `cancelling the collector unregisters`() = runTest {
        val job = launch { monitor.availability().collect {} }
        testScheduler.runCurrent()
        job.cancel()
        job.join()
        assertEquals(1, unregistered)
    }
}
