package com.bars.exchange.tracker.ui.main.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bars.exchange.tracker.ui.components.AssetTickerItem
import com.bars.exchange.tracker.ui.main.mvi.AssetsEffect
import com.bars.exchange.tracker.ui.main.mvi.MainViewModel
import kotlinx.coroutines.flow.collectLatest

// Logger object for HomeScreen
private object HomeScreenLogger {
    const val TAG = "HomeScreen"
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    onOpenAssetsBottomSheet: () -> Unit = {}
) {
    // Track HomeScreen visibility using lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // HomeScreen became visible
                    viewModel.setHomeScreenVisibility(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    // HomeScreen is no longer visible
                    viewModel.setHomeScreenVisibility(false)
                }
                else -> { /* Ignore other events */ }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AssetsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> {
                    // Other effects handled at parent level
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Header with matching navigation bar color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Exchange Rates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onOpenAssetsBottomSheet() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Exchange Rate",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        // Content Area for the list
        val selectedAssets by viewModel.selectedAssetsForDisplay.collectAsState()
        Log.d(HomeScreenLogger.TAG, "Collected selectedAssetsForDisplay with ${selectedAssets.size} assets")

        // Log when selected assets change
        LaunchedEffect(selectedAssets) {
            Log.d(HomeScreenLogger.TAG, "Selected assets updated, count: ${selectedAssets.size}")
            selectedAssets.forEach { asset ->
                Log.d(HomeScreenLogger.TAG, "Displaying asset: ${asset.symbol} (${asset.id})")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = if (selectedAssets.isEmpty()) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = if (selectedAssets.isEmpty()) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (selectedAssets.isEmpty()) {
                Text(
                    text = "No exchange rates selected yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap + to add assets to track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                // Display selected assets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Selected Assets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        // Use default fling behavior for smooth scrolling
                        flingBehavior = ScrollableDefaults.flingBehavior()
                    ) {
                        items(
                            items = selectedAssets,
                            key = { it.id } // Use stable keys for better performance
                        ) { asset ->
                            // Wrap each item in AnimatedVisibility for appear/disappear animations
                            AnimatedVisibility(
                                visible = true, // Always visible once in the list
                                enter = fadeIn(
                                    animationSpec = tween(durationMillis = 400)
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(durationMillis = 400)
                                )
                            ) {
                                AssetTickerItem(
                                    asset = asset,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
