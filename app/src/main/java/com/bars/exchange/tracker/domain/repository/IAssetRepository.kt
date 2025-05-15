package com.bars.exchange.tracker.domain.repository

import com.bars.exchange.tracker.data.datasource.TickerData
import com.bars.exchange.tracker.domain.model.Asset // Added
import com.bars.exchange.tracker.ui.main.mvi.Asset as UiAsset
import com.bars.exchange.tracker.ui.main.mvi.TickerUpdate
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
    
    /**
     * Saves selected assets to persistent storage.
     * @param assets The list of selected assets to save.
     */
    suspend fun saveSelectedAssets(assets: List<UiAsset>)
    
    /**
     * Loads selected assets from persistent storage.
     * @return A list of UI assets that were previously selected.
     */
    suspend fun getSelectedAssets(): List<UiAsset>
    
    /**
     * Subscribes to ticker updates for a specific symbol.
     * Updates are throttled to a maximum of one update every 5 seconds.
     * @param symbol The symbol to subscribe to.
     * @return A Flow emitting ticker updates for the symbol.
     */
    fun subscribeToTickerUpdates(symbol: String): Flow<TickerUpdate>
    
    /**
     * Gets the latest cached ticker update for a symbol.
     * @param symbol The symbol to get the update for.
     * @return The latest ticker update or null if none exists.
     */
    suspend fun getLatestTickerUpdate(symbol: String): TickerUpdate?
    
    /**
     * Unsubscribes from ticker updates for a specific symbol.
     * @param symbol The symbol to unsubscribe from.
     */
    fun unsubscribeFromTickerUpdates(symbol: String)
    
    /**
     * Unsubscribes from all ticker updates.
     */
    fun unsubscribeFromAllTickerUpdates()
}
