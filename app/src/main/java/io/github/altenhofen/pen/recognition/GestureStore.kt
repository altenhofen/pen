package io.github.altenhofen.pen.recognition

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import io.github.altenhofen.pen.calibration.GestureCalibrationPayload

@Entity(tableName = "gesture_clusters")
internal data class GestureClusterRow(
    @PrimaryKey @ColumnInfo(name = "cluster_id") val clusterId: String,
    @ColumnInfo(name = "action_id") val actionId: String,
    val packed: ByteArray,
)

@Dao
internal abstract class GestureDao {
    @Query("SELECT * FROM gesture_clusters")
    abstract fun all(): List<GestureClusterRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(rows: List<GestureClusterRow>)

    @Query("DELETE FROM gesture_clusters")
    abstract fun deleteAll()

    @Transaction
    open fun replaceAll(rows: List<GestureClusterRow>) {
        deleteAll()
        if (rows.isNotEmpty()) upsert(rows)
    }
}

internal class GestureStore(private val dao: GestureDao) {
    fun load(): List<GestureCluster> = dao.all().map { it.toCluster() }

    fun replaceAll(clusters: List<GestureCluster>) {
        dao.replaceAll(clusters.map { it.toRow() })
    }

    fun commitTraining(payload: GestureCalibrationPayload) {
        dao.upsert(payload.clusters.map { it.toRow() })
    }

    fun sampleCounts(): Map<GestureAction, Int> =
        load().groupingBy { it.action }.eachCount()

    companion object {
        fun open(context: Context): GestureStore = GestureStore(PrototypeDatabase.open(context).gestures())
    }
}

private fun GestureCluster.toRow() = GestureClusterRow(id.value, action.id, vector.pack())

private fun GestureClusterRow.toCluster(): GestureCluster {
    val action = GestureAction.fromId(actionId) ?: throw IllegalArgumentException("unknown gesture $actionId")
    return GestureCluster(ClusterId(clusterId), action, FeatureVector.from(unpack(packed)))
}
