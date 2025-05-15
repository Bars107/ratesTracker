package com.bars.exchange.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bars.exchange.tracker.ui.main.mvi.Asset
import com.bars.exchange.tracker.ui.main.mvi.AssetsEvent
import com.bars.exchange.tracker.ui.main.mvi.MainViewModel

/**
 * A composable that displays an asset with real-time ticker updates.
 * This component subscribes to updates for its specific asset symbol.
 */
@Composable
fun AssetTickerItem(
    asset: Asset,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // State for confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Subscribe to updates for this asset
    LaunchedEffect(asset.symbol) {
        viewModel.subscribeToAsset(asset.symbol)
    }

    // Collect the tickerUpdates StateFlow and extract the update for this specific asset
    val tickerUpdatesMap by viewModel.tickerUpdates.collectAsState(initial = emptyMap())

    // Get the ticker update for this specific asset
    val tickerUpdate = tickerUpdatesMap[asset.symbol]

    // Show confirmation dialog if needed
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Remove Asset") },
            text = { Text("Are you sure you want to remove ${asset.name} (${asset.symbol}) from your tracked assets?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Remove the asset
                        viewModel.onEvent(AssetsEvent.RemoveAsset(asset))
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Delete button positioned in the top-right corner
            IconButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp) // Add margin from top and right
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Asset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Asset name and symbol
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = asset.symbol,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Price information
                if (tickerUpdate != null) {
                    // Current price
                    Text(
                        text = tickerUpdate.price,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Price change percentage
                    val changeColor = if (tickerUpdate.isPositiveChange) {
                        Color(0xFF4CAF50) // Green
                    } else {
                        Color(0xFFF44336) // Red
                    }

                    Text(
                        text = tickerUpdate.formattedPriceChange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )

                    // Volume
                    Text(
                        text = "Vol: ${tickerUpdate.volume}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
