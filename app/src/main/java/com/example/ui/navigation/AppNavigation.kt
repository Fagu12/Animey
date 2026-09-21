package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.AnimeyApplication
import com.example.ui.components.OfflineIndicatorBanner
import com.example.features.account.AccountScreen
import com.example.features.account.AccountViewModel
import com.example.features.account.AniListLoginScreen
import com.example.features.details.AnimeDetailsScreen
import com.example.features.discover.DiscoverScreen
import com.example.features.downloads.DownloadsScreen
import com.example.features.downloads.DownloadsViewModel
import com.example.features.extensions.ExtensionPreferencesScreen
import com.example.features.extensions.ExtensionsScreen
import com.example.features.history.HistoryScreen
import com.example.features.home.HomeScreen
import com.example.features.library.LibraryScreen
import com.example.features.player.PlayerScreen
import com.example.features.search.SearchScreen
import com.example.features.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container
    val isOnline by container.networkMonitor.isOnline.collectAsState(initial = container.networkMonitor.isCurrentlyOnline)

    // Check if current route is a primary tab
    val isPrimaryTab = Screen.bottomNavItems.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val showBottomBar = !isTablet && isPrimaryTab
        val showNavRail = isTablet && isPrimaryTab

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("animey_main_scaffold"),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("animey_bottom_navigation_bar")
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = stringResource(id = screen.titleRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = screen.titleRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(screen.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("animey_tablet_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = stringResource(id = screen.titleRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = screen.titleRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tablet_${screen.testTag}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    OfflineIndicatorBanner(isOffline = !isOnline)

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Diagnostics.route,
                        enterTransition = { fadeIn(animationSpec = tween(200)) },
                        exitTransition = { fadeOut(animationSpec = tween(160)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                        popExitTransition = { fadeOut(animationSpec = tween(160)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAnime = { animeId ->
                        navController.navigate(Screen.AnimeDetails.createRoute(animeId))
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToExtensions = { navController.navigate(Screen.Extensions.route) },
                    onNavigateToPlayer = { animeId, epId ->
                        navController.navigate(Screen.Player.createRoute(animeId, epId))
                    }
                )
            }
            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onNavigateToAnime = { animeId ->
                        navController.navigate(Screen.AnimeDetails.createRoute(animeId))
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToAnime = { animeId ->
                        navController.navigate(Screen.AnimeDetails.createRoute(animeId))
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToAnime = { animeId ->
                        navController.navigate(Screen.AnimeDetails.createRoute(animeId))
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { animeId, epId ->
                        navController.navigate(Screen.Player.createRoute(animeId, epId))
                    }
                )
            }
            composable(Screen.Downloads.route) {
                val context = LocalContext.current
                val container = (context.applicationContext as AnimeyApplication).container
                val downloadsViewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModel.provideFactory(container.downloadManager)
                )
                DownloadsScreen(
                    viewModel = downloadsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { animeId, epId ->
                        navController.navigate(Screen.Player.createRoute(animeId, epId))
                    }
                )
            }
            composable(Screen.Extensions.route) {
                ExtensionsScreen(
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPreferences = { extensionId ->
                        navController.navigate(Screen.ExtensionPreferences.createRoute(extensionId))
                    }
                )
            }

            composable(Screen.ExtensionPreferences.route) { backStackEntry ->
                val extensionIdRaw = backStackEntry.arguments?.getString("extensionId") ?: ""
                val decodedExtensionId = try {
                    java.net.URLDecoder.decode(extensionIdRaw, "UTF-8")
                } catch (e: Exception) {
                    extensionIdRaw
                }
                ExtensionPreferencesScreen(
                    extensionId = decodedExtensionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                )
            }

            composable(Screen.Diagnostics.route) {
                val context = LocalContext.current
                val container = (context.applicationContext as AnimeyApplication).container
                val diagViewModel: com.example.features.diagnostics.MangayomiDiagnosticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.features.diagnostics.MangayomiDiagnosticsViewModel(
                                animeDao = container.database.animeDao(),
                                episodeDao = container.database.episodeDao(),
                                okHttpClient = okhttp3.OkHttpClient()
                            ) as T
                        }
                    }
                )
                com.example.features.diagnostics.MangayomiDiagnosticsScreen(
                    viewModel = diagViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Account.route) {
                val context = LocalContext.current
                val container = (context.applicationContext as AnimeyApplication).container
                val accountViewModel: AccountViewModel = viewModel(
                    factory = AccountViewModel.provideFactory(
                        aniListRepository = container.aniListRepository,
                        trackingRepository = container.trackingRepository,
                        loginUseCase = container.aniListLoginUseCase,
                        logoutUseCase = container.aniListLogoutUseCase
                    )
                )
                AccountScreen(
                    viewModel = accountViewModel,
                    onNavigateToLogin = { navController.navigate(Screen.AniListLogin.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AniListLogin.route) {
                val context = LocalContext.current
                val container = (context.applicationContext as AnimeyApplication).container
                val accountViewModel: AccountViewModel = viewModel(
                    factory = AccountViewModel.provideFactory(
                        aniListRepository = container.aniListRepository,
                        trackingRepository = container.trackingRepository,
                        loginUseCase = container.aniListLoginUseCase,
                        logoutUseCase = container.aniListLogoutUseCase
                    )
                )
                AniListLoginScreen(
                    viewModel = accountViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = { navController.popBackStack() }
                )
            }
            composable(Screen.AnimeDetails.route) { backStackEntry ->
                val animeIdRaw = backStackEntry.arguments?.getString("animeId") ?: ""
                val decodedAnimeId = java.net.URLDecoder.decode(animeIdRaw, "UTF-8")
                AnimeDetailsScreen(
                    animeId = decodedAnimeId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAnime = { animeId ->
                        navController.navigate(Screen.AnimeDetails.createRoute(animeId))
                    },
                    onNavigateToPlayer = { animeId, epId ->
                        navController.navigate(Screen.Player.createRoute(animeId, epId))
                    }
                )
            }
            composable(Screen.Player.route) { backStackEntry ->
                val animeIdRaw = backStackEntry.arguments?.getString("animeId") ?: ""
                val episodeIdRaw = backStackEntry.arguments?.getString("episodeId") ?: ""
                val decodedAnimeId = java.net.URLDecoder.decode(animeIdRaw, "UTF-8")
                val decodedEpisodeId = java.net.URLDecoder.decode(episodeIdRaw, "UTF-8")
                PlayerScreen(
                    animeId = decodedAnimeId,
                    episodeId = decodedEpisodeId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
}
}
