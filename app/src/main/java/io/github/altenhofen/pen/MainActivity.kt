package io.github.altenhofen.pen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import io.github.altenhofen.pen.ui.CalibrationMinigameScreen
import io.github.altenhofen.pen.ui.SettingsScreen
import io.github.altenhofen.pen.ui.theme.PenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LauncherScreen {
    Settings,
    Calibrate,
}

internal enum class ProfileTransferStatus(@param:StringRes val messageRes: Int) {
    Exported(R.string.transfer_exported),
    ExportFailed(R.string.transfer_export_failed),
    Imported(R.string.transfer_imported),
    ImportFailed(R.string.transfer_import_failed),
}

class MainActivity : ComponentActivity() {
    private val recognizer by lazy { AdaptiveRecognizer.open(applicationContext) }
    private val settingsStore by lazy { MotorSettingsStore.open(applicationContext) }
    private val transfer by lazy {
        ProfileTransfer(settingsStore, PrototypeStore.open(applicationContext), contentResolver)
    }
    private val transferStatus = MutableStateFlow<ProfileTransferStatus?>(null)

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
                            onCalibrate = { screen = LauncherScreen.Calibrate },
                            onSetDefaultKeyboard = {
                                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            onExport = { exportDocument.launch("pen-configuration.zip") },
                            onImport = { importDocument.launch(arrayOf("application/zip", "*/*")) },
                            status = status?.let { getString(it.messageRes) },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.Calibrate -> CalibrationMinigameScreen(
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
            transferStatus.value = withContext(Dispatchers.IO) {
                runCatching { transfer.exportTo(uri) }.fold(
                    onSuccess = { ProfileTransferStatus.Exported },
                    onFailure = { ProfileTransferStatus.ExportFailed },
                )
            }
        }
    }

    private fun handleImport(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            transferStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    transfer.importFrom(uri)
                    recognizer.reload()
                }.fold(
                    onSuccess = { ProfileTransferStatus.Imported },
                    onFailure = { ProfileTransferStatus.ImportFailed },
                )
            }
        }
    }
}
