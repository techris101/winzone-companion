package com.winzone.companion.data.state

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingStateEntity::class], version = 1, exportSchema = false)
abstract class WinZoneDatabase : RoomDatabase() {
    abstract fun offlineQueueDao(): OfflineQueueDao
}
