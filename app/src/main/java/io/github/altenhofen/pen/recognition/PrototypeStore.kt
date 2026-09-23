package io.github.altenhofen.pen.recognition

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(tableName = "prototypes")
data class PrototypeRow(
    @PrimaryKey val label: String,
    val packed: ByteArray,
)

@Dao
interface PrototypeDao {
    @Query("SELECT * FROM prototypes")
    fun all(): List<PrototypeRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(row: PrototypeRow)
}

@Database(entities = [PrototypeRow::class], version = 1, exportSchema = false)
abstract class PrototypeDatabase : RoomDatabase() {
    abstract fun prototypes(): PrototypeDao
}

class PrototypeStore(private val dao: PrototypeDao) {
    fun loadOrSeed(): Map<Char, FloatArray> {
        val existing = dao.all()
        if (existing.isEmpty()) {
            val seeded = seedLabels.associateWith { label ->
                preprocessPolylines(seedPolylines(label))!!.copyValues()
            }
            seeded.forEach { (label, values) -> dao.upsert(row(label, values)) }
            return seeded
        }
        return existing.associate { row -> row.label.single() to unpack(row.packed) }
    }

    fun save(label: Char, values: FloatArray) {
        dao.upsert(row(label, values))
    }

    companion object {
        fun open(context: Context): PrototypeStore {
            val database = Room.databaseBuilder(
                context.applicationContext,
                PrototypeDatabase::class.java,
                "prototypes.db",
            ).allowMainThreadQueries().build()
            return PrototypeStore(database.prototypes())
        }
    }
}

private fun row(label: Char, values: FloatArray): PrototypeRow {
    val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    values.forEach { buffer.putFloat(it) }
    return PrototypeRow(label.toString(), buffer.array())
}

private fun unpack(packed: ByteArray): FloatArray {
    val buffer = ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(packed.size / Float.SIZE_BYTES) { buffer.float }
}
