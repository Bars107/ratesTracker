package com.bars.exchange.tracker.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.AssetsEvent
import com.bars.exchange.tracker.ui.main.mvi.AssetsState
import com.bars.exchange.tracker.ui.theme.TrackerApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsBottomSheet(
    state: AssetsState,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onEvent(AssetsEvent.LoadAssets)
    }

    // Create a sheet state that disables gesture-based dismissal
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { false } // Prevents dismissal by gesture
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                    IconButton(onClick = { onEvent(AssetsEvent.ClearSelection) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            println("AssetsBottomSheet: Done button clicked, saving ${state.selectedAssets.size} selected assets")
                            state.selectedAssets.forEach { asset ->
                                println("AssetsBottomSheet: Selected asset to save: ${asset.symbol} (${asset.id}), isSelected=${asset.isSelected}")
                            }
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
                        // Use state restoration policy for better performance
                        state = androidx.compose.foundation.lazy.LazyListState(
                            firstVisibleItemIndex = 0,
                            firstVisibleItemScrollOffset = 0
                        )
                    ) {
                        items(
                            items = state.filteredAssets,
                            key = { it.id }, // Use stable keys for better performance
                        ) { asset ->
                            AssetItem(
                                asset = asset,
                                onClick = { onEvent(AssetsEvent.ToggleAssetSelection(asset)) }
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

@OptIn(ExperimentalMaterial3Api::class)
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
