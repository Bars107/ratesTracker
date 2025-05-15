package com.bars.exchange.tracker.data.repository

import android.util.Log
import com.bars.exchange.tracker.data.datasource.IDataSource
import com.bars.exchange.tracker.data.datasource.LocalDataSource
import com.bars.exchange.tracker.data.datasource.TickerData
import com.bars.exchange.tracker.di.LocalDataSourceAnnotation
import com.bars.exchange.tracker.di.RemoteDataSourceAnnotation
import com.bars.exchange.tracker.domain.model.Asset
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import com.bars.exchange.tracker.ui.main.mvi.TickerUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.bars.exchange.tracker.ui.main.mvi.Asset as UiAsset

/**
 * Implementation of the IAssetRepository interface.
 * It uses a RemoteDataSource to fetch fresh data and a LocalDataSource as a cache and single source of truth.
 */
class AssetRepositoryImpl @Inject constructor(
    @RemoteDataSourceAnnotation private val remoteDataSource: IDataSource,
    @LocalDataSourceAnnotation private val localDataSource: IDataSource
) : IAssetRepository {

    companion object {
        private const val TAG = "AssetRepositoryImpl"

        // Cache expiration time in milliseconds (1 minute)
        private const val CACHE_EXPIRATION_TIME = 60 * 1000L

        // Throttle duration for ticker updates (5 seconds)
        private const val TICKER_THROTTLE_DURATION = 5 * 1000L
    }
    
    // Last fetch timestamp
    private var lastFetchTimestamp = 0L
    
    // Map to keep track of active subscriptions
    private val activeSubscriptions = ConcurrentHashMap<String, Flow<TickerUpdate>>()

    override suspend fun getAvailableAssets(): Result<List<Asset>> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - lastFetchTimestamp
        val assetsCount = localDataSource.getAssetsCount()
        
        // Check if we have cached data that's not expired
        if (assetsCount > 0 && cacheAge < CACHE_EXPIRATION_TIME) {
            Log.d(TAG, "Using cached assets (age: ${cacheAge/1000}s)")
            // Use cached data
            return@withContext try {
                val localResult = localDataSource.getAvailableAssets()
                if (localResult.isSuccess) {
                    val assetInfoList = localResult.getOrNull()!!
                    val domainAssets = assetInfoList.map {
                        Asset(
                            symbol = it.symbol,
                            baseAsset = it.baseAsset,
                            quoteAsset = it.quoteAsset,
                            status = it.status
                        )
                    }
                    Result.success(domainAssets)
                } else {
                    // If local cache read fails, try remote
                    fetchFromRemote()
                }
            } catch (e: Exception) {
                Log.e(TAG,"Error reading from cache: ${e.message}")
                fetchFromRemote()
            }
        } else {
            Log.d(TAG,"Cache expired or empty, fetching from remote...")
            return@withContext fetchFromRemote()
        }
    }
    
    private suspend fun fetchFromRemote(): Result<List<Asset>> {
        Log.d(TAG,"Attempting to fetch from remote data source...")
        
        return try {
            // Get remote data
            val remoteResult = remoteDataSource.getAvailableAssets()
            
            if (remoteResult.isSuccess) {
                // Remote fetch succeeded
                val assetInfoList = remoteResult.getOrNull()!!
                Log.d(TAG,"Successfully fetched from remote. Updating local cache...")
                
                // Update local cache in a separate IO context to avoid blocking
                withContext(Dispatchers.IO) {
                    try {
                        localDataSource.clearAllAssets()
                        localDataSource.saveAssets(assetInfoList)
                        // Update the last fetch timestamp
                        lastFetchTimestamp = System.currentTimeMillis()
                        Log.d(TAG,"Local cache updated. Timestamp: $lastFetchTimestamp")
                    } catch (e: Exception) {
                        Log.e(TAG,"Error updating local cache: ${e.message}")
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
                Result.success(domainAssets)
            } else {
                // Remote fetch failed, try local cache
                Log.d(TAG,"Remote fetch failed. Falling back to local cache...")
                val localResult = withContext(Dispatchers.IO) {
                    localDataSource.getAvailableAssets()
                }
                
                if (localResult.isSuccess) {
                    // Local fetch succeeded
                    val assetInfoList = localResult.getOrNull()!!
                    Log.d(TAG,"Successfully fetched from local cache.")
                    
                    // Map to domain model and return
                    val domainAssets = assetInfoList.map {
                        Asset(
                            symbol = it.symbol,
                            baseAsset = it.baseAsset,
                            quoteAsset = it.quoteAsset,
                            status = it.status
                        )
                    }
                    Result.success(domainAssets)
                } else {
                    // Both remote and local failed
                    val error = localResult.exceptionOrNull() ?: Exception("Unknown error")
                    Log.e(TAG,"Local fetch failed: ${error.message}")
                    Result.failure(error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG,"Error fetching assets: ${e.message}")
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
        unsubscribeFromAllTickerUpdates()
    }
    
    override fun subscribeToTickerUpdates(symbol: String): Flow<TickerUpdate> {
        Log.d(TAG,"Subscribing to ticker updates for $symbol")
        
        // Get the ticker flow from the remote data source
        val remoteFlow = remoteDataSource.getTickerUpdates(listOf(symbol))
            .filter { it.symbol == symbol } // Only get updates for this symbol
            .distinctUntilChanged() // Only emit when the data changes
            .sample(TICKER_THROTTLE_DURATION) // Sample to max 1 update per 5 seconds
            .map { tickerData ->
                // Convert to UI model
                val tickerUpdate = TickerUpdate(
                    symbol = tickerData.symbol,
                    price = tickerData.price,
                    priceChangePercent = tickerData.priceChangePercent,
                    volume = tickerData.volume,
                    timestamp = tickerData.timestamp
                )
                
                // Save to local cache
                (localDataSource as? LocalDataSource)?.saveTickerUpdate(tickerUpdate)
                
                tickerUpdate
            }
            .catch { e ->
                Log.e(TAG,"Error in ticker subscription for $symbol: ${e.message}")
                // If there's an error, try to emit the last cached value if available
                val cachedUpdate = getLatestTickerUpdate(symbol)
                if (cachedUpdate != null) {
                    emit(cachedUpdate)
                }
            }
            .shareIn(
                CoroutineScope(Dispatchers.IO),
                SharingStarted.Lazily,
                replay = 1
            )
        
        // Store the flow for later reference
        activeSubscriptions[symbol] = remoteFlow
        
        return remoteFlow
    }
    
    override suspend fun getLatestTickerUpdate(symbol: String): TickerUpdate? = withContext(Dispatchers.IO) {
        try {
            // Cast to LocalDataSource to access the getTickerUpdate method
            val localDataSource = localDataSource as? LocalDataSource
                ?: throw IllegalStateException("LocalDataSource is not of the expected type")
            
            val update = localDataSource.getTickerUpdate(symbol)
            if (update != null) {
                Log.d(TAG,"Retrieved cached ticker update for $symbol")
            } else {
                Log.d(TAG,"No cached ticker update found for $symbol")
            }
            update
        } catch (e: Exception) {
            Log.e(TAG,"Error getting latest ticker update: ${e.message}")
            null
        }
    }
    
    override fun unsubscribeFromTickerUpdates(symbol: String) {
        Log.d(TAG,"Unsubscribing from ticker updates for $symbol")
        activeSubscriptions.remove(symbol)
        remoteDataSource.stopTickerUpdates(listOf(symbol))
    }
    
    override fun unsubscribeFromAllTickerUpdates() {
        Log.d(TAG,"Unsubscribing from all ticker updates")
        activeSubscriptions.clear()
        remoteDataSource.stopAllTickerUpdates()
    }
    
    override suspend fun saveSelectedAssets(assets: List<UiAsset>) = withContext(Dispatchers.IO) {
        try {
            // Cast to LocalDataSource to access the saveSelectedAssets method
            (localDataSource as? LocalDataSource)?.saveSelectedAssets(assets)
                ?: throw IllegalStateException("LocalDataSource is not of the expected type")

            Log.d(TAG,"Saved ${assets.size} selected assets to persistent storage")
            return@withContext
        } catch (e: Exception) {
            Log.d(TAG,"Error saving selected assets: ${e.message}")
        }
    }
    
    override suspend fun getSelectedAssets(): List<UiAsset> = withContext(Dispatchers.IO) {
        try {
            // Cast to LocalDataSource to access the getSelectedAssets method
            val localDataSource = localDataSource as? LocalDataSource
                ?: throw IllegalStateException("LocalDataSource is not of the expected type")
            
            val selectedAssets = localDataSource.getSelectedAssets()
            Log.d(TAG,"Loaded ${selectedAssets.size} selected assets from persistent storage")
            selectedAssets
        } catch (e: Exception) {
            Log.d(TAG,"Error loading selected assets: ${e.message}")
            emptyList()
        }
    }
}
