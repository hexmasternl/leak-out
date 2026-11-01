package nl.hexmaster.leakout.scores.online

import kotlinx.coroutines.flow.emptyFlow
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

    @Test
    fun `online state is the registration's answers without repeats, and stops listening at the end`() = runTest {
        var onChange: ((Boolean) -> Unit)? = null
        var stopped = 0
        val withOnline = CallbackNetworkMonitor(register = { { } }, registerOnline = { callback ->
            onChange = callback
            callback(false)
            ({ stopped++ })
        })
        val states = mutableListOf<Boolean>()
        val job = launch { withOnline.online().take(3).toList(states) }
        testScheduler.runCurrent()
        onChange!!(false)
        onChange!!(true)
        testScheduler.runCurrent()
        onChange!!(true)
        onChange!!(false)
        job.join()
        assertEquals(listOf(false, true, false), states)
        assertEquals(1, stopped)
    }

    @Test
    fun `a monitor that cannot tell says online`() = runTest {
        assertEquals(listOf(true), monitor.online().toList())
        assertEquals(listOf(true), NetworkMonitor { emptyFlow() }.online().toList())
    }
}
