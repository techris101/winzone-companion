package com.winzone.companion.di

import android.content.Context
import androidx.room.Room
import com.winzone.companion.data.state.OfflineQueueDao
import com.winzone.companion.data.state.WinZoneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WinZoneDatabase {
        return Room.databaseBuilder(
            context,
            WinZoneDatabase::class.java,
            "winzone_database.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideOfflineQueueDao(database: WinZoneDatabase): OfflineQueueDao {
        return database.offlineQueueDao()
    }
}
