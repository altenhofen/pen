package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.calibration.CalibrateGlyphs
import io.github.altenhofen.pen.calibration.CalibrationEvent
import io.github.altenhofen.pen.calibration.CalibrationMinigame
import io.github.altenhofen.pen.calibration.GlyphCalibrationPhase
import io.github.altenhofen.pen.calibration.SelectedGlyphs
import io.github.altenhofen.pen.ime.DrawingCanvasView
import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.ime.StylusGate
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.settings.CaptureStyle

@Composable
internal fun CalibrationMinigameScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = remember { CalibrationMinigame() }
    var epoch by remember { mutableIntStateOf(0) }
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
        is GlyphCalibrationPhase.WriteGlyphs -> GlyphWritingPanel(
            sessionLabel = phase.session.currentLabel,
            progress = phase.session.progress,
            awaitingAdvance = phase.session.awaitingAdvance,
            capture = capture,
            onStrokeSettled = { strokes -> act { recordSettled(strokes) } },
            onNext = {
                act {
                    if (next() == CalibrationEvent.ReadyToCommit) {
                        recognizer.commitTraining(payload())
                    }
                }
            },
            onCancel = onDone,
            modifier = modifier,
        )
        GlyphCalibrationPhase.Complete -> Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.calibrate_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.calibrate_complete, game.trainedCount()))
            Button(onClick = onDone) {
                Text(stringResource(R.string.action_done))
            }
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
private fun GlyphWritingPanel(
    sessionLabel: Char?,
    progress: Pair<Int, Int>,
    awaitingAdvance: Boolean,
    capture: CaptureStyle,
    onStrokeSettled: (List<Stroke>) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settled by rememberUpdatedState(onStrokeSettled)
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.calibrate_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(
                R.string.calibrate_write_prompt,
                sessionLabel?.toString().orEmpty(),
                progress.first,
                progress.second,
            ),
        )
        key(sessionLabel) {
            AndroidView(
                factory = { context ->
                    DrawingCanvasView(context, acceptsTool = StylusGate::acceptsTraining).apply {
                        setOnGlyphSettledListener { strokes -> settled(strokes) }
                    }
                },
                update = { view ->
                    view.configure(capture)
                    view.setOnGlyphSettledListener { strokes -> settled(strokes) }
                },
                modifier = Modifier.fillMaxWidth().height(320.dp),
            )
        }
        Button(onClick = onNext, enabled = awaitingAdvance) {
            Text(stringResource(R.string.action_next))
        }
        Button(onClick = onCancel) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}
