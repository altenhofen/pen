package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.ClusterRow
import io.github.altenhofen.pen.recognition.Feedback
import io.github.altenhofen.pen.recognition.PrototypeDao
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.featuresFromStrokes
import io.github.altenhofen.pen.recognition.seedStrokes
import org.junit.Assert.assertEquals
import org.junit.Test

class CalibrationSessionTest {
    @Test
    fun fiveSamplesPerLabelCommitOneClusterPerLabelOnce() {
        val session = CalibrationSession.begin(listOf('8', '9'), samplesPerLabel = 5)
        val events = (1..5).map { session.recordSettled(seedStrokes('8', jitter = it * 0.05f)) } +
            (1..5).map { session.recordSettled(seedStrokes('9', jitter = it * 0.05f)) }
        assertEquals(List(9) { CalibrationEvent.NeedMoreSamples } + CalibrationEvent.ReadyToCommit, events)
        assertEquals(CalibrationEvent.Ignored, session.recordSettled(seedStrokes('9')))

        val dao = InMemoryPrototypeDao()
        val store = PrototypeStore(dao)
        val recognizer = AdaptiveRecognizer(store)
        recognizer.commitCalibration(session.payload())

        val expectedIds = listOf("cal:${session.sessionId}:8", "cal:${session.sessionId}:9")
        assertEquals(expectedIds, dao.calibrationIds())
        assertEquals(38, dao.all().size)

        recognizer.reload()
        val sample = requireNotNull(featuresFromStrokes(seedStrokes('9', jitter = 0.1f)))
        val adapted = store.adapt(
            requireNotNull(recognizer.recognize(seedStrokes('9', jitter = 0.1f), 0.15f)).winner.clusterId,
            sample,
            Feedback.Accepted,
        )
        assertEquals("cal:${session.sessionId}:9", adapted.id.value)
        val afterAdapt = dao.all().map { it.packed.toList() }

        recognizer.commitCalibration(session.payload())

        assertEquals(expectedIds, dao.calibrationIds())
        assertEquals(38, dao.all().size)
        assertEquals(afterAdapt, dao.all().map { it.packed.toList() })
    }

    @Test
    fun userTrainingStoresOneClusterPerSample() {
        val session = CalibrationSession.begin(listOf('a', 'b'), samplesPerLabel = 2, oneClusterPerSample = true)
        repeat(2) { session.recordSettled(seedStrokes('a', jitter = it * 0.05f)) }
        repeat(2) { session.recordSettled(seedStrokes('b', jitter = it * 0.05f)) }

        val dao = InMemoryPrototypeDao()
        val store = PrototypeStore(dao)
        AdaptiveRecognizer(store).commitUserTraining(session.payload())

        val ids = dao.all().map { it.clusterId }.filter { it.startsWith("user:") }.sorted()
        assertEquals(
            listOf(
                "user:${session.sessionId}:a:0",
                "user:${session.sessionId}:a:1",
                "user:${session.sessionId}:b:0",
                "user:${session.sessionId}:b:1",
            ),
            ids,
        )
        assertEquals(40, dao.all().size)
    }
}

private class InMemoryPrototypeDao : PrototypeDao() {
    private val rows = LinkedHashMap<String, ClusterRow>()

    override fun all(): List<ClusterRow> = rows.values.toList()

    fun calibrationIds(): List<String> = rows.keys.filter { it.startsWith("cal:") }

    override fun find(clusterId: String): ClusterRow? = rows[clusterId]

    override fun countExisting(clusterIds: List<String>): Int = clusterIds.count { it in rows }

    override fun upsert(rows: List<ClusterRow>) {
        rows.forEach { this.rows[it.clusterId] = it }
    }

    override fun deleteAll() {
        rows.clear()
    }
}
