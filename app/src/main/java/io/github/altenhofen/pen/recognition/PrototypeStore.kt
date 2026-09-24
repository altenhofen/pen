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

@Entity(tableName = "word_samples")
internal data class WordSampleRow(
    @PrimaryKey val id: String,
    val word: String,
    val packed: ByteArray,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long,
    val pinned: Boolean = false,
)

@Dao
internal abstract class WordSampleDao {
    @Query("SELECT * FROM word_samples")
    abstract fun all(): List<WordSampleRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(rows: List<WordSampleRow>)

    @Query("DELETE FROM word_samples WHERE id = :id")
    abstract fun deleteOne(id: String)

    @Query("DELETE FROM word_samples WHERE word = :word")
    abstract fun deleteWord(word: String)

    @Query("DELETE FROM word_samples")
    abstract fun deleteAll()

    @Transaction
    open fun apply(inserted: List<WordSampleRow>, deletedIds: List<String>) {
        deletedIds.forEach { deleteOne(it) }
        upsert(inserted)
    }

    @Transaction
    open fun replaceAll(rows: List<WordSampleRow>) {
        deleteAll()
        upsert(rows)
    }
}

@Dao
internal abstract class CustomWordDao {
    @Query("SELECT word FROM custom_words ORDER BY word")
    abstract fun all(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(row: CustomWordRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsertAll(rows: List<CustomWordRow>)

    @Query("DELETE FROM custom_words WHERE word = :word")
    abstract fun delete(word: String)

    @Query("DELETE FROM custom_words")
    abstract fun deleteAll()

    @Transaction
    open fun replaceAll(words: List<String>) {
        deleteAll()
        upsertAll(words.map { CustomWordRow(it) })
    }
}

@Entity(tableName = "custom_words")
internal data class CustomWordRow(
    @PrimaryKey val word: String,
)

@Database(
    entities = [ClusterRow::class, WordSampleRow::class, CustomWordRow::class, GestureClusterRow::class],
    version = 5,
    exportSchema = false,
)
internal abstract class PrototypeDatabase : RoomDatabase() {
    abstract fun prototypes(): PrototypeDao
    abstract fun words(): WordSampleDao
    abstract fun customWords(): CustomWordDao
    abstract fun gestures(): GestureDao

    companion object {
        @Volatile
        private var instance: PrototypeDatabase? = null

        fun open(context: Context): PrototypeDatabase {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrototypeDatabase::class.java,
                    "prototypes.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }

        fun invalidate() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE word_samples ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_words (word TEXT NOT NULL, PRIMARY KEY(word))")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_words (word TEXT NOT NULL, PRIMARY KEY(word))")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS gesture_clusters (" +
                "cluster_id TEXT NOT NULL, action_id TEXT NOT NULL, packed BLOB NOT NULL, PRIMARY KEY(cluster_id))",
        )
        if (!db.hasColumn("word_samples", "pinned")) {
            db.execSQL("ALTER TABLE word_samples ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS word_samples (" +
                "id TEXT NOT NULL, word TEXT NOT NULL, packed BLOB NOT NULL, confirmed_at INTEGER NOT NULL, PRIMARY KEY(id))",
        )
    }
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

    fun adapt(clusterId: ClusterId, sample: FeatureVector, feedback: Feedback): PrototypeCluster? {
        val current = dao.find(clusterId.value)?.toCluster() ?: return null
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
        fun open(context: Context): PrototypeStore = PrototypeStore(PrototypeDatabase.open(context).prototypes())
    }
}

internal class WordMemoryStore(private val dao: WordSampleDao) {
    fun load(): WordMemory = WordMemory(dao.all().map { it.toSample() }.sortedBy { it.confirmedAt })

    fun apply(inserted: WordSample, evicted: List<WordSample>) {
        dao.apply(listOf(inserted.toRow()), evicted.map { it.id })
    }

    fun remove(id: String) {
        dao.deleteOne(id)
    }

    fun removeWord(word: String) {
        dao.deleteWord(word)
    }

    fun replaceAll(samples: List<WordSample>) {
        dao.replaceAll(samples.map { it.toRow() })
    }

    companion object {
        fun open(context: Context): WordMemoryStore = WordMemoryStore(PrototypeDatabase.open(context).words())
    }
}

internal class CustomWordStore(private val dao: CustomWordDao) {
    fun load(): List<String> = dao.all()

    fun add(word: String) {
        require(word.isNotBlank())
        dao.upsert(CustomWordRow(word.trim()))
    }

    fun remove(word: String) {
        dao.delete(word)
    }

    fun replaceAll(words: List<String>) {
        dao.replaceAll(words.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted())
    }

    companion object {
        fun open(context: Context): CustomWordStore = CustomWordStore(PrototypeDatabase.open(context).customWords())
    }
}

internal fun FeatureVector.pack(): ByteArray {
    val values = copyValues()
    val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    values.forEach { buffer.putFloat(it) }
    return buffer.array()
}

internal fun unpack(packed: ByteArray): FloatArray {
    val buffer = ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(packed.size / Float.SIZE_BYTES) { buffer.float }
}

private fun WordSample.toRow() = WordSampleRow(id, word, vector.pack(), confirmedAt, pinned)

private fun WordSampleRow.toSample() = WordSample(id, word, FeatureVector.from(unpack(packed), WORD_SAMPLE_COUNT), confirmedAt, pinned)

private fun PrototypeCluster.toRow() = ClusterRow(id.value, label.toString(), vector.pack())

private fun ClusterRow.toCluster() = PrototypeCluster(ClusterId(clusterId), label.single(), FeatureVector.from(unpack(packed)))
