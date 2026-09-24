package io.github.altenhofen.pen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.ime.DrawingCanvasView
import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.ime.StylusGate
import io.github.altenhofen.pen.settings.CaptureStyle

@Composable
internal fun MyWordTrainingScreen(
    word: String,
    sampleCount: Int,
    capture: CaptureStyle,
    allowFingerInput: Boolean,
    onSample: (List<Stroke>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSampleState = rememberUpdatedState(onSample)
    val fingerInk by rememberUpdatedState(allowFingerInput)
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.my_words_training_title, word), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.my_words_training_progress, sampleCount), style = MaterialTheme.typography.bodyMedium)
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                val density = context.resources.displayMetrics.density
                DrawingCanvasView(
                    context,
                    acceptsTouch = { event -> StylusGate.acceptsImeInk(event, density, fingerInk) },
                ).apply {
                    configure(capture)
                    setOnGlyphSettledListener { strokes ->
                        onSampleState.value(strokes)
                    }
                }
            },
            update = { view -> view.configure(capture) },
        )
        androidx.compose.material3.Button(onClick = onDone) {
            Text(stringResource(R.string.action_done))
        }
    }
}
