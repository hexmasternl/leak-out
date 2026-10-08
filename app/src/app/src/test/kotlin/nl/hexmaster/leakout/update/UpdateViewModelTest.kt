package nl.hexmaster.leakout.update

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.core.update.UpdateStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = error("not used")
    }
    private val updates = FakeAppUpdates()
    private val required = UpdateCheck(available = true, priority = 5)
    private val optional = UpdateCheck(available = true, priority = 0)

    @Test
    fun `starting the activity checks and a required update closes the gate`() = runTest {
        updates.next = required
        val vm = UpdateViewModel(updates)
        vm.onStart(owner)
        assertEquals(1, updates.checks)
        assertEquals(UpdateStatus.Required, vm.gate.value.status)
        assertTrue(vm.gate.value.showBlockingScreen())
        assertFalse(vm.gate.value.mayStartLevel())
    }

    @Test
    fun `an optional update at start is offered once`() = runTest {
        updates.next = optional
        val vm = UpdateViewModel(updates)
        vm.onStart(owner)
        assertTrue(vm.gate.value.showOptionalPrompt())
        vm.dismissOptional()
        vm.onStart(owner)
        assertFalse(vm.gate.value.showOptionalPrompt())
        assertTrue(vm.gate.value.mayStartLevel())
    }

    @Test
    fun `returning to the foreground checks again`() = runTest {
        val vm = UpdateViewModel(updates)
        vm.onStart(owner)
        assertTrue(vm.gate.value.mayStartLevel())
        updates.next = required
        vm.onStart(owner)
        assertEquals(2, updates.checks)
        assertTrue(vm.gate.value.showBlockingScreen())
    }

    @Test
    fun `a failed check never blocks`() = runTest {
        updates.failing = true
        val vm = UpdateViewModel(updates)
        vm.onStart(owner)
        assertEquals(1, updates.checks)
        assertEquals(UpdateStatus.None, vm.gate.value.status)
        assertTrue(vm.gate.value.mayStartLevel())
        assertFalse(vm.gate.value.showOptionalPrompt())
    }

    @Test
    fun `an immediate update in progress is resumed when the activity resumes`() = runTest {
        updates.next = required.copy(immediateInProgress = true)
        val vm = UpdateViewModel(updates)
        val requests = mutableListOf<UpdateFlow>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.requests.toList(requests) }
        vm.onStart(owner)
        assertEquals("not before the activity is resumed", emptyList<UpdateFlow>(), requests)
        vm.onResume(owner)
        assertEquals(listOf(UpdateFlow.Required), requests)
    }

    @Test
    fun `update now and accepting the optional prompt ask the activity for the Play flow`() = runTest {
        updates.next = optional
        val vm = UpdateViewModel(updates)
        val requests = mutableListOf<UpdateFlow>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.requests.toList(requests) }
        vm.onStart(owner)
        vm.acceptOptional()
        assertFalse(vm.gate.value.showOptionalPrompt())
        vm.updateNow()
        assertEquals(listOf(UpdateFlow.Optional, UpdateFlow.Required), requests)
    }

    @Test
    fun `a finished download is offered for install outside a level only`() = runTest {
        updates.next = optional
        val vm = UpdateViewModel(updates)
        vm.onStart(owner)
        vm.acceptOptional()
        vm.setInLevel(true)
        updates.download(DownloadState.Downloaded)
        assertFalse(vm.gate.value.showInstallPrompt())
        vm.setInLevel(false)
        assertTrue(vm.gate.value.showInstallPrompt())
        vm.install()
        assertEquals(1, updates.completes)
        assertFalse(vm.gate.value.showInstallPrompt())
    }

    @Test
    fun `a required update found during a level waits until the level is left`() = runTest {
        val vm = UpdateViewModel(updates)
        vm.setInLevel(true)
        updates.next = required
        vm.onStart(owner)
        assertFalse(vm.gate.value.showBlockingScreen())
        assertFalse(vm.gate.value.mayStartLevel())
        vm.setInLevel(false)
        assertTrue(vm.gate.value.showBlockingScreen())
    }
}
