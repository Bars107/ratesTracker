package com.bars.exchange.tracker.ui.main

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bars.exchange.tracker.ui.components.AssetsBottomSheet
import com.bars.exchange.tracker.ui.main.mvi.AssetsEffect
import com.bars.exchange.tracker.ui.main.mvi.MainViewModel
import com.bars.exchange.tracker.ui.main.screens.FavoriteScreen
import com.bars.exchange.tracker.ui.main.screens.HomeScreen
import com.bars.exchange.tracker.ui.main.screens.MarketsScreen
import com.bars.exchange.tracker.ui.main.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

// List of bottom navigation items
val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Favorite,
    BottomNavItem.Markets,
    BottomNavItem.Settings
)

private fun getIndex(route: String?): Int {
    return bottomNavItems.indexOfFirst { it.route == route }.takeIf { it != -1 } ?: Int.MAX_VALUE
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultSlideEnter(
    animationSpec: FiniteAnimationSpec<IntOffset>
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (getIndex(initialRoute) < getIndex(targetRoute)) {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec)
    } else {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultSlideExit(
    animationSpec: FiniteAnimationSpec<IntOffset>
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (getIndex(initialRoute) < getIndex(targetRoute)) {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec)
    } else {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnter(
    animationSpec: FiniteAnimationSpec<IntOffset>
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (getIndex(initialRoute) > getIndex(targetRoute)) {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec)
    } else {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExit(
    animationSpec: FiniteAnimationSpec<IntOffset>
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (getIndex(initialRoute) > getIndex(targetRoute)) {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec)
    } else {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec)
    }
}

private const val TAG = "AppNavigation"

@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()
    var showAssetsBottomSheet by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    
    // Handle effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            Log.d(TAG,"Received effect: $effect")
            when (effect) {
                is AssetsEffect.AssetSelectionUpdated -> {
                    Log.d(TAG,"Asset selection updated with ${effect.selectedAssets.size} assets")
                    // Handle asset selection updates
                    // No need to close the bottom sheet here
                }
                AssetsEffect.CloseBottomSheet -> {
                    Log.d(TAG,"Closing bottom sheet without saving")
                    showAssetsBottomSheet = false
                }
                AssetsEffect.SaveAndCloseBottomSheet -> {
                    Log.d(TAG,"Saving assets and closing bottom sheet")
                    // Save selected assets and close the bottom sheet
                    showAssetsBottomSheet = false
                }
                else -> {
                    Log.w(TAG,"Unhandled effect: $effect")
                    /* Other effects handled by screens */ 
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                items = bottomNavItems
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                onOpenAssetsBottomSheet = { showAssetsBottomSheet = true },
                viewModel = viewModel
            )
            
            // Show bottom sheet when requested
            if (showAssetsBottomSheet) {
                AssetsBottomSheet(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onDismiss = { showAssetsBottomSheet = false }
                )
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onOpenAssetsBottomSheet: () -> Unit = {},
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        val animationSpec = tween<IntOffset>(durationMillis = 300)

        composable(
            BottomNavItem.Home.route,
            enterTransition = { defaultSlideEnter(animationSpec) },
            exitTransition = { defaultSlideExit(animationSpec) },
            // Home is the first item, so pop transitions are fixed
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec) }
        ) {
            HomeScreen(
                onOpenAssetsBottomSheet = onOpenAssetsBottomSheet,
                viewModel = viewModel
            )
        }

        composable(
            BottomNavItem.Favorite.route,
            enterTransition = { defaultSlideEnter(animationSpec) },
            exitTransition = { defaultSlideExit(animationSpec) },
            popEnterTransition = { defaultPopEnter(animationSpec) },
            popExitTransition = { defaultPopExit(animationSpec) }
        ) {
            FavoriteScreen()
        }

        composable(
            BottomNavItem.Markets.route,
            enterTransition = { defaultSlideEnter(animationSpec) },
            exitTransition = { defaultSlideExit(animationSpec) },
            popEnterTransition = { defaultPopEnter(animationSpec) },
            popExitTransition = { defaultPopExit(animationSpec) }
        ) {
            MarketsScreen()
        }

        composable(
            BottomNavItem.Settings.route,
            // Settings is the last item, so transitions are fixed
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec) }
        ) {
            SettingsScreen()
        }
    }
}


@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                alwaysShowLabel = true // Or false, depending on your preference
            )
        }
    }
}
