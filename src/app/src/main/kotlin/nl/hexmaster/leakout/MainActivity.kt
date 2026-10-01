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
import androidx.compose.runtime.setValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffectsViewModel
import nl.hexmaster.leakout.audio.SoundPoolSoundEffectPlayer
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.core.library.NextLevelResolver
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository
import nl.hexmaster.leakout.settings.DataStoreSettingsRepository
import nl.hexmaster.leakout.settings.SettingsRepository
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.levels.asHistoryReader
import nl.hexmaster.leakout.ui.settings.AppInfo
import nl.hexmaster.leakout.ui.settings.SettingsScreen
import nl.hexmaster.leakout.ui.settings.SettingsViewModel
import nl.hexmaster.leakout.ui.splash.SplashGate
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen

/** The screens, until a navigation library is worth adding. */
private sealed interface Screen {
    data object Welcome : Screen
    data object Library : Screen
    data object Settings : Screen
    data class Game(val level: Level) : Screen
}

class MainActivity : ComponentActivity() {
    private val levels by lazy { BundledLevels(assets) }

    // The player's finished levels, kept on the device across restarts.
    private val history: LevelHistoryRepository by lazy { RoomLevelHistoryRepository(LeakOutDatabase.get(this)) }
    private val library: LevelLibraryViewModel by viewModels {
        viewModelFactory { initializer { LevelLibraryViewModel(history.asHistoryReader(), levels) } }
    }

    // The player's preferences, on the app's one shared settings DataStore (Context.settingsDataStore).
    private val settings: SettingsRepository by lazy { DataStoreSettingsRepository.create(this) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        viewModelFactory { initializer { SettingsViewModel(settings, history) } }
    }

    // The game's sound effects, loaded once and kept across configuration changes.
    private val soundEffectsViewModel: SoundEffectsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val catalog = SoundCatalog.from(assets.list(SoundCatalog.ASSET_DIR).orEmpty().asList())
                SoundEffectsViewModel(settings, catalog, SoundPoolSoundEffectPlayer(assets, catalog.files))
            }
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
        setContent {
            LeakOutTheme {
                SplashGate {
                    var screen by remember { mutableStateOf<Screen>(Screen.Welcome) }
                    // Every visit opens on the page asked for, not the one left last time.
                    val openLibrary = { page: Int ->
                        library.reopen(page)
                        screen = Screen.Library
                    }
                    when (val current = screen) {
                        is Screen.Game -> {
                            val next = remember(current.level) { levels.next(current.level) }
                            GameScreen(
                                level = current.level,
                                onExit = { screen = Screen.Welcome },
                                onNextLevel = next?.let { { screen = Screen.Game(it) } },
                                history = history,
                                soundEffects = soundEffectsViewModel.soundEffects,
                            )
                        }
                        Screen.Library -> LevelLibraryScreen(
                            viewModel = library,
                            onStartLevel = { id -> levels.load(id)?.let { screen = Screen.Game(it) } },
                            onBack = { screen = Screen.Welcome },
                        )
                        Screen.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            appInfo = appInfo,
                            onBack = { screen = Screen.Welcome },
                        )
                        Screen.Welcome -> WelcomeScreen(
                            resolveNextLevel = ::resolveNextLevel,
                            onStartLevel = { id -> levels.load(id)?.let { screen = Screen.Game(it) } },
                            onOpenLevelLibrary = openLibrary,
                            onSettings = { screen = Screen.Settings },
                            onExit = { finishAffinity() },
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

