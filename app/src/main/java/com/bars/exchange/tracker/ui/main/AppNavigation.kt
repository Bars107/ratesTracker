package com.bars.exchange.tracker.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bars.exchange.tracker.ui.main.screens.FavoriteScreen
import com.bars.exchange.tracker.ui.main.screens.HomeScreen
import com.bars.exchange.tracker.ui.main.screens.MarketsScreen
import com.bars.exchange.tracker.ui.main.screens.SettingsScreen

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

@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                items = bottomNavItems
            )
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
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
            HomeScreen()
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
        modifier = modifier
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
