package nl.hexmaster.leakout

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.snapshotFlow
import nl.hexmaster.leakout.reminders.ReminderNotifications
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.ads.ConsentManager
import nl.hexmaster.leakout.ads.SessionStartAds
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.achievements.playgames.showPlayGamesAchievements
import nl.hexmaster.leakout.audio.AudioManagerAudioFocus
import nl.hexmaster.leakout.audio.BundledMusic
import nl.hexmaster.leakout.audio.DataStoreMusicRotationStore
import nl.hexmaster.leakout.audio.MediaPlayerLoopingSoundPlayer
import nl.hexmaster.leakout.audio.MediaPlayerMusicEngine
import nl.hexmaster.leakout.audio.MusicViewModel
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffectsViewModel
import nl.hexmaster.leakout.audio.SoundPoolSoundEffectPlayer
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.core.library.NextLevelResolver
import nl.hexmaster.leakout.haptics.HapticsViewModel
import nl.hexmaster.leakout.haptics.VibratorHapticPlayer
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.settings.DataStoreSettingsRepository
import nl.hexmaster.leakout.settings.SettingsRepository
import nl.hexmaster.leakout.ui.achievements.AchievementsScreen
import nl.hexmaster.leakout.ui.achievements.AchievementsViewModel
import nl.hexmaster.leakout.ui.daily.DailyPipeGame
import nl.hexmaster.leakout.ui.daily.DailyPipeViewModel
import nl.hexmaster.leakout.ui.daily.DailyResults
import nl.hexmaster.leakout.ui.designer.AndroidLevelExporter
import nl.hexmaster.leakout.ui.designer.LevelDesignerScreen
import nl.hexmaster.leakout.ui.designer.LevelDesignerViewModel
import nl.hexmaster.leakout.ui.designer.TestPlayScreen
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.game.GameSession
import nl.hexmaster.leakout.ui.game.TopScoresAccess
import nl.hexmaster.leakout.ui.levels.LevelLibraryScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.levels.asHistoryReader
import nl.hexmaster.leakout.ui.settings.AppInfo
import nl.hexmaster.leakout.ui.settings.SettingsScreen
import nl.hexmaster.leakout.ui.settings.SettingsViewModel
import nl.hexmaster.leakout.ui.splash.SplashGate
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.ExitConfirmation
import nl.hexmaster.leakout.ui.title.StartupVersion
import nl.hexmaster.leakout.ui.title.WelcomeScreen
import nl.hexmaster.leakout.ui.update.UpdateGateHost
import nl.hexmaster.leakout.update.UpdateViewModel

/** The screens, until a navigation library is worth adding. */
private sealed interface Screen {
    data object Welcome : Screen
    data object Library : Screen
    data object Settings : Screen

    /** The achievements, from the local store; reached from Settings, and Back returns there. */
    data object Achievements : Screen
    data class Game(val level: Level) : Screen

    /** The level designer; only reachable while [FeatureFlags.levelDesigner] is on. */
    data object Designer : Screen

    /** A test play of a design: nothing is recorded, and leaving returns to the designer. */
    data class TestPlay(val level: Level) : Screen

    /**
     * Today's daily pipe, with the stack dealt from the server's seed. Nothing goes to the level
     * history or the achievements, and no bundled level or page unlocks; the result goes to the server.
     */
    data class DailyPipe(val game: DailyPipeGame) : Screen
}

/**
 * Keeps the designer across activity recreation: a recreated activity returns to the designer, and
 * a test play returns to it too, its design kept by the view model. Every other screen starts again
 * on the welcome screen, as before.
 */
private val ScreenSaver = Saver<Screen, String>(
    save = { screen -> if (screen is Screen.Designer || screen is Screen.TestPlay) DESIGNER_SCREEN else null },
    restore = { saved -> if (saved == DESIGNER_SCREEN && FeatureFlags.levelDesigner) Screen.Designer else Screen.Welcome },
)

private const val DESIGNER_SCREEN = "designer"

class MainActivity : ComponentActivity() {
    private val levels by lazy { BundledLevels(assets) }

    // The player's finished levels, kept on the device across restarts.
    private val history: LevelHistoryRepository by lazy { appContainer.history }
    private val library: LevelLibraryViewModel by viewModels {
        viewModelFactory { initializer { LevelLibraryViewModel(history.asHistoryReader(), levels) } }
    }

    // The player's preferences, on the app's one shared settings DataStore (Context.settingsDataStore).
    private val settings: SettingsRepository by lazy { DataStoreSettingsRepository.create(this) }
    private val exitConfirmation by lazy { ExitConfirmation(settings) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                SettingsViewModel(
                    settings,
                    history,
                    musicPlaylist,
                    onProgressReset = appContainer.tracker::reconcile,
                    achievements = appContainer.achievements,
                    signIn = appContainer.playGames.auth.state,
                    onTipsReset = { appContainer.tracker.tipsReset() },
                    onRemindersChanged = { enabled ->
                        appContainer.reminders.refresh()
                        if (enabled) requestNotificationPermission()
                    },
                )
            }
        }
    }

    private val achievementsViewModel: AchievementsViewModel by viewModels {
        viewModelFactory { initializer { AchievementsViewModel(appContainer.achievements, appContainer.playGames.auth.state) } }
    }

    // The bundled playlist, which is also the music credits on the About panel; null when unreadable.
    private val musicPlaylist by lazy { BundledMusic.load(assets) }

    // The background music, one stream across every screen, kept across configuration changes.
    private val musicViewModel: MusicViewModel by viewModels {
        viewModelFactory {
            initializer {
                MusicViewModel(
                    settings = settings,
                    store = DataStoreMusicRotationStore.create(this@MainActivity),
                    playlist = musicPlaylist,
                    engine = MediaPlayerMusicEngine(assets),
                    focus = AudioManagerAudioFocus(this@MainActivity),
                )
            }
        }
    }

    // The Daily Pipe button: only for Play Games players, and only in a build with a score server.
    private val dailyPipeViewModel: DailyPipeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = appContainer
                DailyPipeViewModel(
                    dailyPipes = container.dailyPipes,
                    signIn = if (container.scoreServerConfig.enabled) container.playGames.auth.state else flowOf(SignInState.SignedOut),
                    online = container.network.online(),
                )
            }
        }
    }

    // The game's sound effects, loaded once and kept across configuration changes.
    private val soundEffectsViewModel: SoundEffectsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val catalog = SoundCatalog.from(assets.list(SoundCatalog.ASSET_DIR).orEmpty().asList())
                SoundEffectsViewModel(
                    settings,
                    catalog,
                    SoundPoolSoundEffectPlayer(assets, catalog.files),
                    MediaPlayerLoopingSoundPlayer(assets),
                )
            }
        }
    }

    // The game's haptic feedback, following the stored vibration setting.
    private val hapticsViewModel: HapticsViewModel by viewModels {
        viewModelFactory {
            initializer { HapticsViewModel(settings, VibratorHapticPlayer(applicationContext)) }
        }
    }

    // The design being edited, kept across test plays, configuration changes and process death.
    // Created on first use, so it never exists while the level designer flag is off.
    private val designerViewModel: LevelDesignerViewModel by viewModels {
        viewModelFactory {
            initializer { LevelDesignerViewModel(levels, AndroidLevelExporter(this@MainActivity), createSavedStateHandle()) }
        }
    }

    // Google Play update checks and the update screens' state (openspec/specs/app-updates).
    private val updateViewModel: UpdateViewModel by viewModels {
        viewModelFactory { initializer { UpdateViewModel(appContainer.appUpdates, appContainer.updateGate) } }
    }

    // Read once: the installed version does not change while the process runs.
    private val appInfo: AppInfo by lazy {
        @Suppress("DEPRECATION") // The int-flag overload works on every supported API level (min 26).
        val info = packageManager.getPackageInfo(packageName, 0)
        AppInfo(versionName = info.versionName.orEmpty(), versionCode = PackageInfoCompat.getLongVersionCode(info))
    }

    // The UMP consent flow and the ad that opens a session from the menu (openspec/specs/session-start-ads, ad-consent).
    private val consent by lazy { ConsentManager(this) }
    private val sessionStartAds by lazy { SessionStartAds(appContainer.ads) }

    // Set when a daily pipe reminder was tapped; the welcome screen opens the daily pipe once and clears it.
    private var openDailyPipeFromReminder by mutableStateOf(false)

    // The Android 13+ notification permission; the answer re-plans the reminders.
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        appContainer.scope.launch { appContainer.reminders.refresh() }
    }

    /** Asks for the notification permission when Android 13+ needs it and it is not granted yet. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canNotify(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** The welcome screen is up: asks for the permission once per install, never at the first frame. */
    private suspend fun requestNotificationPermissionOnce() {
        val store = appContainer.reminderState
        if (store.permissionAsked()) return
        store.setPermissionAsked()
        requestNotificationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: swaps the splash theme for Theme.LeakOut. The system splash is
        // dismissed on the first frame, which is the Compose splash on the same colour.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Dark styles: transparent bars with light icons over surface.deep whenever they show.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        hideSystemBars()
        // Music plays while the activity is visible: it pauses on stop and resumes in place on start.
        val music = musicViewModel.music
        openDailyPipeFromReminder = savedInstanceState == null && intent.getBooleanExtra(ReminderNotifications.EXTRA_OPEN_DAILY_PIPE, false)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    music.onForeground()
                    // Every open, cold start or return, cancels the pending reminders and plans a new window.
                    appContainer.scope.launch { appContainer.reminders.onAppOpened() }
                }

                override fun onStop(owner: LifecycleOwner) = music.onBackground()
            },
        )
        // Play Games clients need an activity: the silent sign-in check and the achievement sync run on this one.
        val playGames = appContainer.playGames
        // Only levels from the library offer the top 10; a test play never does.
        val topScores = TopScoresAccess.forLibraryLevel(appContainer.scoreServerConfig, appContainer.topScores, playGames.auth.state) {
            playGames.auth.signIn(this@MainActivity)
        }
        playGames.attach(this)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    playGames.detach(this@MainActivity)
                    appContainer.ads.detach(this@MainActivity)
                }
            },
        )
        // Ads are requested only once the consent flow allows it; the SDK starts off the main thread.
        val ads = appContainer.ads
        ads.attach(this)
        consent.gather(this) {
            appContainer.scope.launch(Dispatchers.IO) {
                ads.initializeSdk()
                withContext(Dispatchers.Main) { ads.start() }
            }
        }
        setContent {
            LeakOutTheme {
                SplashGate {
                    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Welcome) }
                    // Every visit opens on the page asked for, not the one left last time.
                    val openLibrary = { page: Int ->
                        library.reopen(page)
                        screen = Screen.Library
                    }
                    // The only way into the designer, and a no-op while its flag is off.
                    val openDesigner = { if (FeatureFlags.levelDesigner) screen = Screen.Designer }
                    // A required update replaces every screen but a level; the level is finished first.
                    UpdateGateHost(
                        viewModel = updateViewModel,
                        inLevel = screen is Screen.Game || screen is Screen.TestPlay || screen is Screen.DailyPipe,
                        onWelcome = screen == Screen.Welcome,
                        onCloseApp = { finishAffinity() },
                    ) { mayStartLevel ->
                        when (val current = screen) {
                            is Screen.Game -> {
                                val next = remember(current.level) { levels.next(current.level) }
                                GameScreen(
                                    level = current.level,
                                    onExit = { screen = Screen.Welcome },
                                    onNextLevel = next?.let { { screen = Screen.Game(it) } },
                                    history = history,
                                    achievements = appContainer.tracker,
                                    soundEffects = soundEffectsViewModel.soundEffects,
                                    haptics = hapticsViewModel.haptics,
                                    tips = settings,
                                    scores = appContainer.scores,
                                    topScores = topScores,
                                    mayStartLevel = mayStartLevel,
                                )
                            }
                            is Screen.DailyPipe -> {
                                val game = current.game
                                // Every attempt, restarts included, deals the same sections from the server's seed.
                                val session = remember(game) { GameSession(game.level, GameEngine.seeded(game.stackSeed)) }
                                val dailyPipes = appContainer.dailyPipes
                                val results = remember(game) { DailyResults { score, stars -> dailyPipes.submit(game.date, score, stars) } }
                                GameScreen(
                                    level = game.level,
                                    onExit = { screen = Screen.Welcome },
                                    session = session,
                                    soundEffects = soundEffectsViewModel.soundEffects,
                                    haptics = hapticsViewModel.haptics,
                                    tips = settings,
                                    mayStartLevel = mayStartLevel,
                                    levelLabel = stringResource(R.string.daily_pipe_label),
                                    daily = results,
                                )
                            }
                            is Screen.TestPlay -> TestPlayScreen(
                                level = current.level,
                                onExit = { screen = Screen.Designer },
                                soundEffects = soundEffectsViewModel.soundEffects,
                                haptics = hapticsViewModel.haptics,
                            )
                            Screen.Designer -> LevelDesignerScreen(
                                viewModel = designerViewModel,
                                onTestPlay = { screen = Screen.TestPlay(it) },
                                onBack = { screen = Screen.Welcome },
                            )
                            Screen.Library -> LevelLibraryScreen(
                                viewModel = library,
                                onStartLevel = { id -> levels.load(id)?.let { level -> sessionStartAds.beforeMenuStart { screen = Screen.Game(level) } } },
                                onBack = { screen = Screen.Welcome },
                            )
                            Screen.Settings -> {
                                val privacyOptionsRequired by consent.privacyOptionsRequired.collectAsState()
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    appInfo = appInfo,
                                    onBack = { screen = Screen.Welcome },
                                    onSoundPreview = soundEffectsViewModel.soundEffects::preview,
                                    onMusicVolumePreview = music::previewVolume,
                                    onSignIn = { playGames.auth.signIn(this@MainActivity) },
                                    onOpenAchievements = { screen = Screen.Achievements },
                                    privacyOptionsRequired = privacyOptionsRequired,
                                    onPrivacyOptions = { consent.showPrivacyOptions(this@MainActivity) },
                                )
                            }
                            Screen.Achievements -> AchievementsScreen(
                                viewModel = achievementsViewModel,
                                onBack = { screen = Screen.Settings },
                                onOpenPlayGames = { showPlayGamesAchievements(this@MainActivity) },
                            )
                            Screen.Welcome -> {
                                val dailyPipe by dailyPipeViewModel.state.collectAsState()
                                val openedDailyPipe by dailyPipeViewModel.opened.collectAsState()
                                // A downloaded pipe opens the game once; the gate is asked like any other level start.
                                LaunchedEffect(Unit) { requestNotificationPermissionOnce() }
                                // A tapped daily pipe reminder opens the pipe as soon as the button is usable.
                                LaunchedEffect(openDailyPipeFromReminder) {
                                    if (!openDailyPipeFromReminder) return@LaunchedEffect
                                    val usable = withTimeoutOrNull(REMINDER_OPEN_WAIT_MS) {
                                        snapshotFlow { dailyPipe.enabled }.first { it }
                                    }
                                    openDailyPipeFromReminder = false
                                    if (usable != null) dailyPipeViewModel.open()
                                }
                                LaunchedEffect(openedDailyPipe) {
                                    val game = openedDailyPipe ?: return@LaunchedEffect
                                    dailyPipeViewModel.consumeOpened()
                                    if (mayStartLevel()) sessionStartAds.beforeMenuStart { screen = Screen.DailyPipe(game) }
                                }
                                WelcomeScreen(
                                    resolveNextLevel = ::resolveNextLevel,
                                    onStartLevel = { id -> levels.load(id)?.let { level -> sessionStartAds.beforeMenuStart { screen = Screen.Game(level) } } },
                                    onOpenLevelLibrary = openLibrary,
                                    onSettings = { screen = Screen.Settings },
                                    onExit = { finishAffinity() },
                                    onOpenLevelDesigner = if (FeatureFlags.levelDesigner) openDesigner else null,
                                    shouldConfirmExit = exitConfirmation::shouldAsk,
                                    onExitAnswered = exitConfirmation::answered,
                                    versionLabel = if (versionLabelShown) null else StartupVersion.format(appInfo),
                                    onVersionLabelShown = { versionLabelShown = true },
                                    dailyPipe = dailyPipe,
                                    onDailyPipe = dailyPipeViewModel::open,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** The next level from a snapshot of the history, read when the player picks on the play menu. */
    private suspend fun resolveNextLevel(): NextLevel = withContext(Dispatchers.IO) {
        NextLevelResolver.resolve(levels.levelCount(), history.asHistoryReader().stars().first())
    }

    private companion object {
        // Process lifetime: the version label shows once when the app starts, not on every return.
        @Volatile
        var versionLabelShown = false

        // How long a tapped reminder waits for the Daily Pipe button to become usable (sign-in, network).
        const val REMINDER_OPEN_WAIT_MS = 5_000L
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs and the notification shade can restore the bars; hide them again on return.
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

