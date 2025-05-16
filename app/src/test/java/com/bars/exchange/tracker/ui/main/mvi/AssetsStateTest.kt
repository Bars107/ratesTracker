package com.bars.exchange.tracker.ui.main.mvi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetsStateTest {

    @Test
    fun `test initial state has empty lists and is not loading`() {
        // Given
        val initialState = AssetsState()
        
        // Then
        assertTrue(initialState.assets.isEmpty())
        assertTrue(initialState.filteredAssets.isEmpty())
        assertTrue(initialState.selectedAssets.isEmpty())
        assertFalse(initialState.isLoading)
        assertEquals("", initialState.searchQuery)
    }
    
    @Test
    fun `test state copy maintains values not explicitly changed`() {
        // Given
        val asset = Asset(id = "1", symbol = "BTCUSDT", name = "Bitcoin", imageUrl = null, isSelected = true)
        val initialState = AssetsState(
            assets = listOf(asset),
            filteredAssets = listOf(asset),
            selectedAssets = listOf(asset),
            isLoading = true,
            searchQuery = "BTC"
        )
        
        // When - only change isLoading
        val updatedState = initialState.copy(isLoading = false)
        
        // Then - other properties remain the same
        assertEquals(initialState.assets, updatedState.assets)
        assertEquals(initialState.filteredAssets, updatedState.filteredAssets)
        assertEquals(initialState.selectedAssets, updatedState.selectedAssets)
        assertEquals(initialState.searchQuery, updatedState.searchQuery)
        assertFalse(updatedState.isLoading) // This one changed
    }
    
    @Test
    fun `test state update with new assets`() {
        // Given
        val initialState = AssetsState()
        val assets = listOf(
            Asset(id = "1", symbol = "BTCUSDT", name = "Bitcoin", imageUrl = null, isSelected = false),
            Asset(id = "2", symbol = "ETHUSDT", name = "Ethereum", imageUrl = null, isSelected = false)
        )
        
        // When
        val updatedState = initialState.copy(assets = assets, filteredAssets = assets)
        
        // Then
        assertEquals(2, updatedState.assets.size)
        assertEquals(2, updatedState.filteredAssets.size)
        assertTrue(updatedState.selectedAssets.isEmpty())
    }
    
    @Test
    fun `test state update with selected assets`() {
        // Given
        val assets = listOf(
            Asset(id = "1", symbol = "BTCUSDT", name = "Bitcoin", imageUrl = null, isSelected = true),
            Asset(id = "2", symbol = "ETHUSDT", name = "Ethereum", imageUrl = null, isSelected = false)
        )
        val initialState = AssetsState(assets = assets, filteredAssets = assets)
        
        // When
        val selectedAssets = listOf(assets[0])
        val updatedState = initialState.copy(selectedAssets = selectedAssets)
        
        // Then
        assertEquals(1, updatedState.selectedAssets.size)
        assertEquals("BTCUSDT", updatedState.selectedAssets[0].symbol)
    }
}
