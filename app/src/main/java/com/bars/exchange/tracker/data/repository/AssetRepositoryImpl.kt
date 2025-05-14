package com.bars.exchange.tracker.data.repository

import com.bars.exchange.tracker.data.datasource.IDataSource
import com.bars.exchange.tracker.data.datasource.TickerData
import com.bars.exchange.tracker.di.LocalDataSourceAnnotation
import com.bars.exchange.tracker.di.RemoteDataSourceAnnotation
import com.bars.exchange.tracker.domain.model.Asset
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of the IAssetRepository interface.
 * It uses a RemoteDataSource to fetch fresh data and a LocalDataSource as a cache and single source of truth.
 */
class AssetRepositoryImpl @Inject constructor(
    @RemoteDataSourceAnnotation private val remoteDataSource: IDataSource,
    @LocalDataSourceAnnotation private val localDataSource: IDataSource
) : IAssetRepository {

    override suspend fun getAvailableAssets(): Result<List<Asset>> = withContext(Dispatchers.IO) {
        // Simple approach: try remote first, then fallback to local if needed
        println("AssetRepository: Attempting to fetch from remote data source...")
        
        try {
            // Get remote data first
            val remoteResult = remoteDataSource.getAvailableAssets()
            
            if (remoteResult.isSuccess) {
                // Remote fetch succeeded
                val assetInfoList = remoteResult.getOrNull()!!
                println("AssetRepository: Successfully fetched from remote. Updating local cache...")
                
                // Update local cache in a separate IO context to avoid blocking
                withContext(Dispatchers.IO) {
                    try {
                        localDataSource.clearAllAssets()
                        localDataSource.saveAssets(assetInfoList)
                        println("AssetRepository: Local cache updated.")
                    } catch (e: Exception) {
                        println("AssetRepository: Error updating local cache: ${e.message}")
                    }
                }
                
                // Map to domain model and return
                val domainAssets = assetInfoList.map {
                    Asset(
                        symbol = it.symbol,
                        baseAsset = it.baseAsset,
                        quoteAsset = it.quoteAsset,
                        status = it.status
                    )
                }
                return@withContext Result.success(domainAssets)
            } else {
                // Remote fetch failed, try local cache
                println("AssetRepository: Remote fetch failed. Falling back to local cache...")
                val localResult = withContext(Dispatchers.IO) {
                    localDataSource.getAvailableAssets()
                }
                
                if (localResult.isSuccess) {
                    // Local fetch succeeded
                    val assetInfoList = localResult.getOrNull()!!
                    println("AssetRepository: Successfully fetched from local cache.")
                    
                    // Map to domain model and return
                    val domainAssets = assetInfoList.map {
                        Asset(
                            symbol = it.symbol,
                            baseAsset = it.baseAsset,
                            quoteAsset = it.quoteAsset,
                            status = it.status
                        )
                    }
                    return@withContext Result.success(domainAssets)
                } else {
                    // Both remote and local failed
                    val error = localResult.exceptionOrNull() ?: Exception("Unknown error")
                    println("AssetRepository: Local fetch failed: ${error.message}")
                    Result.failure(error)
                }
            }
        } catch (e: Exception) {
            println("AssetRepository: Error fetching assets: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getTickerUpdates(symbols: List<String>): Flow<TickerData> {
        // For now, ticker updates are directly from the remote source.
        // Future enhancements could include caching or offline handling for ticker data if feasible.
        return remoteDataSource.getTickerUpdates(symbols)
    }

    override fun stopTickerUpdates(symbols: List<String>) {
        remoteDataSource.stopTickerUpdates(symbols)
    }

    override fun stopAllTickerUpdates() {
        remoteDataSource.stopAllTickerUpdates()
    }
}
