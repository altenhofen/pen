package io.github.altenhofen.pen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore
import io.github.altenhofen.pen.ui.CalibrationScreen
import io.github.altenhofen.pen.ui.CharacterFineTuneScreen
import io.github.altenhofen.pen.ui.SettingsScreen
import io.github.altenhofen.pen.ui.theme.PenTheme
import kotlinx.coroutines.launch

private enum class LauncherScreen {
    Settings,
    Calibrate89,
    FineTuneAlphabet,
}

class MainActivity : ComponentActivity() {
    private val recognizer by lazy { AdaptiveRecognizer.open(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = MotorSettingsStore.open(applicationContext)
        setContent {
            PenTheme {
                val current by settings.values.collectAsState(initial = MotorSettings.Default)
                val scope = rememberCoroutineScope()
                var screen by rememberSaveable { mutableStateOf(LauncherScreen.Settings) }
                BackHandler(enabled = screen != LauncherScreen.Settings) { screen = LauncherScreen.Settings }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screen) {
                        LauncherScreen.Settings -> SettingsScreen(
                            current = current,
                            onUpdate = { transform -> scope.launch { settings.update(transform) } },
                            onCalibrate = { screen = LauncherScreen.Calibrate89 },
                            onFineTune = { screen = LauncherScreen.FineTuneAlphabet },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.Calibrate89 -> CalibrationScreen(
                            recognizer = recognizer,
                            capture = current.capture(),
                            onDone = { screen = LauncherScreen.Settings },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.FineTuneAlphabet -> CharacterFineTuneScreen(
                            recognizer = recognizer,
                            capture = current.capture(),
                            onDone = { screen = LauncherScreen.Settings },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
