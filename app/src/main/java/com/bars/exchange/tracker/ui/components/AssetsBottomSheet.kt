package com.bars.exchange.tracker.ui.components

import android.util.Log
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.AssetsEvent
import com.bars.exchange.tracker.ui.main.mvi.AssetsState
import com.bars.exchange.tracker.ui.theme.TrackerApplicationTheme


private const val TAG = "AssetsBottomSheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsBottomSheet(
    state: AssetsState,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    // Create a local copy of the selected assets to track changes within the bottom sheet
    // This allows us to discard changes if the user closes without saving
    // Using SnapshotStateList which will properly trigger recomposition when modified
    val localSelectedAssets: SnapshotStateList<Asset> = remember { mutableStateListOf() }
    
    // Update local selection when the state's selected assets change (initial load)
    LaunchedEffect(state.selectedAssets) {
        localSelectedAssets.clear()
        localSelectedAssets.addAll(state.selectedAssets)
    }
    
    // Function to handle local asset selection toggle
    val toggleAssetSelection = { asset: Asset ->
        val existingIndex = localSelectedAssets.indexOfFirst { it.id == asset.id }
        
        if (existingIndex >= 0) {
            // Asset is already selected, remove it
            localSelectedAssets.removeAt(existingIndex)
        } else {
            // Asset is not selected, add it
            localSelectedAssets.add(asset.copy(isSelected = true))
        }
    }
    
    // Load assets when the sheet is opened, clear search, and sync selected assets
    LaunchedEffect(Unit) {
        // Clear any existing search query
        onEvent(AssetsEvent.SearchQueryChanged(""))
        // Sync selected assets to ensure proper selection state
        onEvent(AssetsEvent.SyncSelectedAssets)
        // Load assets
        onEvent(AssetsEvent.LoadAssets)
    }

    // Intercept back press to prevent unexpected dismissal
    androidx.activity.compose.BackHandler(enabled = true) {
        // Only handle back press explicitly through our dismiss handler
        onDismiss()
    }

    // Custom bottom sheet dialog implementation
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top bar with close and done buttons
            TopAppBar(
                title = {
                    Text(
                        text = "Select Assets",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Just dismiss without saving changes
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // First, update the ViewModel with our local selection
                            Log.d(TAG, "Done button clicked, saving ${localSelectedAssets.size} selected assets")
                            
                            // Update the ViewModel's state with our local selection
                            // First, clear existing selection
                            onEvent(AssetsEvent.ClearSelection)
                            
                            // For each selected asset in our local state, toggle it in the ViewModel
                            localSelectedAssets.forEach { asset ->
                                Log.d(TAG, "Selected asset to save: ${asset.symbol} (${asset.id}), isSelected=${asset.isSelected}")
                                onEvent(AssetsEvent.ToggleAssetSelection(asset))
                            }
                            
                            // Save the selected assets
                            onEvent(AssetsEvent.SaveSelectedAssets)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done"
                        )
                    }
                }
            )

            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { query ->
                    onEvent(AssetsEvent.SearchQueryChanged(query))
                },
                onSearch = { },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Search suggestions will go here later
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Asset list
            // Main content area with LazyColumn for virtualized rendering
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                state.filteredAssets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isEmpty()) "No assets available"
                            else "No matching assets found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    // Remember the LazyListState to maintain scroll position
                    val listState = remember {
                        androidx.compose.foundation.lazy.LazyListState()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        // Performance optimizations
                        contentPadding = PaddingValues(bottom = 16.dp),
                        // Use default fling behavior for smooth scrolling
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        // Use remembered state to prevent issues with scrolling
                        state = listState
                    ) {
                        items(
                            items = state.filteredAssets,
                            key = { it.id }, // Use stable keys for better performance
                        ) { asset ->
                            // Check if this asset is in our local selection
                            val isSelected = localSelectedAssets.any { it.id == asset.id }
                            
                            // Create a copy of the asset with the correct selection state
                            val assetWithLocalSelection = asset.copy(isSelected = isSelected)
                            
                            AssetItem(
                                asset = assetWithLocalSelection,
                                onClick = { toggleAssetSelection(asset) }
                            )
                        }

                        // Load more assets when scrolling to the bottom
                        if (state.hasMoreAssets) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        // Load more when this item becomes visible
                                        LaunchedEffect(Unit) {
                                            onEvent(AssetsEvent.LoadMoreAssets)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
@ThemePreviews
private fun AssetsBottomSheetPreview() {
    TrackerApplicationTheme {
        Surface {
            AssetsBottomSheet(
                state = AssetsState(
                    assets = listOf(
                        Asset(
                            id = "BTC_USDT",
                            symbol = "BTCUSDT",
                            name = "Bitcoin/USDT",
                            imageUrl = null
                        )
                    )
                ),
                onEvent = {},
                onDismiss = {}
            )
        }
    }
}
