package com.bars.exchange.tracker.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bars.exchange.tracker.data.datasource.local.entity.TickerUpdateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the ticker_updates table.
 */
@Dao
interface TickerUpdateDao {
    /**
     * Inserts a ticker update into the database. If an update for the symbol already exists, it's replaced.
     * @param update The ticker update to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickerUpdate(update: TickerUpdateEntity)
    
    /**
     * Retrieves a ticker update for a specific symbol.
     * @param symbol The symbol to get the update for.
     * @return The ticker update entity or null if not found.
     */
    @Query("SELECT * FROM ticker_updates WHERE symbol = :symbol")
    suspend fun getTickerUpdate(symbol: String): TickerUpdateEntity?
    
    /**
     * Retrieves ticker updates for a list of symbols.
     * @param symbols The list of symbols to get updates for.
     * @return A list of ticker update entities.
     */
    @Query("SELECT * FROM ticker_updates WHERE symbol IN (:symbols)")
    suspend fun getTickerUpdates(symbols: List<String>): List<TickerUpdateEntity>
    
    /**
     * Observes a ticker update for a specific symbol as a Flow.
     * @param symbol The symbol to observe updates for.
     * @return A Flow emitting the ticker update entity or null if not found.
     */
    @Query("SELECT * FROM ticker_updates WHERE symbol = :symbol")
    fun observeTickerUpdate(symbol: String): Flow<TickerUpdateEntity?>
    
    /**
     * Observes ticker updates for a list of symbols as a Flow.
     * @param symbols The list of symbols to observe updates for.
     * @return A Flow emitting a list of ticker update entities.
     */
    @Query("SELECT * FROM ticker_updates WHERE symbol IN (:symbols)")
    fun observeTickerUpdates(symbols: List<String>): Flow<List<TickerUpdateEntity>>
}
