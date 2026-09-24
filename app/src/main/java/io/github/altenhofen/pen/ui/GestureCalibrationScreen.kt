package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.calibration.GestureCalibrationEvent
import io.github.altenhofen.pen.calibration.GestureCalibrationPayload
import io.github.altenhofen.pen.calibration.GestureCalibrationMinigame
import io.github.altenhofen.pen.calibration.GestureCalibrationPhase
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.GestureStore
import io.github.altenhofen.pen.settings.CaptureStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GestureCalibrationScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember { GestureCalibrationMinigame() }
    var epoch by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    suspend fun reloadSampleCounts() {
        val counts = withContext(Dispatchers.IO) { GestureStore.open(context).sampleCounts() }
        withContext(Dispatchers.Main) {
            game.refreshCounts(counts)
            epoch++
        }
    }
    LaunchedEffect(Unit) { reloadSampleCounts() }
    fun act(block: GestureCalibrationMinigame.() -> Unit) {
        game.block()
        epoch++
    }
    val phase = remember(epoch) { game.phase() }
    when (phase) {
        is GestureCalibrationPhase.PickActions -> GesturePickerGrid(
            selected = phase.selected,
            sampleCounts = phase.sampleCounts,
            onToggle = { act { toggle(it) } },
            onSelectAll = { act { selectAll() } },
            onSelectNone = { act { selectNone() } },
            onStart = { act { start() } },
            onClearTraining = { action ->
                scope.launch(Dispatchers.IO + NonCancellable) {
                    recognizer.clearGestureTraining(setOf(action))
                    reloadSampleCounts()
                }
            },
            onClearAllTraining = {
                scope.launch(Dispatchers.IO + NonCancellable) {
                    recognizer.clearAllGestureTraining()
                    reloadSampleCounts()
                }
            },
            onCancel = onDone,
            modifier = modifier,
        )
        is GestureCalibrationPhase.WriteGestures -> InkWritingPanel(
            title = stringResource(
                R.string.calibrate_now_drawing_gesture,
                stringResource(phase.session.currentAction!!.labelRes),
            ),
            sampleLine = stringResource(
                R.string.calibrate_gesture_samples,
                phase.session.sampleCount,
                GestureAction.MIN_TRAINING_SAMPLES,
            ),
            sessionKey = phase.session.currentAction,
            capture = capture,
            canAdvance = phase.session.canAdvance,
            onSettled = { strokes ->
                var payload: GestureCalibrationPayload? = null
                act {
                    if (recordInk(strokes) is GestureCalibrationEvent.Recorded) {
                        payload = payloadForCurrentAction()
                    }
                }
                payload?.let { p ->
                    scope.launch(Dispatchers.IO + NonCancellable) { recognizer.commitGestureTraining(p) }
                }
            },
            onNext = { leftover ->
                act {
                    recordInk(leftover)
                    val payload = when (val event = next()) {
                        is GestureCalibrationEvent.Advanced -> event.payload
                        is GestureCalibrationEvent.ReadyToCommit -> event.payload
                        else -> null
                    }
                    if (payload != null) {
                        scope.launch(Dispatchers.IO + NonCancellable) { recognizer.commitGestureTraining(payload) }
                    }
                }
            },
            onCancel = onDone,
            modifier = modifier,
        )
        GestureCalibrationPhase.Complete -> CalibrationComplete(
            title = stringResource(R.string.calibrate_gestures_title),
            message = stringResource(R.string.calibrate_gestures_complete, game.trainedCount()),
            onDone = onDone,
            modifier = modifier,
        )
    }
}

@Composable
private fun GesturePickerGrid(
    selected: Set<GestureAction>,
    sampleCounts: Map<GestureAction, Int>,
    onToggle: (GestureAction) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onStart: () -> Unit,
    onClearTraining: (GestureAction) -> Unit,
    onClearAllTraining: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAnyTraining = sampleCounts.values.any { it > 0 }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.calibrate_gestures_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.calibrate_gestures_pick_prompt))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectAll) {
                Text(stringResource(R.string.action_select_all))
            }
            Button(onClick = onSelectNone) {
                Text(stringResource(R.string.action_select_none))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(GestureAction.catalog, key = { it.id }) { action ->
                val count = sampleCounts[action] ?: 0
                val trained = count >= GestureAction.MIN_TRAINING_SAMPLES
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FilterChip(
                        selected = action in selected,
                        onClick = { onToggle(action) },
                        label = {
                            Text(
                                stringResource(
                                    if (trained) R.string.calibrate_gesture_chip_trained else R.string.calibrate_gesture_chip,
                                    stringResource(action.labelRes),
                                    count,
                                    GestureAction.MIN_TRAINING_SAMPLES,
                                ),
                            )
                        },
                    )
                    if (count > 0) {
                        TextButton(onClick = { onClearTraining(action) }) {
                            Text(stringResource(R.string.gesture_clear_training))
                        }
                    }
                }
            }
        }
        if (hasAnyTraining) {
            TextButton(onClick = onClearAllTraining) {
                Text(stringResource(R.string.gesture_clear_all_training))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(onClick = onStart, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.action_start))
            }
        }
    }
}
