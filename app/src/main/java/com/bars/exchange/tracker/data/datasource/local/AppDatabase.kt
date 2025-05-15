package com.bars.exchange.tracker.data.datasource.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bars.exchange.tracker.data.datasource.local.dao.AssetDao
import com.bars.exchange.tracker.data.datasource.local.dao.SelectedAssetDao
import com.bars.exchange.tracker.data.datasource.local.dao.TickerUpdateDao
import com.bars.exchange.tracker.data.datasource.local.entity.AssetEntity
import com.bars.exchange.tracker.data.datasource.local.entity.SelectedAssetEntity
import com.bars.exchange.tracker.data.datasource.local.entity.TickerUpdateEntity

/**
 * The Room database for the application.
 */
@Database(entities = [AssetEntity::class, SelectedAssetEntity::class, TickerUpdateEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao

    abstract fun selectedAssetDao(): SelectedAssetDao
    
    abstract fun tickerUpdateDao(): TickerUpdateDao

    companion object {
        // Migration from version 1 to 2: Add selected_assets table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS selected_assets " +
                    "(id TEXT NOT NULL PRIMARY KEY, " +
                    "symbol TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "imageUrl TEXT)"
                )
            }
        }
        
        // Migration from version 2 to 3: Add ticker_updates table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS ticker_updates " +
                    "(symbol TEXT NOT NULL PRIMARY KEY, " +
                    "price TEXT NOT NULL, " +
                    "priceChangePercent TEXT NOT NULL, " +
                    "volume TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL)"
                )
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exchange_tracker_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
