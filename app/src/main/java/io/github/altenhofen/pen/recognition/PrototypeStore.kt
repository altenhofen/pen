package io.github.altenhofen.pen.recognition

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.altenhofen.pen.calibration.CalibrationPayload
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(tableName = "prototype_clusters")
internal data class ClusterRow(
    @PrimaryKey @ColumnInfo(name = "cluster_id") val clusterId: String,
    val label: String,
    val packed: ByteArray,
)

@Dao
internal abstract class PrototypeDao {
    @Query("SELECT * FROM prototype_clusters")
    abstract fun all(): List<ClusterRow>

    @Query("SELECT * FROM prototype_clusters WHERE cluster_id = :clusterId")
    abstract fun find(clusterId: String): ClusterRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(rows: List<ClusterRow>)

    @Query("DELETE FROM prototype_clusters")
    abstract fun deleteAll()

    @Transaction
    open fun replaceAll(rows: List<ClusterRow>) {
        require(rows.isNotEmpty()) { "replaceAll requires at least one cluster" }
        deleteAll()
        upsert(rows)
    }
}

@Database(entities = [ClusterRow::class], version = 2, exportSchema = false)
internal abstract class PrototypeDatabase : RoomDatabase() {
    abstract fun prototypes(): PrototypeDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS prototype_clusters (" +
                "cluster_id TEXT NOT NULL, label TEXT NOT NULL, packed BLOB NOT NULL, PRIMARY KEY(cluster_id))",
        )
        db.execSQL(
            "INSERT INTO prototype_clusters (cluster_id, label, packed) " +
                "SELECT 'legacy:' || label, label, packed FROM prototypes",
        )
        db.execSQL("DROP TABLE prototypes")
    }
}

internal class PrototypeStore(private val dao: PrototypeDao) {
    fun loadOrSeed(): List<PrototypeCluster> {
        seedIfEmpty()
        return dao.all().map { it.toCluster() }
    }

    fun replaceAll(clusters: List<PrototypeCluster>) {
        require(clusters.isNotEmpty()) { "replaceAll requires at least one cluster" }
        dao.replaceAll(clusters.map { it.toRow() })
    }

    private fun seedIfEmpty() {
        if (dao.all().isEmpty()) dao.upsert(seedClusters().map { it.toRow() })
    }

    fun upsert(cluster: PrototypeCluster) {
        dao.upsert(listOf(cluster.toRow()))
    }

    fun adapt(clusterId: ClusterId, sample: FeatureVector, feedback: Feedback): PrototypeCluster {
        val current = requireNotNull(dao.find(clusterId.value)) { "unknown cluster ${clusterId.value}" }.toCluster()
        val prototype = current.vector.copyValues()
        val values = when (feedback) {
            Feedback.Accepted -> PrototypeUpdate.attract(prototype, sample.copyValues(), PrototypeUpdate.ACCEPT_REWARD)
            Feedback.Rejected -> PrototypeUpdate.repel(prototype, sample.copyValues())
        }
        val updated = current.copy(vector = FeatureVector.from(values))
        upsert(updated)
        return updated
    }

    fun commitTraining(payload: CalibrationPayload) {
        seedIfEmpty()
        dao.upsert(payload.clusters.map { it.toRow() })
    }

    companion object {
        @Volatile
        private var instance: PrototypeStore? = null

        fun open(context: Context): PrototypeStore {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: PrototypeStore(
                    Room.databaseBuilder(
                        context.applicationContext,
                        PrototypeDatabase::class.java,
                        "prototypes.db",
                    ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build().prototypes(),
                ).also { instance = it }
            }
        }
    }
}

private fun PrototypeCluster.toRow(): ClusterRow {
    val values = vector.copyValues()
    val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    values.forEach { buffer.putFloat(it) }
    return ClusterRow(id.value, label.toString(), buffer.array())
}

private fun ClusterRow.toCluster(): PrototypeCluster {
    val buffer = ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN)
    val values = FloatArray(packed.size / Float.SIZE_BYTES) { buffer.float }
    return PrototypeCluster(ClusterId(clusterId), label.single(), FeatureVector.from(values))
}
