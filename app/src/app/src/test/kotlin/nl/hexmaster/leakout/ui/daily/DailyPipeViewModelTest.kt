package nl.hexmaster.leakout.ui.daily

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.scores.online.DailyPipe
import nl.hexmaster.leakout.scores.online.DailyPipeDownload
import nl.hexmaster.leakout.scores.online.DailyPipes
import nl.hexmaster.leakout.scores.online.DailyStreak
import nl.hexmaster.leakout.scores.online.DailySubmission
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyPipeViewModelTest {
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedIn("Ann"))
    private val online = MutableStateFlow(true)
    private val dailyPipes = FakeDailyPipes()
    private val date = LocalDate.of(2026, 11, 2)
    private val streak = DailyStreak(3, 7, wonToday = false)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = DailyPipeViewModel(dailyPipes, signIn, online)

    @Test
    fun `the button is shown only to a signed-in player`() {
        signIn.value = SignInState.SignedOut
        val vm = vm()
        assertFalse(vm.state.value.visible)

        signIn.value = SignInState.SignedIn("Ann")
        assertTrue(vm.state.value.visible)
        assertTrue(vm.state.value.enabled)
    }

    @Test
    fun `offline the button is disabled until the connection returns`() {
        online.value = false
        val vm = vm()
        assertTrue(vm.state.value.visible)
        assertFalse(vm.state.value.enabled)

        vm.open()
        assertEquals(0, dailyPipes.downloads)

        online.value = true
        assertTrue(vm.state.value.enabled)
    }

    @Test
    fun `a valid pipe opens with its level and seed`() {
        dailyPipes.answer = { DailyPipeDownload.Available(DailyPipe(date, generatedLevel(), 1234L, streak, null)) }
        val vm = vm()

        vm.open()

        val game = vm.opened.value!!
        assertEquals(date, game.date)
        assertEquals(20261102, game.level.id)
        assertEquals(1234L, game.stackSeed)
        assertEquals(streak, game.streak)
        assertNull(vm.state.value.problem)
        vm.consumeOpened()
        assertNull(vm.opened.value)
    }

    @Test
    fun `while downloading the button is disabled and says it is loading`() {
        val gate = CompletableDeferred<DailyPipeDownload>()
        dailyPipes.suspendingAnswer = { gate.await() }
        val vm = vm()

        vm.open()
        assertTrue(vm.state.value.loading)
        assertFalse(vm.state.value.enabled)
        vm.open()
        assertEquals(1, dailyPipes.downloads)

        gate.complete(DailyPipeDownload.NotPublished)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `no pipe yet stays on the welcome screen with that message`() {
        dailyPipes.answer = { DailyPipeDownload.NotPublished }
        val vm = vm()

        vm.open()

        assertNull(vm.opened.value)
        assertEquals(DailyPipeProblem.NotPublished, vm.state.value.problem)
        vm.dismissProblem()
        assertNull(vm.state.value.problem)
    }

    @Test
    fun `a failed download stays on the welcome screen`() {
        dailyPipes.answer = { DailyPipeDownload.Failed }
        val vm = vm()

        vm.open()

        assertNull(vm.opened.value)
        assertEquals(DailyPipeProblem.DownloadFailed, vm.state.value.problem)
    }

    @Test
    fun `a level that does not parse or validate is not opened`() {
        val vm = vm()

        dailyPipes.answer = { DailyPipeDownload.Available(DailyPipe(date, """{"id":1,"futureField":true}""", 1L, streak, null)) }
        vm.open()
        assertEquals(DailyPipeProblem.Unsupported, vm.state.value.problem)

        dailyPipes.answer = { DailyPipeDownload.Available(DailyPipe(date, invalidLevel(), 1L, streak, null)) }
        vm.open()
        assertEquals(DailyPipeProblem.Unsupported, vm.state.value.problem)
        assertNull(vm.opened.value)
    }

    @Test
    fun `a new tap clears the last problem`() {
        dailyPipes.answer = { DailyPipeDownload.Failed }
        val vm = vm()
        vm.open()

        dailyPipes.answer = { DailyPipeDownload.Available(DailyPipe(date, generatedLevel(), 1L, streak, null)) }
        vm.open()

        assertNull(vm.state.value.problem)
    }

    /** A small valid level, written the way the server writes a daily pipe. */
    private fun generatedLevel(): String =
        """{"id":20261102,"name":"Daily Pipe 2026-11-02","grid":{"columns":6,"rows":5},"start":{"x":0,"y":0,"openSide":"RIGHT"},""" +
            """"finish":{"x":5,"y":4,"openSide":"LEFT"},"blocked":[],"flow":{"initialDelayMs":20000,"cellsPerSecond":0.3,""" +
            """"reservoirSpeedFactor":0.3,"speedRampPerCell":0},"stack":{"visibleSize":5,"weights":{"H":7,"V":7,"LT":7,"LB":7,"RT":7,"RB":7,"X":2,"RES":0}},""" +
            """"rules":{"allowReplace":true,"replacePenaltyMs":1500},"stars":{"one":80,"two":120,"three":160}}"""

    /** Parses, but fails validation: start and finish on the same cell. */
    private fun invalidLevel(): String =
        """{"id":1,"name":"x","grid":{"columns":6,"rows":5},"start":{"x":0,"y":0,"openSide":"RIGHT"},""" +
            """"finish":{"x":0,"y":0,"openSide":"RIGHT"},"blocked":[],"flow":{"initialDelayMs":1000,"cellsPerSecond":0.3},""" +
            """"stars":{"one":1,"two":2,"three":3}}"""

    private class FakeDailyPipes : DailyPipes {
        var downloads = 0
        var answer: () -> DailyPipeDownload = { DailyPipeDownload.Failed }
        var suspendingAnswer: (suspend () -> DailyPipeDownload)? = null

        override suspend fun today(): DailyPipeDownload {
            downloads++
            return suspendingAnswer?.invoke() ?: answer()
        }

        override suspend fun submit(date: LocalDate, score: Int, stars: Int): DailySubmission = DailySubmission.Failed
    }
}
