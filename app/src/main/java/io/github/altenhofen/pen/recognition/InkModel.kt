package io.github.altenhofen.pen.recognition

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import io.github.altenhofen.pen.ime.Stroke

sealed interface InkModelState {
    data object Checking : InkModelState
    data object Downloading : InkModelState
    data object Ready : InkModelState
    data class Unavailable(val reason: String) : InkModelState
}

/** Google ML Kit Digital Ink, downloaded once and then fully on-device. */
internal class InkModel(
    languageTag: String,
    private val onState: (InkModelState) -> Unit,
) {
    private val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
    private val model = identifier?.let { DigitalInkRecognitionModel.builder(it).build() }
    private val models = RemoteModelManager.getInstance()
    private var recognizer: DigitalInkRecognizer? = null

    var state: InkModelState = InkModelState.Checking
        private set(value) {
            field = value
            onState(value)
        }

    fun ensureReady() {
        val model = model ?: run {
            state = InkModelState.Unavailable("unsupported language")
            return
        }
        if (state == InkModelState.Ready || state == InkModelState.Downloading) return
        state = InkModelState.Checking
        models.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) open(model) else download(model)
            }
            .addOnFailureListener { state = InkModelState.Unavailable(it.message ?: "check failed") }
    }

    private fun download(model: DigitalInkRecognitionModel) {
        state = InkModelState.Downloading
        models.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { open(model) }
            .addOnFailureListener { state = InkModelState.Unavailable("offline") }
    }

    private fun open(model: DigitalInkRecognitionModel) {
        recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        state = InkModelState.Ready
    }

    /** Calls [onResult] exactly once, with an empty list when the model cannot answer. */
    fun recognize(
        strokes: List<Stroke>,
        preContext: String,
        width: Float,
        height: Float,
        onResult: (List<String>) -> Unit,
    ) {
        val client = recognizer
        if (state != InkModelState.Ready || client == null) {
            onResult(emptyList())
            return
        }
        val ink = Ink.builder().apply {
            strokes.forEach { stroke ->
                val builder = Ink.Stroke.builder()
                stroke.points().forEach { builder.addPoint(Ink.Point.create(it.x, it.y)) }
                addStroke(builder.build())
            }
        }.build()
        val context = RecognitionContext.builder()
            .setPreContext(preContext)
            .setWritingArea(WritingArea(width, height))
            .build()
        client.recognize(ink, context)
            .addOnSuccessListener { result -> onResult(result.candidates.map { it.text }) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
    }
}
