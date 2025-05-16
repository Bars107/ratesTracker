package com.bars.exchange.tracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AssetTest {

    @Test
    fun `test asset creation with correct properties`() {
        // Given
        val symbol = "BTCUSDT"
        val baseAsset = "BTC"
        val quoteAsset = "USDT"
        val status = "TRADING"

        // When
        val asset = Asset(
            symbol = symbol,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            status = status
        )
        
        // Then
        assertEquals(symbol, asset.symbol)
        assertEquals(baseAsset, asset.baseAsset)
        assertEquals(quoteAsset, asset.quoteAsset)
        assertEquals(status, asset.status)
    }
    
    @Test
    fun `test asset copy with modified properties`() {
        // Given
        val originalAsset = Asset(
            symbol = "BTCUSDT",
            baseAsset = "BTC",
            quoteAsset = "USDT",
            status = "TRADING"
        )
        
        // When - copy with modified status
        val updatedAsset = originalAsset.copy(
            status = "BREAK"
        )
        
        // Then - only specified properties should change
        assertEquals(originalAsset.symbol, updatedAsset.symbol)
        assertEquals(originalAsset.baseAsset, updatedAsset.baseAsset)
        assertEquals(originalAsset.quoteAsset, updatedAsset.quoteAsset)
        assertEquals("BREAK", updatedAsset.status) // Changed
        assertEquals("TRADING", originalAsset.status) // Original unchanged
    }
    
    @Test
    fun `test asset equality based on all properties`() {
        // Given
        val asset1 = Asset(
            symbol = "BTCUSDT",
            baseAsset = "BTC",
            quoteAsset = "USDT",
            status = "TRADING"
        )
        
        val asset2 = Asset(
            symbol = "BTCUSDT",
            baseAsset = "BTC",
            quoteAsset = "USDT",
            status = "TRADING"
        )
        
        val asset3 = Asset(
            symbol = "ETHUSDT",
            baseAsset = "ETH",
            quoteAsset = "USDT",
            status = "TRADING"
        )
        
        // Then - assets with same properties should be equal
        assertEquals(asset1, asset2)
        assertFalse(asset1 == asset3)
    }
}
