package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.altenhofen.pen.calibration.CalibrationEvent
import io.github.altenhofen.pen.calibration.CalibrationSession
import io.github.altenhofen.pen.calibration.FineTuneAlphabet
import io.github.altenhofen.pen.ime.DrawingCanvasView
import io.github.altenhofen.pen.ime.StylusGate
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.settings.CaptureStyle

@Composable
internal fun GlyphTrainingScreen(
    title: String,
    session: CalibrationSession,
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onCommit: (CalibrationSession) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember { mutableStateOf(session.currentLabel) }
    var progress by remember { mutableStateOf(session.progress) }

    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        val current = label
        Text(
            if (current == null) {
                "Training saved for ${session.labels.size} characters"
            } else {
                "Draw $current: ${progress.first} of ${progress.second} samples collected"
            },
        )
        AndroidView(
            factory = { context ->
                DrawingCanvasView(context, acceptsTool = StylusGate::acceptsTraining)
                    .apply {
                        setOnGlyphSettledListener { strokes ->
                            if (session.recordSettled(strokes) == CalibrationEvent.ReadyToCommit) {
                                onCommit(session)
                            }
                            label = session.currentLabel
                            progress = session.progress
                        }
                    }
            },
            update = { it.configure(capture) },
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
        Button(onClick = onDone) {
            Text(if (current == null) "Done" else "Cancel")
        }
    }
}

@Composable
internal fun CalibrationScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = remember { CalibrationSession.begin(listOf('8', '9'), samplesPerLabel = 5) }
    GlyphTrainingScreen(
        title = "Calibrate",
        session = session,
        recognizer = recognizer,
        capture = capture,
        onCommit = { recognizer.commitCalibration(it.payload()) },
        onDone = onDone,
        modifier = modifier,
    )
}

@Composable
internal fun CharacterFineTuneScreen(
    recognizer: AdaptiveRecognizer,
    capture: CaptureStyle,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = remember {
        CalibrationSession.begin(
            labels = FineTuneAlphabet,
            samplesPerLabel = 3,
            oneClusterPerSample = true,
        )
    }
    GlyphTrainingScreen(
        title = "Fine-tune alphabet",
        session = session,
        recognizer = recognizer,
        capture = capture,
        onCommit = { recognizer.commitUserTraining(it.payload()) },
        onDone = onDone,
        modifier = modifier,
    )
}
