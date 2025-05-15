package com.bars.exchange.tracker.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bars.exchange.tracker.data.datasource.local.entity.SelectedAssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the selected_assets table.
 */
@Dao
interface SelectedAssetDao {
    /**
     * Inserts a list of selected assets into the database. If an asset already exists, it's replaced.
     * @param assets The list of selected assets to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelectedAssets(assets: List<SelectedAssetEntity>)
    
    /**
     * Retrieves all selected assets from the database.
     * Returns a Flow, so observers are notified of changes.
     * @return A Flow emitting a list of SelectedAssetEntity.
     */
    @Query("SELECT * FROM selected_assets")
    fun getAllSelectedAssets(): Flow<List<SelectedAssetEntity>>
    
    /**
     * Retrieves all selected assets from the database.
     * Suspend function version for one-time queries.
     * @return A list of SelectedAssetEntity.
     */
    @Query("SELECT * FROM selected_assets")
    suspend fun getAllSelectedAssetsSuspend(): List<SelectedAssetEntity>
    
    /**
     * Deletes all selected assets from the table.
     */
    @Query("DELETE FROM selected_assets")
    suspend fun clearAllSelectedAssets()
}
