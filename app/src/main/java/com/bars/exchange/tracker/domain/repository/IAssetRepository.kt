package com.bars.exchange.tracker.domain.repository

import com.bars.exchange.tracker.data.datasource.TickerData
import com.bars.exchange.tracker.domain.model.Asset // Added
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the asset repository.
 * This abstracts the data source from the domain layer (use cases).
 */
interface IAssetRepository {

    /**
     * Gets the list of available trading assets.
     * Returns a Result containing either the list of assets or an error.
     */
    suspend fun getAvailableAssets(): Result<List<Asset>>

    /**
     * Subscribes to real-time ticker updates for a list of symbols.
     */
    fun getTickerUpdates(symbols: List<String>): Flow<TickerData>

    /**
     * Stops updates for specific symbols.
     */
    fun stopTickerUpdates(symbols: List<String>)

    /**
     * Stops all ticker updates.
     */
    fun stopAllTickerUpdates()
}
