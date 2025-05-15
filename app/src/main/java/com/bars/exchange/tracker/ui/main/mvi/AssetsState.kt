package com.bars.exchange.tracker.ui.main.mvi

// Represents an individual asset
data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String? = null,
    val isSelected: Boolean = false
)

// Represents the state of the assets bottom sheet
data class AssetsState(
    val isLoading: Boolean = false,
    val assets: List<Asset> = emptyList(),
    val searchQuery: String = "",
    val filteredAssets: List<Asset> = emptyList(),
    val selectedAssets: List<Asset> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreAssets: Boolean = true,
    val currentPage: Int = 0,
    val pageSize: Int = 50 // Limit to 50 assets per page for better performance
)

// Events that can be triggered from the UI
sealed interface AssetsEvent {
    data class SearchQueryChanged(val query: String) : AssetsEvent
    data object LoadAssets : AssetsEvent
    data object LoadMoreAssets : AssetsEvent
    data class ToggleAssetSelection(val asset: Asset) : AssetsEvent
    data class RemoveAsset(val asset: Asset) : AssetsEvent
    data object SaveSelectedAssets : AssetsEvent
    data object ClearSelection : AssetsEvent
    data object SyncSelectedAssets : AssetsEvent
}

// Side effects that need to be handled
sealed interface AssetsEffect {
    data class ShowError(val message: String) : AssetsEffect
    data class AssetSelectionUpdated(val selectedAssets: List<Asset>) : AssetsEffect
    data object CloseBottomSheet : AssetsEffect
    data object SaveAndCloseBottomSheet : AssetsEffect
}
