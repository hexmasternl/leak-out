package nl.hexmaster.leakout

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.withContext
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
import nl.hexmaster.leakout.ui.designer.AndroidLevelExporter
import nl.hexmaster.leakout.ui.designer.LevelDesignerScreen
import nl.hexmaster.leakout.ui.designer.LevelDesignerViewModel
import nl.hexmaster.leakout.ui.designer.TestPlayScreen
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.levels.asHistoryReader
import nl.hexmaster.leakout.ui.settings.AppInfo
import nl.hexmaster.leakout.ui.settings.SettingsScreen
import nl.hexmaster.leakout.ui.settings.SettingsViewModel
import nl.hexmaster.leakout.ui.splash.SplashGate
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.ExitConfirmation
import nl.hexmaster.leakout.ui.title.WelcomeScreen

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

    // Read once: the installed version does not change while the process runs.
    private val appInfo: AppInfo by lazy {
        @Suppress("DEPRECATION") // The int-flag overload works on every supported API level (min 26).
        val info = packageManager.getPackageInfo(packageName, 0)
        AppInfo(versionName = info.versionName.orEmpty(), versionCode = PackageInfoCompat.getLongVersionCode(info))
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
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = music.onForeground()

                override fun onStop(owner: LifecycleOwner) = music.onBackground()
            },
        )
        // Play Games clients need an activity: the silent sign-in check and the achievement sync run on this one.
        val playGames = appContainer.playGames
        playGames.attach(this)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) = playGames.detach(this@MainActivity)
            },
        )
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
                            onStartLevel = { id -> levels.load(id)?.let { screen = Screen.Game(it) } },
                            onBack = { screen = Screen.Welcome },
                        )
                        Screen.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            appInfo = appInfo,
                            onBack = { screen = Screen.Welcome },
                            onSoundPreview = soundEffectsViewModel.soundEffects::preview,
                            onMusicVolumePreview = music::previewVolume,
                            onSignIn = { playGames.auth.signIn(this@MainActivity) },
                            onOpenAchievements = { screen = Screen.Achievements },
                        )
                        Screen.Achievements -> AchievementsScreen(
                            viewModel = achievementsViewModel,
                            onBack = { screen = Screen.Settings },
                            onOpenPlayGames = { showPlayGamesAchievements(this@MainActivity) },
                        )
                        Screen.Welcome -> WelcomeScreen(
                            resolveNextLevel = ::resolveNextLevel,
                            onStartLevel = { id -> levels.load(id)?.let { screen = Screen.Game(it) } },
                            onOpenLevelLibrary = openLibrary,
                            onSettings = { screen = Screen.Settings },
                            onExit = { finishAffinity() },
                            onOpenLevelDesigner = if (FeatureFlags.levelDesigner) openDesigner else null,
                            shouldConfirmExit = exitConfirmation::shouldAsk,
                            onExitAnswered = exitConfirmation::answered,
                        )
                    }
                }
            }
        }
    }

    /** The next level from a snapshot of the history, read when the player picks on the play menu. */
    private suspend fun resolveNextLevel(): NextLevel = withContext(Dispatchers.IO) {
        NextLevelResolver.resolve(levels.levelCount(), history.asHistoryReader().stars().first())
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

