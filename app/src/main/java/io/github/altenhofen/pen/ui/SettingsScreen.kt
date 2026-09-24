package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.settings.AppLanguage
import io.github.altenhofen.pen.settings.HandwritingLanguage
import io.github.altenhofen.pen.settings.InkLanguage
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.SpaceAfterSuggestion
import java.util.Locale
import kotlin.math.roundToLong

private enum class RunwayPanel {
    ReadyToWrite,
    TeachRecognition,
    InkAndTiming,
    Language,
    ProfileBackup,
}

private object SettingsRunway {
    val defaultOrder: List<RunwayPanel> = listOf(
        RunwayPanel.ReadyToWrite,
        RunwayPanel.TeachRecognition,
        RunwayPanel.InkAndTiming,
        RunwayPanel.Language,
        RunwayPanel.ProfileBackup,
    )
}

@Composable
internal fun SettingsScreen(
    current: MotorSettings,
    onUpdate: ((MotorSettings) -> MotorSettings) -> Unit,
    onCalibrate: () -> Unit,
    onGestures: () -> Unit,
    onMyWords: () -> Unit,
    onSetDefaultKeyboard: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    passphraseRequest: PassphraseRequest? = null,
    onPassphrase: (PassphraseRequest, CharArray) -> Unit = { _, _ -> },
    onPassphraseCancelled: () -> Unit = {},
    appLanguage: AppLanguage?,
    onAppLanguage: (AppLanguage?) -> Unit,
    status: String? = null,
    modifier: Modifier = Modifier,
) {
    if (passphraseRequest != null) {
        PassphraseDialog(
            request = passphraseRequest,
            onSubmit = { passphrase -> onPassphrase(passphraseRequest, passphrase) },
            onDismiss = onPassphraseCancelled,
        )
    }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        SettingsRunway.defaultOrder.forEach { panel ->
            when (panel) {
                RunwayPanel.ReadyToWrite -> OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onCalibrate, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_calibrate))
                        }
                        Button(onClick = onSetDefaultKeyboard, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_set_default_keyboard))
                        }
                    }
                }
                RunwayPanel.TeachRecognition -> OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onMyWords, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_my_words))
                        }
                        Button(onClick = onGestures, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_gestures))
                        }
                    }
                }
                RunwayPanel.InkAndTiming -> OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SettingSwitch(
                            title = stringResource(R.string.space_after_full_word),
                            hint = stringResource(R.string.space_after_full_word_hint),
                            checked = current.spaceAfterFullWord,
                            onCheckedChange = { enabled ->
                                onUpdate { settings -> settings.withSpaceAfterFullWord(enabled) }
                            },
                        )
                        LanguagePicker(
                            label = stringResource(R.string.space_after_suggestion),
                            options = SpaceAfterSuggestion.entries,
                            selected = current.spaceAfterSuggestion,
                            optionLabel = {
                                when (it) {
                                    SpaceAfterSuggestion.Off ->
                                        stringResource(R.string.space_after_suggestion_off)
                                    SpaceAfterSuggestion.On ->
                                        stringResource(R.string.space_after_suggestion_on)
                                    SpaceAfterSuggestion.Smart ->
                                        stringResource(R.string.space_after_suggestion_smart)
                                }
                            },
                            onPick = { choice ->
                                onUpdate { settings -> settings.withSpaceAfterSuggestion(choice) }
                            },
                        )
                        Text(
                            stringResource(R.string.space_after_suggestion_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SettingSwitch(
                            title = stringResource(R.string.recognize_spaces_in_handwriting),
                            hint = stringResource(R.string.recognize_spaces_in_handwriting_hint),
                            checked = current.recognizeSpacesInHandwriting,
                            onCheckedChange = { enabled ->
                                onUpdate { settings -> settings.withRecognizeSpacesInHandwriting(enabled) }
                            },
                        )
                        HorizontalDivider()
                        SettingSlider(
                            label = stringResource(R.string.settle_window),
                            value = current.settleMillis.toFloat(),
                            range = MotorSettings.MIN_SETTLE_MILLIS.toFloat()..MotorSettings.MAX_SETTLE_MILLIS.toFloat(),
                            valueText = { stringResource(R.string.settle_window_value, it.roundToLong()) },
                            onCommit = { onUpdate { settings -> settings.withSettleMillis(it.roundToLong()) } },
                        )
                        SettingSlider(
                            label = stringResource(R.string.stroke_width),
                            value = current.strokeWidthDp,
                            range = MotorSettings.MIN_STROKE_WIDTH_DP..MotorSettings.MAX_STROKE_WIDTH_DP,
                            valueText = { stringResource(R.string.stroke_width_value, it) },
                            onCommit = { onUpdate { settings -> settings.withStrokeWidthDp(it) } },
                        )
                        SettingSwitch(
                            title = stringResource(R.string.allow_finger_input),
                            hint = stringResource(R.string.allow_finger_input_hint),
                            checked = current.allowFingerInput,
                            onCheckedChange = { enabled ->
                                onUpdate { settings -> settings.withAllowFingerInput(enabled) }
                            },
                        )
                        SettingSwitch(
                            title = stringResource(R.string.double_tap_for_space),
                            hint = stringResource(R.string.double_tap_for_space_hint),
                            checked = current.doubleTapForSpace,
                            onCheckedChange = { enabled ->
                                onUpdate { settings -> settings.withDoubleTapForSpace(enabled) }
                            },
                        )
                    }
                }
                RunwayPanel.Language -> OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.language_section),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LanguagePicker(
                            label = stringResource(R.string.app_language),
                            options = APP_LANGUAGE_OPTIONS,
                            selected = appLanguage,
                            optionLabel = {
                                it?.let { language -> nativeName(language.tag) }
                                    ?: stringResource(R.string.app_language_system_default)
                            },
                            onPick = onAppLanguage,
                        )
                        LanguagePicker(
                            label = stringResource(R.string.handwriting_language),
                            options = HANDWRITING_OPTIONS,
                            selected = current.handwriting,
                            optionLabel = {
                                when (it) {
                                    HandwritingLanguage.FollowApp ->
                                        stringResource(R.string.handwriting_language_follow_app)
                                    is HandwritingLanguage.Explicit -> nativeName(it.language.tag)
                                }
                            },
                            onPick = { choice ->
                                onUpdate { settings -> settings.withHandwriting(choice) }
                            },
                        )
                    }
                }
                RunwayPanel.ProfileBackup -> OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_export_profile))
                        }
                        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_import_profile))
                        }
                    }
                }
            }
        }
        if (status != null) {
            Text(status)
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val APP_LANGUAGE_OPTIONS: List<AppLanguage?> = listOf(null) + AppLanguage.entries

private val HANDWRITING_OPTIONS: List<HandwritingLanguage> =
    listOf(HandwritingLanguage.FollowApp) + InkLanguage.entries.map(HandwritingLanguage::Explicit)

private fun nativeName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
}

@Composable
private fun <T> LanguagePicker(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(optionLabel(selected))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    val text = optionLabel(option)
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            open = false
                            onPick(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: @Composable (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    Column {
        Text(stringResource(R.string.slider_label_format, label, valueText(draft)))
        Slider(
            value = draft,
            onValueChange = { draft = it },
            valueRange = range,
            onValueChangeFinished = { onCommit(draft) },
        )
    }
}
