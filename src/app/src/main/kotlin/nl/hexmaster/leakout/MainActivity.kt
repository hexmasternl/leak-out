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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.levels.InMemoryHistory
import nl.hexmaster.leakout.ui.levels.LevelLibraryScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.splash.SplashGate
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen

/** The screens, until a navigation library is worth adding. */
private sealed interface Screen {
    data object Welcome : Screen
    data object Library : Screen
    data class Game(val level: Level) : Screen
}

class MainActivity : ComponentActivity() {
    private val levels by lazy { BundledLevels(assets) }

    // Stands in for the persistent level history until game-level-history provides it.
    private val history = InMemoryHistory()
    private val library: LevelLibraryViewModel by viewModels {
        viewModelFactory { initializer { LevelLibraryViewModel(history, levels) } }
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
                    // Every visit opens on the most useful page, not the one left last time.
                    val openLibrary = {
                        library.reopen()
                        screen = Screen.Library
                    }
                    when (val current = screen) {
                        is Screen.Game -> {
                            val next = remember(current.level) { levels.next(current.level) }
                            GameScreen(
                                level = current.level,
                                onExit = { screen = Screen.Welcome },
                                onNextLevel = next?.let { { screen = Screen.Game(it) } },
                            )
                        }
                        Screen.Library -> LevelLibraryScreen(
                            viewModel = library,
                            onStartLevel = { id -> levels.load(id)?.let { screen = Screen.Game(it) } },
                            onBack = { screen = Screen.Welcome },
                        )
                        Screen.Welcome -> WelcomeScreen(
                            onPlay = openLibrary,
                            onSettings = { /* TODO: navigate to settings once that screen exists. */ },
                            onExit = { finishAffinity() },
                        )
                    }
                }
            }
        }
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

