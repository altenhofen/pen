package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.calibration.GestureCalibrationPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureStoreTest {
    @Test
    fun removeTrainingDropsOnlyMatchingActions() {
        val dao = RecordingGestureDao()
        val store = GestureStore(dao)
        store.commitTraining(payload(GestureAction.Undo, "s1", 2))
        store.commitTraining(payload(GestureAction.Copy, "s2", 1))
        store.removeTraining(setOf(GestureAction.Undo))
        assertEquals(1, store.sampleCounts()[GestureAction.Copy])
        assertTrue(GestureAction.Undo !in store.sampleCounts())
    }

    @Test
    fun removeTrainingBeforeFullCommitDropsOlderSessionClusters() {
        val dao = RecordingGestureDao()
        val store = GestureStore(dao)
        store.commitTraining(payload(GestureAction.DeleteLine, "old-session", GestureAction.MIN_TRAINING_SAMPLES))
        store.removeTraining(setOf(GestureAction.DeleteLine))
        store.commitTraining(payload(GestureAction.DeleteLine, "new-session", GestureAction.MIN_TRAINING_SAMPLES))
        val ids = store.load().map { it.id.value }
        assertEquals(GestureAction.MIN_TRAINING_SAMPLES, ids.size)
        assertEquals(true, ids.all { it.contains("new-session") })
    }

    @Test
    fun clearAllTrainingRemovesEveryCluster() {
        val dao = RecordingGestureDao()
        val store = GestureStore(dao)
        store.commitTraining(payload(GestureAction.Paste, "s3", 3))
        store.clearAllTraining()
        assertTrue(store.load().isEmpty())
    }

    private fun payload(action: GestureAction, sessionId: String, count: Int): GestureCalibrationPayload =
        GestureCalibrationPayload(
            sessionId,
            (0 until count).map { index ->
                GestureCluster(
                    ClusterId.trainingGesture(action.id, sessionId, index),
                    action,
                    FeatureVector.from(FloatArray(SAMPLE_COUNT * 3)),
                )
            },
        )
}

private class RecordingGestureDao : GestureDao() {
    private val rows = LinkedHashMap<String, GestureClusterRow>()

    override fun all(): List<GestureClusterRow> = rows.values.toList()

    override fun upsert(rows: List<GestureClusterRow>) {
        rows.forEach { this.rows[it.clusterId] = it }
    }

    override fun deleteAll() {
        rows.clear()
    }

    override fun deleteForActions(actionIds: List<String>) {
        rows.entries.removeIf { (_, row) -> row.actionId in actionIds }
    }
}
