package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.settings.MotorSettings
import kotlin.math.roundToLong

@Composable
internal fun SettingsScreen(
    current: MotorSettings,
    onUpdate: ((MotorSettings) -> MotorSettings) -> Unit,
    onCalibrate: () -> Unit,
    onSetDefaultKeyboard: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    passphraseRequest: PassphraseRequest? = null,
    onPassphrase: (PassphraseRequest, CharArray) -> Unit = { _, _ -> },
    onPassphraseCancelled: () -> Unit = {},
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
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
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
        SettingSlider(
            label = stringResource(R.string.ambiguity_threshold),
            value = current.ambiguityThreshold,
            range = MotorSettings.MIN_AMBIGUITY..MotorSettings.MAX_AMBIGUITY,
            valueText = { stringResource(R.string.ambiguity_value, it) },
            onCommit = { onUpdate { settings -> settings.withAmbiguityThreshold(it) } },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.allow_finger_input))
                Text(
                    stringResource(R.string.allow_finger_input_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = current.allowFingerInput,
                onCheckedChange = { enabled -> onUpdate { settings -> settings.withAllowFingerInput(enabled) } },
            )
        }
        Button(onClick = onCalibrate) {
            Text(stringResource(R.string.action_calibrate))
        }
        Button(onClick = onSetDefaultKeyboard) {
            Text(stringResource(R.string.action_set_default_keyboard))
        }
        Button(onClick = onExport) {
            Text(stringResource(R.string.action_export_profile))
        }
        Button(onClick = onImport) {
            Text(stringResource(R.string.action_import_profile))
        }
        if (status != null) {
            Text(status)
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
