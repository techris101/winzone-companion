package com.winzone.companion.data.state

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "pending_state")
data class PendingStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    val payloadJson: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface OfflineQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingStateEntity): Long

    @Query("SELECT * FROM pending_state ORDER BY createdAtMs ASC LIMIT :limit")
    suspend fun getOldest(limit: Int = 30): List<PendingStateEntity>

    @Query("DELETE FROM pending_state WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_state WHERE createdAtMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM pending_state")
    suspend fun count(): Int

    @Query("DELETE FROM pending_state WHERE id NOT IN (SELECT id FROM pending_state ORDER BY createdAtMs DESC LIMIT :keepCount)")
    suspend fun trimQueue(keepCount: Int = 60)
}
