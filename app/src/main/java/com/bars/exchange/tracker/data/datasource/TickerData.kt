package com.bars.exchange.tracker.data.datasource

/**
 * Data class representing a ticker update from a data source.
 */
data class TickerData(
    val symbol: String,
    val price: String,
    val priceChangePercent: String,
    val volume: String,
    val timestamp: Long = System.currentTimeMillis()
)
