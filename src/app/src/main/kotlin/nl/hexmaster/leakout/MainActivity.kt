package nl.hexmaster.leakout

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.splash.SplashGate
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen

/** The two screens until the play menu and level library bring their own navigation. */
private sealed interface Screen {
    data object Welcome : Screen
    data class Game(val level: Level) : Screen
}

class MainActivity : ComponentActivity() {
    private val firstLevel: Level by lazy { loadLevelOne() }

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
                    when (val current = screen) {
                        is Screen.Game -> GameScreen(level = current.level, onExit = { screen = Screen.Welcome })
                        Screen.Welcome -> WelcomeScreen(
                            onPlay = { screen = Screen.Game(firstLevel) },
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

    private fun loadLevelOne(): Level {
        val text = assets.open("levels/level_001.json").bufferedReader().use { it.readText() }
        return when (val result = LevelParser.parse(text)) {
            is ParseResult.Success -> result.level
            is ParseResult.Failure -> error("Cannot load level 1: ${result.error.message}")
        }
    }
}

