package com.bars.exchange.tracker.ui.main.mvi

/**
 * Data class representing a ticker update for UI display.
 */
data class TickerUpdate(
    val symbol: String,
    val price: String,
    val priceChangePercent: String,
    val volume: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPositiveChange: Boolean
        get() = priceChangePercent.toDoubleOrNull()?.let { it >= 0 } ?: false
        
    val formattedPriceChange: String
        get() {
            val percentValue = priceChangePercent.toDoubleOrNull() ?: 0.0
            val sign = if (percentValue >= 0) "+" else ""
            return "$sign$priceChangePercent%"
        }
}
