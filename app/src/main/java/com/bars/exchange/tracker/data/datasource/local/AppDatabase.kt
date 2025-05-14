package com.bars.exchange.tracker.data.datasource.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bars.exchange.tracker.data.datasource.local.dao.AssetDao
import com.bars.exchange.tracker.data.datasource.local.entity.AssetEntity

/**
 * The Room database for the application.
 */
@Database(entities = [AssetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exchange_tracker_db"
                )
                // .fallbackToDestructiveMigration() // Consider migration strategy for production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
