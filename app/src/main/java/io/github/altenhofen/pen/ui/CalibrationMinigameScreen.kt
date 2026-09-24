package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.calibration.CalibrateGlyphs
import io.github.altenhofen.pen.calibration.CalibrationEvent
import io.github.altenhofen.pen.calibration.CalibrationPayload
import io.github.altenhofen.pen.calibration.CalibrationMinigame
import io.github.altenhofen.pen.calibration.GlyphCalibrationPhase
import io.github.altenhofen.pen.calibration.SelectedGlyphs
import io.github.altenhofen.pen.ime.DrawingCanvasView
import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.ime.StylusGate
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.settings.CaptureStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@Composable
internal fun CalibrationMinigameScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlyphCalibrationFlow(recognizer, capture, onDone, modifier.fillMaxSize())
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
            onSettled = { strokes ->
                var payload: CalibrationPayload? = null
                act {
                    if (recordInk(strokes) is CalibrationEvent.Recorded) {
                        payload = payloadForCurrentLabel()
                    }
                }
                payload?.let { p ->
                    scope.launch(Dispatchers.IO + NonCancellable) { recognizer.commitTraining(p) }
                }
            },
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
            title = stringResource(R.string.calibrate_title),
            message = stringResource(R.string.calibrate_complete, game.trainedCount()),
            onDone = onDone,
            modifier = modifier,
        )
    }
}

@Composable
internal fun CalibrationComplete(
    title: String,
    message: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
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
            CalibrateGlyphs.families.forEach { family ->
                item(
                    key = family.titleRes,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Text(stringResource(family.titleRes))
                }
                items(family.labels, key = { it }) { glyph ->
                    FilterChip(
                        selected = glyph in selected,
                        onClick = { onToggle(glyph) },
                        label = { Text(glyph.toString()) },
                    )
                }
            }
        }
        Button(onClick = onStart, enabled = SelectedGlyphs.of(selected) != null) {
            Text(stringResource(R.string.action_start))
        }
    }
}

@Composable
internal fun InkWritingPanel(
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
