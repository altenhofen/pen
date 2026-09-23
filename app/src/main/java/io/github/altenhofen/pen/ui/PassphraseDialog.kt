package io.github.altenhofen.pen.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.altenhofen.pen.R

/** The passphrase the launcher is waiting for, and what it will do once it has one. */
internal sealed interface PassphraseRequest {
    /** Typed twice, before the save sheet opens, so a typo cannot lock the user out of their own ink. */
    data object Export : PassphraseRequest

    /** The file is already chosen. One entry either opens it or it does not. */
    data class Import(val uri: Uri) : PassphraseRequest
}

internal const val MIN_PASSPHRASE_LENGTH = 8

@Composable
internal fun PassphraseDialog(
    request: PassphraseRequest,
    onSubmit: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    val confirming = request is PassphraseRequest.Export
    val mismatched = confirming && repeated != passphrase
    val acceptable =
        if (confirming) passphrase.length >= MIN_PASSPHRASE_LENGTH && !mismatched else passphrase.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (confirming) R.string.passphrase_protect_title else R.string.passphrase_unlock_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PassphraseField(R.string.passphrase_label, passphrase) { passphrase = it }
                if (confirming) {
                    PassphraseField(R.string.passphrase_repeat_label, repeated) { repeated = it }
                    Text(stringResource(R.string.passphrase_rule), style = MaterialTheme.typography.bodySmall)
                }
                if (mismatched && repeated.isNotEmpty()) {
                    Text(
                        stringResource(R.string.passphrase_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = acceptable, onClick = { onSubmit(passphrase.toCharArray()) }) {
                Text(stringResource(if (confirming) R.string.action_encrypt_and_save else R.string.action_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PassphraseField(@StringRes labelRes: Int, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
