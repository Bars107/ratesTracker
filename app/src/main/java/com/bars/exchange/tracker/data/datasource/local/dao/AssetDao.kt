package com.bars.exchange.tracker.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bars.exchange.tracker.data.datasource.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the assets table.
 */
@Dao
interface AssetDao {

    /**
     * Inserts a list of assets into the database. If an asset already exists, it's replaced.
     * @param assets The list of assets to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    /**
     * Retrieves all assets from the database, ordered by symbol.
     * Returns a Flow, so observers are notified of changes.
     * @return A Flow emitting a list of AssetEntity.
     */
    @Query("SELECT * FROM assets ORDER BY symbol ASC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    /**
     * Retrieves a single asset by its symbol.
     * @param symbol The symbol of the asset to retrieve.
     * @return A Flow emitting the AssetEntity or null if not found.
     */
    @Query("SELECT * FROM assets WHERE symbol = :symbol")
    fun getAssetBySymbol(symbol: String): Flow<AssetEntity?>

    /**
     * Deletes all assets from the table.
     */
    @Query("DELETE FROM assets")
    suspend fun clearAllAssets()

    /**
     * Gets the count of assets in the database.
     * Useful for checking if the database is empty.
     */
    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetsCount(): Int
}
