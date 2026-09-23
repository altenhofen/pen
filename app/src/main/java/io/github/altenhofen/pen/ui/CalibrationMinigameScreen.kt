package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.calibration.CalibrateGlyphs
import io.github.altenhofen.pen.calibration.CalibrationEvent
import io.github.altenhofen.pen.calibration.CalibrationMinigame
import io.github.altenhofen.pen.calibration.GestureCalibrationEvent
import io.github.altenhofen.pen.calibration.GestureCalibrationMinigame
import io.github.altenhofen.pen.calibration.GestureCalibrationPhase
import io.github.altenhofen.pen.calibration.GlyphCalibrationPhase
import io.github.altenhofen.pen.calibration.SelectedGlyphs
import io.github.altenhofen.pen.ime.DrawingCanvasView
import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.ime.StylusGate
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.GestureStore
import io.github.altenhofen.pen.settings.CaptureStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CalibrateSection { Glyphs, Gestures }

@Composable
internal fun CalibrationMinigameScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableStateOf(CalibrateSection.Glyphs) }
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = section.ordinal) {
            Tab(
                selected = section == CalibrateSection.Glyphs,
                onClick = { section = CalibrateSection.Glyphs },
                text = { Text(stringResource(R.string.calibrate_tab_glyphs)) },
            )
            Tab(
                selected = section == CalibrateSection.Gestures,
                onClick = { section = CalibrateSection.Gestures },
                text = { Text(stringResource(R.string.calibrate_tab_gestures)) },
            )
        }
        when (section) {
            CalibrateSection.Glyphs -> GlyphCalibrationFlow(recognizer, capture, onDone, Modifier.weight(1f))
            CalibrateSection.Gestures -> GestureCalibrationFlow(recognizer, capture, onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlyphCalibrationFlow(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = remember { CalibrationMinigame() }
    var epoch by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    fun act(block: CalibrationMinigame.() -> Unit) {
        game.block()
        epoch++
    }
    val phase = remember(epoch) { game.phase() }
    when (phase) {
        is GlyphCalibrationPhase.PickGlyphs -> GlyphPickerGrid(
            selected = phase.selected,
            onToggle = { act { toggle(it) } },
            onSelectAll = { act { selectAll() } },
            onSelectNone = { act { selectNone() } },
            onStart = { act { start() } },
            modifier = modifier,
        )
        is GlyphCalibrationPhase.WriteGlyphs -> InkWritingPanel(
            title = stringResource(R.string.calibrate_now_writing, phase.session.currentLabel.toString()),
            sampleLine = stringResource(R.string.calibrate_samples_so_far, phase.session.sampleCount),
            sessionKey = phase.session.currentLabel,
            capture = capture,
            canAdvance = phase.session.canAdvance,
            onSettled = { strokes -> act { recordInk(strokes) } },
            onNext = { leftover ->
                act {
                    recordInk(leftover)
                    val payload = when (val event = next()) {
                        is CalibrationEvent.Advanced -> event.payload
                        is CalibrationEvent.ReadyToCommit -> event.payload
                        else -> null
                    }
                    if (payload != null) {
                        scope.launch(Dispatchers.IO + NonCancellable) { recognizer.commitTraining(payload) }
                    }
                }
            },
            onCancel = onDone,
            modifier = modifier,
        )
        GlyphCalibrationPhase.Complete -> CalibrationComplete(
            message = stringResource(R.string.calibrate_complete, game.trainedCount()),
            onDone = onDone,
            modifier = modifier,
        )
    }
}

@Composable
private fun GestureCalibrationFlow(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember { GestureCalibrationMinigame() }
    var epoch by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val counts = withContext(Dispatchers.IO) { GestureStore.open(context).sampleCounts() }
        game.refreshCounts(counts)
        epoch++
    }
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
            onSettled = { strokes -> act { recordInk(strokes) } },
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
            message = stringResource(R.string.calibrate_gestures_complete, game.trainedCount()),
            onDone = onDone,
            modifier = modifier,
        )
    }
}

@Composable
private fun CalibrationComplete(message: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.calibrate_title), style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Button(onClick = onDone) {
            Text(stringResource(R.string.action_done))
        }
    }
}

@Composable
private fun GlyphPickerGrid(
    selected: Set<Char>,
    onToggle: (Char) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.calibrate_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.calibrate_pick_prompt))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectAll) {
                Text(stringResource(R.string.action_select_all))
            }
            Button(onClick = onSelectNone) {
                Text(stringResource(R.string.action_select_none))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(CalibrateGlyphs.ALL, key = { it }) { glyph ->
                FilterChip(
                    selected = glyph in selected,
                    onClick = { onToggle(glyph) },
                    label = { Text(glyph.toString()) },
                )
            }
        }
        Button(onClick = onStart, enabled = SelectedGlyphs.of(selected) != null) {
            Text(stringResource(R.string.action_start))
        }
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
    modifier: Modifier = Modifier,
) {
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
            }
        }
        Button(onClick = onStart, enabled = selected.isNotEmpty()) {
            Text(stringResource(R.string.action_start))
        }
    }
}

@Composable
private fun InkWritingPanel(
    title: String,
    sampleLine: String,
    sessionKey: Any?,
    capture: CaptureStyle,
    canAdvance: Boolean,
    onSettled: (List<Stroke>) -> Unit,
    onNext: (List<Stroke>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settled by rememberUpdatedState(onSettled)
    var canvas by remember { mutableStateOf<DrawingCanvasView?>(null) }
    Box(modifier = modifier.fillMaxSize()) {
        key(sessionKey) {
            AndroidView(
                factory = { context ->
                    DrawingCanvasView(
                        context,
                        acceptsTool = StylusGate::acceptsTraining,
                        autoSettle = true,
                        keepInkAfterSettle = true,
                    ).also { canvas = it }.apply {
                        setOnGlyphSettledListener { strokes -> settled(strokes) }
                    }
                },
                update = { view ->
                    view.configure(capture)
                    view.setPrompt("")
                    view.setOnGlyphSettledListener { strokes -> settled(strokes) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
        ) {
            Text(stringResource(R.string.action_cancel))
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(sampleLine)
        }
        Button(
            onClick = { onNext(canvas?.takeInk().orEmpty()) },
            enabled = canAdvance,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}
