package com.example.aura.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE status = :status ORDER BY timestamp DESC")
    fun getHistoryByStatus(status: String): Flow<List<ActionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionHistoryEntity): Long

    @Query("DELETE FROM action_history")
    suspend fun clearAll()

    @Query("DELETE FROM action_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
