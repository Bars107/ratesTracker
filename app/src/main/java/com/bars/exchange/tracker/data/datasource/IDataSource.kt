package com.bars.exchange.tracker.data.datasource

import kotlinx.coroutines.flow.Flow

// AssetInfo is defined in data/datasource/AssetInfo.kt
// TickerData is defined below or could be in its own file.

/**
 * Consolidated interface for data source operations, covering both local and remote.
 * Implementations will provide logic for relevant methods and no-ops for others.
 */
interface IDataSource {

    // --- Asset Information (can be from Remote or Local) ---

    /**
     * Fetches the list of available trading symbols/assets.
     * - RemoteDataSource: Fetches from API and returns as a Result.
     * - LocalDataSource: Fetches from DB, maps to AssetInfo, and returns as Result<List<AssetInfo>>.
     */
    suspend fun getAvailableAssets(): Result<List<AssetInfo>>

    // --- Caching/Local Storage Operations (primarily for LocalDataSource) ---

    /**
     * Saves a list of assets to the data source (primarily local cache).
     * @param assets The list of AssetInfo to save.
     */
    suspend fun saveAssets(assets: List<AssetInfo>)

    /**
     * Clears all assets from the data source (primarily local cache).
     */
    suspend fun clearAllAssets()

    /**
     * Gets the count of assets currently stored (primarily in local cache).
     */
    suspend fun getAssetsCount(): Int

    /**
     * Gets the timestamp of the last update (primarily for local cache).
     */
    suspend fun getLastUpdateTime(): Long

    // --- Real-time Ticker Operations (primarily for RemoteDataSource) ---

    /**
     * Establishes a WebSocket connection and streams real-time ticker updates for the given symbols.
     */
    fun getTickerUpdates(symbols: List<String>): Flow<TickerData>

    /**
     * Stops listening for ticker updates for specified symbols.
     */
    fun stopTickerUpdates(symbols: List<String>)

    /**
     * Stops all ticker updates and closes connections if applicable.
     */
    fun stopAllTickerUpdates()
}

// Define a simple data class for real-time ticker data (can be moved to its own file)
data class TickerData(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String // 24hr percentage change
    // Add other relevant fields from Binance's ticker stream as needed
)
