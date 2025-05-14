package com.bars.exchange.tracker.data.datasource.dto

import com.bars.exchange.tracker.data.datasource.AssetInfo
import kotlinx.serialization.Serializable

/**
 * Represents the top-level response from Binance API /exchangeInfo endpoint.
 */
@Serializable
data class ExchangeInfoResponse(
    // We only care about the symbols for now, but you can add other fields
    // like timezone, serverTime if they become necessary.
    // @Serializable("timezone") val timezone: String, // Use @SerialName for Ktor < 2.0
    // @Serializable("serverTime") val serverTime: Long,
    val symbols: List<AssetInfo>
)
