package com.bars.exchange.tracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bars.exchange.tracker.domain.usecase.GetAvailableAssetsUseCase
import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.AssetsEffect
import com.bars.exchange.tracker.ui.main.mvi.AssetsEvent
import com.bars.exchange.tracker.ui.main.mvi.AssetsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAvailableAssetsUseCase: GetAvailableAssetsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AssetsState())
    val state: StateFlow<AssetsState> = _state.asStateFlow()

    private val _effect = Channel<AssetsEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        onEvent(AssetsEvent.LoadAssets)
    }

    fun onEvent(event: AssetsEvent) {
        when (event) {
            AssetsEvent.LoadAssets -> loadAssets()
            AssetsEvent.LoadMoreAssets -> loadMoreAssets()
            is AssetsEvent.SearchQueryChanged -> handleSearchQuery(event.query)
            is AssetsEvent.ToggleAssetSelection -> toggleAssetSelection(event.asset)
            AssetsEvent.SaveSelectedAssets -> saveSelectedAssets()
            AssetsEvent.ClearSelection -> clearSelection()
        }
    }

    private fun loadAssets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, currentPage = 0) }
            
            // Store current selected assets to preserve selection state
            val currentSelectedAssets = _state.value.selectedAssets
            
            try {
                val result = getAvailableAssetsUseCase()
                result.fold(
                    onSuccess = { assets ->
                        val uiAssets = assets.map { it.toUiAsset() }
                        val currentState = _state.value
                        val pageSize = currentState.pageSize
                        
                        // Only show the first page initially
                        val initialAssets = if (uiAssets.size > pageSize) {
                            uiAssets.take(pageSize)
                        } else {
                            uiAssets
                        }
                        
                        // Mark assets as selected if they were previously selected
                        val updatedAssets = initialAssets.map { asset ->
                            val wasSelected = currentSelectedAssets.any { it.id == asset.id }
                            asset.copy(isSelected = wasSelected)
                        }
                        
                        _state.update { it.copy(
                            isLoading = false,
                            assets = uiAssets,  // Store all assets for filtering
                            filteredAssets = updatedAssets, // Only show first page
                            hasMoreAssets = uiAssets.size > pageSize,
                            currentPage = 1,
                            selectedAssets = currentSelectedAssets
                        )}
                    },
                    onFailure = { exception ->
                        _state.update { it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load assets"
                        )}
                        _effect.send(AssetsEffect.ShowError(exception.message ?: "Failed to load assets"))
                    }
                )
            } catch (exception: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load assets"
                )}
                _effect.send(AssetsEffect.ShowError(exception.message ?: "Failed to load assets"))
            }
        }
    }
    
    private fun loadMoreAssets() {
        viewModelScope.launch {
            if (_state.value.isLoadingMore || !_state.value.hasMoreAssets) return@launch
            
            _state.update { it.copy(isLoadingMore = true) }
            
            val nextPage = _state.value.currentPage + 1
            val startIndex = nextPage * _state.value.pageSize
            val endIndex = startIndex + _state.value.pageSize
            
            val allAssets = _state.value.assets
            if (startIndex >= allAssets.size) {
                _state.update { it.copy(isLoadingMore = false, hasMoreAssets = false) }
                return@launch
            }
            
            // Get the next page of assets
            val nextPageAssets = allAssets.subList(
                startIndex,
                endIndex.coerceAtMost(allAssets.size)
            )
            
            // Mark assets as selected if they are in the selectedAssets list
            val currentSelectedAssets = _state.value.selectedAssets
            val updatedNextPageAssets = nextPageAssets.map { asset ->
                val isSelected = currentSelectedAssets.any { it.id == asset.id }
                asset.copy(isSelected = isSelected)
            }
            
            val hasMore = endIndex < allAssets.size
            
            _state.update { currentState ->
                currentState.copy(
                    filteredAssets = currentState.filteredAssets + updatedNextPageAssets,
                    isLoadingMore = false,
                    hasMoreAssets = hasMore,
                    currentPage = nextPage
                )
            }
        }
    }

    private fun handleSearchQuery(query: String) {
        viewModelScope.launch {
            _state.update { currentState ->
                // Get current selected assets to preserve selection state
                val currentSelectedAssets = currentState.selectedAssets
                
                // Filter all assets based on query
                val filteredAssets = if (query.isEmpty()) {
                    // If query is empty, show all assets up to current page
                    val endIndex = currentState.currentPage * currentState.pageSize
                    currentState.assets.take(endIndex)
                } else {
                    // If query is not empty, filter all assets
                    currentState.assets.filter { asset ->
                        asset.name.contains(query, ignoreCase = true) ||
                        asset.symbol.contains(query, ignoreCase = true)
                    }
                }
                
                // Mark assets as selected if they are in the selectedAssets list
                val updatedFilteredAssets = filteredAssets.map { asset ->
                    val isSelected = currentSelectedAssets.any { it.id == asset.id }
                    asset.copy(isSelected = isSelected)
                }
                
                currentState.copy(
                    searchQuery = query,
                    filteredAssets = updatedFilteredAssets,
                    hasMoreAssets = filteredAssets.size > currentState.pageSize,
                    currentPage = 1 // Reset to first page when searching
                )
            }
        }
    }

    private fun toggleAssetSelection(asset: Asset) {
        viewModelScope.launch {
            _state.update { currentState ->
                // Check if the asset is already selected
                val isCurrentlySelected = currentState.selectedAssets.any { it.id == asset.id }
                
                // Create a new list of selected assets
                val newSelectedAssets = if (isCurrentlySelected) {
                    // Remove the asset if it's already selected
                    currentState.selectedAssets.filter { it.id != asset.id }
                } else {
                    // Add the asset if it's not selected
                    currentState.selectedAssets + asset
                }
                
                // Update the filtered assets to reflect the selection state
                val updatedFilteredAssets = currentState.filteredAssets.map { existingAsset ->
                    if (existingAsset.id == asset.id) {
                        existingAsset.copy(isSelected = !isCurrentlySelected)
                    } else {
                        existingAsset
                    }
                }
                
                // Update the full assets list
                val updatedAssets = currentState.assets.map { existingAsset ->
                    if (existingAsset.id == asset.id) {
                        existingAsset.copy(isSelected = !isCurrentlySelected)
                    } else {
                        existingAsset
                    }
                }
                
                // Return the updated state
                currentState.copy(
                    filteredAssets = updatedFilteredAssets,
                    assets = updatedAssets,
                    selectedAssets = newSelectedAssets
                )
            }
            
            // Selection changes are only reflected on HomeScreen when user clicks Done
            
            // Notify about the selection update
            _effect.send(AssetsEffect.AssetSelectionUpdated(_state.value.selectedAssets))
        }
    }
    
    // State for the HomeScreen to display selected assets
    private val _selectedAssetsForDisplay = MutableStateFlow<List<Asset>>(emptyList())
    val selectedAssetsForDisplay: StateFlow<List<Asset>> = _selectedAssetsForDisplay.asStateFlow()
    
    private fun saveSelectedAssets() {
        viewModelScope.launch {
            val selectedAssets = _state.value.selectedAssets
            println("MainViewModel: Saving selected assets: ${selectedAssets.size} items")
            selectedAssets.forEach { asset ->
                println("MainViewModel: Selected asset: ${asset.symbol} (${asset.id}), isSelected=${asset.isSelected}")
            }
            
            // Update the selected assets for display
            _selectedAssetsForDisplay.value = selectedAssets
            println("MainViewModel: Updated _selectedAssetsForDisplay with ${_selectedAssetsForDisplay.value.size} items")
            
            // TODO: Save selected assets to local database
            // For now, just send the effect to close the bottom sheet
            _effect.send(AssetsEffect.SaveAndCloseBottomSheet)
            println("MainViewModel: Sent SaveAndCloseBottomSheet effect")
        }
    }
    
    private fun clearSelection() {
        viewModelScope.launch {
            _state.update { currentState ->
                // Clear selection from all assets
                val updatedAssets = currentState.assets.map { it.copy(isSelected = false) }
                val updatedFilteredAssets = currentState.filteredAssets.map { it.copy(isSelected = false) }
                
                currentState.copy(
                    assets = updatedAssets,
                    filteredAssets = updatedFilteredAssets,
                    selectedAssets = emptyList()
                )
            }
            
            // Send effect to close the bottom sheet
            _effect.send(AssetsEffect.CloseBottomSheet)
        }
    }

    private fun com.bars.exchange.tracker.domain.model.Asset.toUiAsset(): Asset {
        return Asset(
            id = "${baseAsset}_${quoteAsset}",
            symbol = symbol,
            name = "$baseAsset/$quoteAsset",
            imageUrl = null // We can add image URLs later if needed
        )
    }
}
