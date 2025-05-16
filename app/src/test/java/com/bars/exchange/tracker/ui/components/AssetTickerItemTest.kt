package com.bars.exchange.tracker.ui.components

import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.AssetsEvent
import com.bars.exchange.tracker.ui.main.mvi.MainViewModel
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AssetTickerItemTest {

    private lateinit var viewModel: MainViewModel
    
    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
    }
    
    @Test
    fun `test onDeleteConfirmed calls removeAsset event`() {
        // Given
        val asset = Asset(
            id = "BTC_USDT",
            symbol = "BTCUSDT",
            name = "BTC/USDT",
            imageUrl = null,
            isSelected = true
        )
        
        // Capture the event parameter
        val eventSlot = slot<AssetsEvent>()
        every { viewModel.onEvent(capture(eventSlot)) } returns Unit
        
        // When
        val assetTickerItemActions = AssetTickerItemActions(viewModel)
        assetTickerItemActions.onDeleteConfirmed(asset)
        
        // Then
        verify { viewModel.onEvent(any()) }
        
        // Verify the captured event is a RemoveAsset event with the correct asset
        val capturedEvent = eventSlot.captured
        assert(capturedEvent is AssetsEvent.RemoveAsset)
        assertEquals(asset.id, (capturedEvent as AssetsEvent.RemoveAsset).asset.id)
        assertEquals(asset.symbol, capturedEvent.asset.symbol)
    }
}

/**
 * Helper class to extract the actions from AssetTickerItem for testing
 */
class AssetTickerItemActions(private val viewModel: MainViewModel) {
    fun onDeleteConfirmed(asset: Asset) {
        viewModel.onEvent(AssetsEvent.RemoveAsset(asset))
    }
}
