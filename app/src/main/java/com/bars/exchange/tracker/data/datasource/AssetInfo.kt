package com.bars.exchange.tracker.data.datasource

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object (DTO) for an asset/symbol from the Binance API.
 * This is used by the RemoteDataSource.
 */
@Serializable
data class AssetInfo(
    val symbol: String,      // e.g., "BTCUSDT"
    val baseAsset: String,   // e.g., "BTC"
    val quoteAsset: String,  // e.g., "USDT"
    val status: String       // e.g., "TRADING"
    // Add other fields from Binance /exchangeInfo if needed, like:
    // val icebergAllowed: Boolean,
    // val isSpotTradingAllowed: Boolean,
    // val isMarginTradingAllowed: Boolean,
    // val permissions: List<String> // e.g. ["SPOT", "MARGIN"]
)
