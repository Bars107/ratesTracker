package com.bars.exchange.tracker.data.datasource

import com.bars.exchange.tracker.data.datasource.local.dao.AssetDao
import com.bars.exchange.tracker.data.datasource.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map // Added
import kotlinx.coroutines.flow.flow // Added for consistency if we ever needed to emit Result.Loading for local
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IDataSource for local Room-based storage.
 */
@Singleton
class LocalDataSource @Inject constructor(
    private val assetDao: AssetDao
) : IDataSource { // Implements IDataSource

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
}
