package com.bars.exchange.tracker.data.datasource

import android.util.Log
import com.bars.exchange.tracker.data.datasource.local.dao.AssetDao
import com.bars.exchange.tracker.data.datasource.local.dao.SelectedAssetDao
import com.bars.exchange.tracker.data.datasource.local.dao.TickerUpdateDao
import com.bars.exchange.tracker.data.datasource.local.entity.AssetEntity
import com.bars.exchange.tracker.data.datasource.local.entity.SelectedAssetEntity
import com.bars.exchange.tracker.data.datasource.local.entity.TickerUpdateEntity
import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.TickerUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IDataSource for local Room-based storage.
 */
@Singleton
class LocalDataSource @Inject constructor(
    private val assetDao: AssetDao,
    private val selectedAssetDao: SelectedAssetDao,
    private val tickerUpdateDao: TickerUpdateDao
) : IDataSource { // Implements IDataSource
    companion object {
        private const val TAG = "LocalDataSource"
    }

    // --- Asset Information --- 
    override suspend fun getAvailableAssets(): Result<List<AssetInfo>> {
        return try {
            val entityList = assetDao.getAllAssetsSuspend()
            val infoList = entityList.map {
                // Map AssetEntity to AssetInfo
                AssetInfo(
                    symbol = it.symbol,
                    baseAsset = it.baseAsset,
                    quoteAsset = it.quoteAsset,
                    status = it.status
                    // isSpotTradingAllowed would need to be stored in AssetEntity if required here
                )
            }
            Result.success(infoList) // Wrap in Result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Caching/Local Storage Operations ---
    override suspend fun saveAssets(assets: List<AssetInfo>) {
        val entities = assets.map {
            // Map AssetInfo to AssetEntity
            AssetEntity(
                symbol = it.symbol,
                baseAsset = it.baseAsset,
                quoteAsset = it.quoteAsset,
                status = it.status,
                lastUpdated = System.currentTimeMillis() // Set update time on save
            )
        }
        // Consider clearing old data if this is a full refresh:
        // assetDao.clearAllAssets()
        assetDao.insertAssets(entities)
    }

    override suspend fun clearAllAssets() {
        assetDao.clearAllAssets()
    }

    override suspend fun getAssetsCount(): Int {
        return assetDao.getAssetsCount()
    }

    /**
     * Gets the timestamp of the most recently updated asset.
     * If no assets, returns 0.
     */
    override suspend fun getLastUpdateTime(): Long {
        // This is a simplified way. We query all, sort by lastUpdated descending, take the first.
        val assetList = assetDao.getAllAssets().map { list -> list.maxByOrNull { it.lastUpdated } }.firstOrNull()
        return assetList?.lastUpdated ?: 0L
    }

    // --- Real-time Ticker Operations (No-op for LocalDataSource) ---

    override fun getTickerUpdates(symbols: List<String>): Flow<TickerData> {
        return flow { /* No-op for local data source */ }
    }

    override fun stopTickerUpdates(symbols: List<String>) {
        // No-op
    }

    override fun stopAllTickerUpdates() {
        // No-op
    }
    
    // --- Selected Assets Operations ---
    
    /**
     * Saves selected assets to the database.
     * @param assets The list of assets to save as selected.
     */
    suspend fun saveSelectedAssets(assets: List<Asset>) {
        try {
            // Clear existing selected assets
            selectedAssetDao.clearAllSelectedAssets()
            
            // Convert UI assets to entities and save them
            val entities = assets.map { asset ->
                SelectedAssetEntity(
                    id = asset.id,
                    symbol = asset.symbol,
                    name = asset.name,
                    imageUrl = asset.imageUrl
                )
            }
            
            selectedAssetDao.insertSelectedAssets(entities)
            Log.d(TAG,"Saved ${entities.size} selected assets to database")
        } catch (e: Exception) {
            Log.e(TAG,"Error saving selected assets: ${e.message}")
        }
    }
    
    /**
     * Gets all selected assets from the database.
     * @return A list of UI Asset objects.
     */
    suspend fun getSelectedAssets(): List<Asset> {
        return try {
            val entities = selectedAssetDao.getAllSelectedAssetsSuspend()
            entities.map { entity ->
                Asset(
                    id = entity.id,
                    symbol = entity.symbol,
                    name = entity.name,
                    imageUrl = entity.imageUrl,
                    isSelected = true // Always true for selected assets
                )
            }
        } catch (e: Exception) {
            Log.e(TAG,"Error getting selected assets: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Saves a ticker update to the database.
     * @param update The ticker update to save.
     */
    suspend fun saveTickerUpdate(update: TickerUpdate) {
        try {
            val entity = TickerUpdateEntity(
                symbol = update.symbol,
                price = update.price,
                priceChangePercent = update.priceChangePercent,
                volume = update.volume,
                timestamp = update.timestamp
            )
            tickerUpdateDao.insertTickerUpdate(entity)
            Log.d(TAG,"Saved ticker update for ${update.symbol}")
        } catch (e: Exception) {
            Log.e(TAG,"Error saving ticker update: ${e.message}")
        }
    }
    
    /**
     * Gets a ticker update for a specific symbol.
     * @param symbol The symbol to get the update for.
     * @return The ticker update or null if not found.
     */
    suspend fun getTickerUpdate(symbol: String): TickerUpdate? {
        return try {
            val entity = tickerUpdateDao.getTickerUpdate(symbol)
            entity?.let {
                TickerUpdate(
                    symbol = it.symbol,
                    price = it.price,
                    priceChangePercent = it.priceChangePercent,
                    volume = it.volume,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e(TAG,"Error getting ticker update: ${e.message}")
            null
        }
    }
}
