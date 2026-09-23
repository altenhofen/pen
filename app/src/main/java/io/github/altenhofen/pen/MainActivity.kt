package io.github.altenhofen.pen

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.altenhofen.pen.profile.ProfileTransfer
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore
import io.github.altenhofen.pen.ui.CalibrationScreen
import io.github.altenhofen.pen.ui.CharacterFineTuneScreen
import io.github.altenhofen.pen.ui.SettingsScreen
import io.github.altenhofen.pen.ui.theme.PenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LauncherScreen {
    Settings,
    Calibrate89,
    FineTuneAlphabet,
}

class MainActivity : ComponentActivity() {
    private val recognizer by lazy { AdaptiveRecognizer.open(applicationContext) }
    private val settingsStore by lazy { MotorSettingsStore.open(applicationContext) }
    private val transfer by lazy {
        ProfileTransfer(settingsStore, PrototypeStore.open(applicationContext), contentResolver)
    }
    private val transferStatus = MutableStateFlow<String?>(null)

    private val exportDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> handleExport(uri) }

    private val importDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> handleImport(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PenTheme {
                val current by settingsStore.values.collectAsState(initial = MotorSettings.Default)
                val status by transferStatus.collectAsState()
                val scope = rememberCoroutineScope()
                var screen by rememberSaveable { mutableStateOf(LauncherScreen.Settings) }
                BackHandler(enabled = screen != LauncherScreen.Settings) { screen = LauncherScreen.Settings }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screen) {
                        LauncherScreen.Settings -> SettingsScreen(
                            current = current,
                            onUpdate = { transform -> scope.launch { settingsStore.update(transform) } },
                            onCalibrate = { screen = LauncherScreen.Calibrate89 },
                            onFineTune = { screen = LauncherScreen.FineTuneAlphabet },
                            onExport = { exportDocument.launch("pen-configuration.zip") },
                            onImport = { importDocument.launch(arrayOf("application/zip", "*/*")) },
                            status = status,
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

    private fun handleExport(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                runCatching { transfer.exportTo(uri) }.fold(
                    onSuccess = { "Exported" },
                    onFailure = { "Export failed" },
                )
            }
            transferStatus.value = status
        }
    }

    private fun handleImport(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                runCatching {
                    transfer.importFrom(uri)
                    recognizer.reload()
                }.fold(
                    onSuccess = { "Imported" },
                    onFailure = { "Import failed" },
                )
            }
            transferStatus.value = status
        }
    }
}
