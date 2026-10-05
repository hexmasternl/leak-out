package nl.hexmaster.leakout.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.audio.Playlist
import nl.hexmaster.leakout.audio.Track
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.library.TileState
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.settings.SettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.levels.LevelCatalog
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.levels.asHistoryReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Settings that have not been read yet: [settings] emits only when the test says so. */
private class SlowSettings : SettingsRepository {
    override val settings = MutableSharedFlow<Settings>(replay = 1)
    override suspend fun setSoundEnabled(enabled: Boolean) = Unit
    override suspend fun setSoundVolume(volume: Float) = Unit
    override suspend fun setMusicEnabled(enabled: Boolean) = Unit
    override suspend fun setMusicVolume(volume: Float) = Unit
    override suspend fun setVibrationEnabled(enabled: Boolean) = Unit
    override val seenTips = MutableStateFlow<Set<TipId>>(emptySet())
    override suspend fun markTipSeen(tip: TipId) = Unit
    override suspend fun resetTips() = Unit
    override val skipExitConfirmation = MutableStateFlow(false)
    override suspend fun setSkipExitConfirmation(skip: Boolean) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** 12 finished levels with 31 stars: ten with 3 stars, one with 1 and one with 0. */
    private val twelveLevels = (1..10).map { LevelHistory(it, 100, 3) } + LevelHistory(11, 50, 1) + LevelHistory(12, 5, 0)

    @Test
    fun `the progress summary counts finished levels and their stars`() {
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository(twelveLevels))
        val state = vm.uiState.value!!
        assertEquals(ProgressSummary(12, 31), state.progress)
        assertTrue(state.progress.hasProgress)
        assertEquals(Settings.Default, state.settings)
    }

    @Test
    fun `an empty history has no progress`() {
        val state = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository()).uiState.value!!
        assertEquals(ProgressSummary.None, state.progress)
        assertFalse(state.progress.hasProgress)
    }

    @Test
    fun `a level finished with 0 stars still counts as finished`() {
        assertEquals(ProgressSummary(1, 0), ProgressSummary.of(listOf(LevelHistory(1, 5, 0))))
        assertTrue(ProgressSummary.of(listOf(LevelHistory(1, 5, 0))).hasProgress)
    }

    @Test
    fun `the state is null until the settings are read`() {
        val settings = SlowSettings()
        val vm = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        assertNull(vm.uiState.value)
        settings.settings.tryEmit(Settings(vibrationEnabled = false))
        assertFalse(vm.uiState.value!!.settings.vibrationEnabled)
    }

    @Test
    fun `a recorded win updates the summary`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        val vm = SettingsViewModel(InMemorySettingsRepository(), history)
        history.recordCompletion(1, 100, 2)
        assertEquals(ProgressSummary(1, 2), vm.uiState.value!!.progress)
    }

    @Test
    fun `a vibration change is written and shown`() = runTest {
        val settings = InMemorySettingsRepository()
        val vm = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        vm.onVibrationChange(false)
        assertFalse(settings.settings.first().vibrationEnabled)
        assertFalse(vm.uiState.value!!.settings.vibrationEnabled)
        assertEquals(listOf("vibrationEnabled"), settings.writes)
        vm.onVibrationChange(true)
        assertTrue(vm.uiState.value!!.settings.vibrationEnabled)
    }

    @Test
    fun `a sound effects change is written and shown`() = runTest {
        val settings = InMemorySettingsRepository()
        val vm = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        vm.onSoundEnabledChange(false)
        assertFalse(settings.settings.first().soundEnabled)
        assertFalse(vm.uiState.value!!.settings.soundEnabled)
        vm.onSoundVolumeChange(0.3f)
        assertEquals(0.3f, settings.settings.first().soundVolume)
        assertEquals(0.3f, vm.uiState.value!!.settings.soundVolume)
        assertEquals(listOf("soundEnabled", "soundVolume"), settings.writes)
    }

    @Test
    fun `a sound volume out of range is clamped`() = runTest {
        val settings = InMemorySettingsRepository()
        val vm = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        vm.onSoundVolumeChange(1.5f)
        assertEquals(1f, vm.uiState.value!!.settings.soundVolume)
    }

    @Test
    fun `a music change is written and shown`() = runTest {
        val settings = InMemorySettingsRepository()
        val vm = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        vm.onMusicEnabledChange(false)
        assertFalse(settings.settings.first().musicEnabled)
        assertFalse(vm.uiState.value!!.settings.musicEnabled)
        vm.onMusicVolumeChange(0.3f)
        assertEquals(0.3f, settings.settings.first().musicVolume)
        assertEquals(0.3f, vm.uiState.value!!.settings.musicVolume)
        assertEquals(listOf("musicEnabled", "musicVolume"), settings.writes)
    }

    @Test
    fun `a music volume out of range is clamped`() = runTest {
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository())
        vm.onMusicVolumeChange(-2f)
        assertEquals(0f, vm.uiState.value!!.settings.musicVolume)
    }

    @Test
    fun `the state carries the music credits in playlist order`() {
        val tracks = listOf(Track("b.mp3", "Second", "Two"), Track("a.mp3", "First", "One"))
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository(), Playlist(tracks))
        assertEquals(tracks, vm.uiState.value!!.musicCredits)
    }

    @Test
    fun `without credits the state has none`() {
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository(), null)
        assertTrue(vm.uiState.value!!.musicCredits.isEmpty())
    }

    @Test
    fun `reset clears the history and keeps every setting`() = runTest {
        val stored = Settings(soundEnabled = false, soundVolume = 0.3f, musicEnabled = false, musicVolume = 0.1f, vibrationEnabled = false)
        val settings = InMemorySettingsRepository(stored)
        val history = InMemoryLevelHistoryRepository(twelveLevels)
        val vm = SettingsViewModel(settings, history)

        vm.onResetConfirmed()

        assertTrue(history.observeAll().first().isEmpty())
        assertEquals(ProgressSummary.None, vm.uiState.value!!.progress)
        assertEquals(stored, settings.settings.first())
        assertEquals(stored, vm.uiState.value!!.settings)
        assertTrue(settings.writes.isEmpty())
    }

    @Test
    fun `a reset keeps the achievements and looks at the emptied history again`() = runTest {
        val history = InMemoryLevelHistoryRepository(twelveLevels)
        val store = InMemoryAchievementRepository()
        val tracker = AchievementTracker(store, history, backgroundScope)
        tracker.reconcile()
        val unlockedBefore = store.observeAll().first().filter { it.unlocked }
        assertTrue(unlockedBefore.isNotEmpty())
        var reconciled = false

        SettingsViewModel(InMemorySettingsRepository(), history, onProgressReset = {
            assertTrue("after the history is cleared", history.observeAll().first().isEmpty())
            reconciled = tracker.reconcile().isEmpty()
        }).onResetConfirmed()

        assertTrue(reconciled)
        assertEquals(unlockedBefore, store.observeAll().first().filter { it.unlocked })
    }

    @Test
    fun `after a reset the level library is back to a fresh install`() = runTest {
        val history = InMemoryLevelHistoryRepository(twelveLevels)
        val library = LevelLibraryViewModel(history.asHistoryReader(), LevelCatalog { 48 })
        assertEquals(TileState.Completed(3), library.uiState.value.library.tile(1)!!.state)

        SettingsViewModel(InMemorySettingsRepository(), history).onResetConfirmed()

        val tiles = library.uiState.value.library.pages.flatMap { it.tiles }
        assertTrue(tiles.none { it.state is TileState.Completed })
        // The spec says "only level 1": the library rules (app-game-level-selector) open a window of
        // three levels at the start of page 1, so a reset gives exactly what a first launch gives.
        assertEquals(listOf(1, 2, 3), tiles.filter { it.isPlayable }.map { it.levelId })
        assertEquals(LevelLibraryViewModel(InMemoryLevelHistoryRepository().asHistoryReader(), LevelCatalog { 48 }).uiState.value, library.uiState.value)
    }

    @Test
    fun `show tips again is offered only when a tip was seen`() {
        val none = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository())
        assertEquals(TipsState(anySeen = false), none.uiState.value!!.tips)
        val seen = SettingsViewModel(InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro)), InMemoryLevelHistoryRepository())
        assertEquals(TipsState(anySeen = true), seen.uiState.value!!.tips)
    }

    @Test
    fun `show tips again resets the tips and says they will show again`() = runTest {
        val settings = InMemorySettingsRepository(Settings(vibrationEnabled = false), setOf(TipId.Intro, TipId.Cross))
        val history = InMemoryLevelHistoryRepository(twelveLevels)
        val vm = SettingsViewModel(settings, history)
        vm.onShowTipsAgain()
        assertEquals(emptySet<TipId>(), settings.seenTips.value)
        assertEquals(TipsState(anySeen = false, justReset = true), vm.uiState.value!!.tips)
        // The settings and the progress are left alone.
        assertFalse(vm.uiState.value!!.settings.vibrationEnabled)
        assertEquals(ProgressSummary(12, 31), vm.uiState.value!!.progress)
    }

    @Test
    fun `without Play Games the panel is unavailable and counts the whole catalogue`() {
        val state = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository()).uiState.value!!
        assertEquals(PlayGamesUiState(SignInState.Unavailable, unlocked = 0, total = Achievement.entries.size), state.playGames)
    }

    @Test
    fun `the panel counts the unlocked achievements out of the catalogue`() {
        val unlocked = Achievement.entries.take(12).map { AchievementProgress(it, unlockedAtMs = 1L) }
        val vm = SettingsViewModel(
            InMemorySettingsRepository(),
            InMemoryLevelHistoryRepository(),
            achievements = InMemoryAchievementRepository(initial = unlocked),
        )
        assertEquals(12, vm.uiState.value!!.playGames.unlocked)
        assertEquals(25, vm.uiState.value!!.playGames.total)
    }

    @Test
    fun `the panel follows the sign-in state`() {
        val signIn = MutableStateFlow<SignInState>(SignInState.SignedOut)
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository(), signIn = signIn)
        assertEquals(SignInState.SignedOut, vm.uiState.value!!.playGames.signIn)
        signIn.value = SignInState.SigningIn
        assertEquals(SignInState.SigningIn, vm.uiState.value!!.playGames.signIn)
        signIn.value = SignInState.SignedIn("Eduard")
        assertEquals(SignInState.SignedIn("Eduard"), vm.uiState.value!!.playGames.signIn)
    }

    @Test
    fun `a new unlock raises the count`() = runTest {
        val store = InMemoryAchievementRepository()
        val vm = SettingsViewModel(InMemorySettingsRepository(), InMemoryLevelHistoryRepository(listOf(LevelHistory(1, 100, 3))), achievements = store)
        assertEquals(0, vm.uiState.value!!.playGames.unlocked)
        AchievementTracker(store, InMemoryLevelHistoryRepository(listOf(LevelHistory(1, 100, 3))), backgroundScope).reconcile()
        assertEquals(1, vm.uiState.value!!.playGames.unlocked)
    }
}
