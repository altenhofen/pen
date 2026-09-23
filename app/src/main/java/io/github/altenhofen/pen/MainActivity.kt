package io.github.altenhofen.pen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.altenhofen.pen.profile.ProfileFailure
import io.github.altenhofen.pen.profile.ProfileTransfer
import io.github.altenhofen.pen.profile.ProfileTransferException
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.CustomWordStore
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordMemoryStore
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.wordFeatures
import io.github.altenhofen.pen.settings.AppLocales
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore
import io.github.altenhofen.pen.ui.CalibrationMinigameScreen
import io.github.altenhofen.pen.ui.MyWordEntry
import io.github.altenhofen.pen.ui.MyWordTrainingScreen
import io.github.altenhofen.pen.ui.MyWordsScreen
import io.github.altenhofen.pen.ui.PassphraseRequest
import io.github.altenhofen.pen.ui.SettingsScreen
import io.github.altenhofen.pen.ui.theme.PenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class LauncherScreen {
    Settings,
    Calibrate,
    MyWords,
    MyWordTrain,
}

internal enum class ProfileTransferStatus(@param:StringRes val messageRes: Int) {
    Exported(R.string.transfer_exported),
    ExportFailed(R.string.transfer_export_failed),
    Imported(R.string.transfer_imported),
    ImportFailed(R.string.transfer_import_failed),
    WrongPassphrase(R.string.transfer_wrong_passphrase),
}

class MainActivity : AppCompatActivity() {
    private val recognizer by lazy { AdaptiveRecognizer.open(applicationContext) }
    private val settingsStore by lazy { MotorSettingsStore.open(applicationContext) }
    private val wordStore by lazy { WordMemoryStore.open(applicationContext) }
    private val customWordStore by lazy { CustomWordStore.open(applicationContext) }
    private val transfer by lazy {
        ProfileTransfer(
            settingsStore,
            PrototypeStore.open(applicationContext),
            wordStore,
            customWordStore,
            contentResolver,
        )
    }
    private val transferStatus = MutableStateFlow<ProfileTransferStatus?>(null)
    private val passphraseRequest = MutableStateFlow<PassphraseRequest?>(null)

    private var exportPassphrase: CharArray? = null

    private val exportDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(ARCHIVE_MIME_TYPE),
    ) { uri -> handleExport(uri) }

    private val importDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> handleImportFile(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PenTheme {
                val current by settingsStore.values.collectAsState(initial = MotorSettings.Default)
                val status by transferStatus.collectAsState()
                val passphrase by passphraseRequest.collectAsState()
                val scope = rememberCoroutineScope()
                var screen by rememberSaveable { mutableStateOf(LauncherScreen.Settings) }
                var appLanguage by remember { mutableStateOf(AppLocales.current()) }
                var trainingWord by rememberSaveable { mutableStateOf("") }
                var trainingSamples by rememberSaveable { mutableIntStateOf(0) }
                var myWords by remember { mutableStateOf(loadMyWords()) }
                BackHandler(enabled = screen != LauncherScreen.Settings) {
                    screen = when (screen) {
                        LauncherScreen.MyWordTrain -> LauncherScreen.MyWords
                        else -> LauncherScreen.Settings
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screen) {
                        LauncherScreen.Settings -> SettingsScreen(
                            current = current,
                            onUpdate = { transform -> scope.launch { settingsStore.update(transform) } },
                            onCalibrate = { screen = LauncherScreen.Calibrate },
                            onMyWords = { myWords = loadMyWords(); screen = LauncherScreen.MyWords },
                            onSetDefaultKeyboard = {
                                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            onExport = { passphraseRequest.value = PassphraseRequest.Export },
                            onImport = { importDocument.launch(arrayOf("*/*")) },
                            passphraseRequest = passphrase,
                            onPassphrase = ::onPassphrase,
                            onPassphraseCancelled = { passphraseRequest.value = null },
                            appLanguage = appLanguage,
                            onAppLanguage = { language ->
                                appLanguage = language
                                AppLocales.choose(language)
                            },
                            status = status?.let { getString(it.messageRes) },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.Calibrate -> CalibrationMinigameScreen(
                            recognizer = recognizer,
                            capture = current.capture(),
                            onDone = { screen = LauncherScreen.Settings },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.MyWords -> MyWordsScreen(
                            words = myWords,
                            onAdd = { word ->
                                scope.launch(Dispatchers.IO) {
                                    customWordStore.add(word)
                                    myWords = loadMyWords()
                                }
                            },
                            onDelete = { word ->
                                scope.launch(Dispatchers.IO) {
                                    customWordStore.remove(word)
                                    wordStore.removeWord(word)
                                    myWords = loadMyWords()
                                }
                            },
                            onTrain = { word ->
                                trainingWord = word
                                trainingSamples = wordStore.load().pinnedTrainingCount(word)
                                screen = LauncherScreen.MyWordTrain
                            },
                            onBack = { screen = LauncherScreen.Settings },
                            modifier = Modifier.padding(innerPadding),
                        )
                        LauncherScreen.MyWordTrain -> MyWordTrainingScreen(
                            word = trainingWord,
                            sampleCount = trainingSamples,
                            capture = current.capture(),
                            onSample = { strokes ->
                                val vector = wordFeatures(strokes) ?: return@MyWordTrainingScreen
                                scope.launch(Dispatchers.IO) {
                                    val sample = WordSample(
                                        "custom:$trainingWord:${UUID.randomUUID()}",
                                        trainingWord,
                                        vector,
                                        System.currentTimeMillis(),
                                        pinned = true,
                                    )
                                    val memory = wordStore.load()
                                    val pinned = memory.samples.count {
                                        it.word == trainingWord && it.pinned
                                    }
                                    if (pinned >= WordMemory.PINNED_TRAINING_CAP) return@launch
                                    val (next, evicted) = memory.remember(sample)
                                    wordStore.apply(sample, evicted)
                                    trainingSamples = next.pinnedTrainingCount(trainingWord)
                                    myWords = loadMyWords()
                                }
                            },
                            onDone = { screen = LauncherScreen.MyWords },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }

    private fun loadMyWords(): List<MyWordEntry> {
        val memory = wordStore.load()
        return customWordStore.load().map { word ->
            MyWordEntry(word, memory.pinnedTrainingCount(word))
        }
    }

    private fun onPassphrase(request: PassphraseRequest, passphrase: CharArray) {
        passphraseRequest.value = null
        when (request) {
            PassphraseRequest.Export -> {
                exportPassphrase = passphrase
                exportDocument.launch(ARCHIVE_FILE_NAME)
            }
            is PassphraseRequest.Import -> handleImport(request.uri, passphrase)
        }
    }

    private fun handleExport(uri: Uri?) {
        val passphrase = exportPassphrase
        exportPassphrase = null
        if (uri == null || passphrase == null) {
            passphrase?.fill('\u0000')
            return
        }
        lifecycleScope.launch {
            transferStatus.value = withContext(Dispatchers.IO) {
                try {
                    runCatching { transfer.exportTo(uri, passphrase) }.fold(
                        onSuccess = { ProfileTransferStatus.Exported },
                        onFailure = { ProfileTransferStatus.ExportFailed },
                    )
                } finally {
                    passphrase.fill('\u0000')
                }
            }
        }
    }

    private fun handleImportFile(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            val encrypted = withContext(Dispatchers.IO) { runCatching { transfer.needsPassphrase(uri) } }
            encrypted.fold(
                onSuccess = { needed ->
                    if (needed) passphraseRequest.value = PassphraseRequest.Import(uri) else handleImport(uri, null)
                },
                onFailure = { transferStatus.value = ProfileTransferStatus.ImportFailed },
            )
        }
    }

    private fun handleImport(uri: Uri, passphrase: CharArray?) {
        lifecycleScope.launch {
            transferStatus.value = withContext(Dispatchers.IO) {
                try {
                    runCatching {
                        transfer.importFrom(uri, passphrase)
                        recognizer.reload()
                    }.fold(
                        onSuccess = { ProfileTransferStatus.Imported },
                        onFailure = { error -> failureStatus(error) },
                    )
                } finally {
                    passphrase?.fill('\u0000')
                }
            }
        }
    }

    private fun failureStatus(error: Throwable): ProfileTransferStatus =
        if (error is ProfileTransferException && error.failure == ProfileFailure.Passphrase) {
            ProfileTransferStatus.WrongPassphrase
        } else {
            ProfileTransferStatus.ImportFailed
        }

    private companion object {
        const val ARCHIVE_MIME_TYPE = "application/octet-stream"
        const val ARCHIVE_FILE_NAME = "pen-profile.penbak"
    }
}
