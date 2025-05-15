package com.bars.exchange.tracker.data.datasource.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a ticker update in the database.
 */
@Entity(tableName = "ticker_updates")
data class TickerUpdateEntity(
    @PrimaryKey val symbol: String,
    val price: String,
    val priceChangePercent: String,
    val volume: String,
    val timestamp: Long = System.currentTimeMillis()
)
