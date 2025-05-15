package com.bars.exchange.tracker.ui.main.mvi

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import com.bars.exchange.tracker.domain.usecase.GetAvailableAssetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAvailableAssetsUseCase: GetAvailableAssetsUseCase,
    private val assetRepository: IAssetRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _state = MutableStateFlow(AssetsState())
    val state: StateFlow<AssetsState> = _state.asStateFlow()

    private val _effect = Channel<AssetsEffect>()
    val effect = _effect.receiveAsFlow()
    
    // Track HomeScreen visibility
    private var isHomeScreenVisible = false
    
    // Store active subscription jobs
    private val subscriptionJobs = mutableMapOf<String, Job>()

    // Store ticker updates for UI display
    private val _tickerUpdates = MutableStateFlow<Map<String, TickerUpdate>>(emptyMap())
    val tickerUpdates = _tickerUpdates.asStateFlow()

    // Store selected assets for display
    private val _selectedAssetsForDisplay = MutableStateFlow<List<Asset>>(emptyList())
    val selectedAssetsForDisplay = _selectedAssetsForDisplay.asStateFlow()

    init {
        // Load previously selected assets first
        loadSelectedAssets()
        // Then load all available assets
        onEvent(AssetsEvent.LoadAssets)
    }
    
    private fun loadSelectedAssets() {
        viewModelScope.launch {
            try {
                val savedAssets = assetRepository.getSelectedAssets()
                if (savedAssets.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${savedAssets.size} previously selected assets")
                    // Update the state with loaded selected assets
                    _state.update { it.copy(selectedAssets = savedAssets) }
                    // Also update the display state
                    _selectedAssetsForDisplay.emit(savedAssets)
                    
                    // Load cached ticker updates for selected assets
                    loadCachedTickerUpdates(savedAssets)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected assets: ${e.message}", e)
            }
        }
    }
    
    private suspend fun loadCachedTickerUpdates(assets: List<Asset>) {
        val updates = mutableMapOf<String, TickerUpdate>()
        
        assets.forEach { asset ->
            try {
                val symbol = asset.symbol
                val cachedUpdate = assetRepository.getLatestTickerUpdate(symbol)
                if (cachedUpdate != null) {
                    updates[symbol] = cachedUpdate
                    Log.d(TAG, "Loaded cached ticker update for $symbol")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached ticker update: ${e.message}", e)
            }
        }

        if (updates.isNotEmpty()) {
            _tickerUpdates.update { currentUpdates ->
                currentUpdates.toMutableMap().apply { putAll(updates) }
            }
        }
    }

    fun onEvent(event: AssetsEvent) {
        when (event) {
            AssetsEvent.LoadAssets -> loadAssets()
            AssetsEvent.LoadMoreAssets -> loadMoreAssets()
            is AssetsEvent.SearchQueryChanged -> handleSearchQuery(event.query)
            is AssetsEvent.ToggleAssetSelection -> toggleAssetSelection(event.asset)
            is AssetsEvent.RemoveAsset -> removeAsset(event.asset)
            AssetsEvent.SaveSelectedAssets -> saveSelectedAssets()
            AssetsEvent.ClearSelection -> clearSelection()
            // Add a new event to sync the selected assets state
            AssetsEvent.SyncSelectedAssets -> syncSelectedAssets()
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
            
            // Notify about the selection update
            _effect.send(AssetsEffect.AssetSelectionUpdated(_state.value.selectedAssets))
        }
    }

    private fun saveSelectedAssets() {
        viewModelScope.launch {
            // Get selected assets and ensure isSelected is true for all of them
            val selectedAssets = _state.value.selectedAssets.map { it.copy(isSelected = true) }
            
            Log.d(TAG,"Saving selected assets: ${selectedAssets.size} items")
            selectedAssets.forEach { asset ->
                Log.d(TAG, "Selected asset: ${asset.symbol} (${asset.id}), isSelected=${asset.isSelected}")
            }
            
            // Update the selected assets for display with the corrected isSelected flag
            _selectedAssetsForDisplay.emit(selectedAssets)
            Log.d(TAG,"Updated _selectedAssetsForDisplay with ${selectedAssets.size} items, all with isSelected=true")
            
            // Also update the state to reflect the corrected isSelected values
            _state.update { currentState ->
                currentState.copy(
                    selectedAssets = selectedAssets
                )
            }
            
            // Save selected assets to local database
            try {
                assetRepository.saveSelectedAssets(selectedAssets)
                Log.d(TAG,"Successfully saved selected assets to persistent storage")
            } catch (e: Exception) {
                Log.e(TAG,"Error saving selected assets: ${e.message}")
            }
            
            // Send the effect to close the bottom sheet
            _effect.send(AssetsEffect.SaveAndCloseBottomSheet)
            Log.d(TAG,"Sent SaveAndCloseBottomSheet effect")
        }
    }
    
    private fun clearSelection() {
        viewModelScope.launch {
            _state.update { currentState ->
                // Clear selected assets
                val updatedFilteredAssets = currentState.filteredAssets.map { it.copy(isSelected = false) }
                val updatedAssets = currentState.assets.map { it.copy(isSelected = false) }
                
                currentState.copy(
                    selectedAssets = emptyList(),
                    filteredAssets = updatedFilteredAssets,
                    assets = updatedAssets
                )
            }
            
            // Unsubscribe from all ticker updates
            unsubscribeFromAllTickerUpdates()
            
            // Update the display state
            _selectedAssetsForDisplay.emit(emptyList())
        }
    }
    
    /**
     * Removes an asset from the selected assets and unsubscribes from its ticker updates.
     * @param asset The asset to remove.
     */
    private fun removeAsset(asset: Asset) {
        viewModelScope.launch {
            Log.d(TAG, "Removing asset: ${asset.symbol} (${asset.id})")
            
            // Unsubscribe from ticker updates for this asset
            unsubscribeFromTickerUpdates(asset.symbol)
            
            // Update the state to remove the asset from selected assets
            _state.update { currentState ->
                // Remove the asset from selected assets
                val updatedSelectedAssets = currentState.selectedAssets.filter { it.id != asset.id }
                
                // Update all assets to reflect selection state
                val updatedAssets = currentState.assets.map { a ->
                    if (a.id == asset.id) a.copy(isSelected = false) else a
                }
                
                // Update filtered assets as well
                val updatedFilteredAssets = currentState.filteredAssets.map { a ->
                    if (a.id == asset.id) a.copy(isSelected = false) else a
                }
                
                currentState.copy(
                    selectedAssets = updatedSelectedAssets,
                    assets = updatedAssets,
                    filteredAssets = updatedFilteredAssets
                )
            }
            
            // Update the display state without calling saveSelectedAssets()
            // This ensures we only remove this specific asset from the UI without affecting others
            val updatedDisplayAssets = _selectedAssetsForDisplay.value.filter { it.id != asset.id }
            _selectedAssetsForDisplay.emit(updatedDisplayAssets)
            
            // Save only this specific asset change to persistent storage without affecting other assets
            viewModelScope.launch {
                try {
                    // Save the current state of selected assets to the repository
                    val currentSelectedAssets = _state.value.selectedAssets
                    Log.d(TAG, "Saving ${currentSelectedAssets.size} selected assets after removal")
                    assetRepository.saveSelectedAssets(currentSelectedAssets)
                    Log.d(TAG, "Successfully saved selected assets after removal")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving selected assets after removal: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Synchronizes the selected assets state between the HomeScreen and AssetsBottomSheet.
     * This ensures that assets selected on the HomeScreen are also shown as selected in the AssetsBottomSheet.
     */
    private fun syncSelectedAssets() {
        viewModelScope.launch {
            // Get the current selected assets from the display state
            val selectedAssets = _selectedAssetsForDisplay.value
            
            if (selectedAssets.isEmpty()) return@launch

            Log.d(TAG,"Syncing selected assets: ${selectedAssets.size} items")
            
            // Update the filtered assets and all assets to reflect the selection state
            _state.update { currentState ->
                // Create maps for quick lookup of selected assets by ID
                val selectedAssetIds = selectedAssets.map { it.id }.toSet()
                
                // Update filtered assets to reflect selection state
                val updatedFilteredAssets = currentState.filteredAssets.map { asset ->
                    asset.copy(isSelected = selectedAssetIds.contains(asset.id))
                }
                
                // Update all assets to reflect selection state
                val updatedAssets = currentState.assets.map { asset ->
                    asset.copy(isSelected = selectedAssetIds.contains(asset.id))
                }
                
                // Update the state
                currentState.copy(
                    selectedAssets = selectedAssets,
                    filteredAssets = updatedFilteredAssets,
                    assets = updatedAssets
                )
            }
            
            Log.d(TAG, "Selected assets synced successfully")
        }
    }
    
    /**
     * Sets the HomeScreen visibility and manages subscriptions accordingly.
     * @param isVisible Whether the HomeScreen is currently visible.
     */
    fun setHomeScreenVisibility(isVisible: Boolean) {
        if (isHomeScreenVisible == isVisible) return // No change
        
        isHomeScreenVisible = isVisible
        Log.d(TAG, "HomeScreen visibility changed to $isVisible")
        
        if (isVisible) {
            // HomeScreen became visible, subscribe to updates for all selected assets
            subscribeToSelectedAssets()
        } else {
            // HomeScreen became invisible, unsubscribe from all updates
            unsubscribeFromAllTickerUpdates()
        }
    }
    
    /**
     * Subscribes to ticker updates for all selected assets.
     */
    private fun subscribeToSelectedAssets() {
        val selectedAssets = _selectedAssetsForDisplay.value
        if (selectedAssets.isEmpty()) return
        
        Log.d(TAG, "Subscribing to updates for ${selectedAssets.size} selected assets")
        
        selectedAssets.forEach { asset ->
            subscribeToAsset(asset.symbol)
        }
    }
    
    /**
     * Subscribes to ticker updates for a specific asset symbol.
     * @param symbol The symbol to subscribe to.
     */
    fun subscribeToAsset(symbol: String) {
        // Don't subscribe if already subscribed or if HomeScreen is not visible
        if (subscriptionJobs.containsKey(symbol) || !isHomeScreenVisible) return
        
        Log.d(TAG, "Subscribing to ticker updates for $symbol")
        
        val job = viewModelScope.launch {
            try {
                assetRepository.subscribeToTickerUpdates(symbol)
                    .collect { update ->
                        // Update the ticker updates map
                        _tickerUpdates.update { currentUpdates ->
                            currentUpdates.toMutableMap().apply { put(symbol, update) }
                        }
                        Log.d(TAG, "Received ticker update for $symbol: ${update.price}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in subscription for $symbol: ${e.message}", e)
            }
        }
        
        subscriptionJobs[symbol] = job
    }

    /**
     * Unsubscribes from ticker updates for a specific asset symbol.
     * @param symbol The symbol to unsubscribe from.
     */
    private fun unsubscribeFromTickerUpdates(symbol: String) {
        // Cancel the subscription job if it exists
        val job = subscriptionJobs.remove(symbol)
        if (job != null) {
            job.cancel()
            Log.d(TAG, "Unsubscribed from ticker updates for $symbol")
            
            // Tell the repository to stop updates for this symbol
            viewModelScope.launch {
                assetRepository.unsubscribeFromTickerUpdates(symbol)
            }
        }
    }

    /**
     * Unsubscribes from all active ticker updates.
     */
    private fun unsubscribeFromAllTickerUpdates() {
        if (subscriptionJobs.isEmpty()) return
        
        Log.d(TAG, "Unsubscribing from all ticker updates")
        
        // Cancel all subscription jobs
        subscriptionJobs.forEach { (symbol, job) ->
            job.cancel()
            Log.d(TAG, "Unsubscribed from ticker updates for $symbol")
        }
        
        // Clear the jobs map
        subscriptionJobs.clear()
        
        // Tell the repository to stop all updates
        viewModelScope.launch {
            assetRepository.unsubscribeFromAllTickerUpdates()
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
