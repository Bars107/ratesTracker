package com.bars.exchange.tracker.domain.model

/**
 * Represents a trading asset or symbol in the domain layer.
 */
data class Asset(
    val symbol: String,      // e.g., "BTCUSDT"
    val baseAsset: String,   // e.g., "BTC"
    val quoteAsset: String,  // e.g., "USDT"
    val status: String       // e.g., "TRADING", "BREAK", etc.
)
