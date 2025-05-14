package com.bars.exchange.tracker.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart // Placeholder for Markets
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String, // In a real app, use @StringRes val titleResId: Int
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Filled.Home
    )

    data object Favorite : BottomNavItem(
        route = "favorite",
        title = "Favorite",
        icon = Icons.Filled.Favorite
    )

    data object Markets : BottomNavItem(
        route = "markets",
        title = "Markets",
        icon = Icons.Filled.ShoppingCart // Placeholder
    )

    data object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Filled.Settings
    )
}
