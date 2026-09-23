package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.altenhofen.pen.R

@Composable
internal fun MyWordsScreen(
    words: List<MyWordEntry>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTrain: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.my_words_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.my_words_hint), style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.my_words_add_label)) },
                singleLine = true,
            )
            Button(
                onClick = {
                    val word = draft.trim()
                    if (word.isNotEmpty()) {
                        onAdd(word)
                        draft = ""
                    }
                },
            ) {
                Text(stringResource(R.string.my_words_add_action))
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(words, key = { it.word }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.word, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.my_words_training_count, entry.trainingSamples),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(onClick = { onTrain(entry.word) }) {
                        Text(stringResource(R.string.my_words_train))
                    }
                    TextButton(onClick = { onDelete(entry.word) }) {
                        Text(stringResource(R.string.my_words_delete))
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.action_done))
        }
    }
}

internal data class MyWordEntry(val word: String, val trainingSamples: Int)
