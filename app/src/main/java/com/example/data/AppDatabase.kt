package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userPrompt: String,
    val status: String,
    val stepsCount: Int,
    val executionLog: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_models")
data class SavedModelEntity(
    @PrimaryKey
    val path: String,
    val filename: String,
    val sizeFormatted: String,
    val quantization: String,
    val parameters: String,
    val architecture: String,
    val isSelected: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

@Dao
interface SavedModelDao {
    @Query("SELECT * FROM saved_models ORDER BY lastUsedTimestamp DESC")
    fun getAllSavedModels(): Flow<List<SavedModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateModel(model: SavedModelEntity)

    @Query("UPDATE saved_models SET isSelected = (path = :selectedPath)")
    suspend fun setSelectedModel(selectedPath: String)

    @Query("DELETE FROM saved_models WHERE path = :path")
    suspend fun deleteModel(path: String)
}

@Database(
    entities = [CommandHistoryEntity::class, SavedModelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun savedModelDao(): SavedModelDao
}
