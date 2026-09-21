package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Home : Screen(
        route = "home",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home"
    )

    data object Discover : Screen(
        route = "discover",
        titleRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_item_discover"
    )

    data object Search : Screen(
        route = "search",
        titleRes = R.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_item_search"
    )

    data object Library : Screen(
        route = "library",
        titleRes = R.string.nav_library,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_item_library"
    )

    data object History : Screen(
        route = "history",
        titleRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_item_history"
    )

    data object Downloads : Screen(
        route = "downloads",
        titleRes = R.string.nav_downloads,
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download,
        testTag = "nav_item_downloads"
    )

    data object Extensions : Screen(
        route = "extensions",
        titleRes = R.string.nav_extensions,
        selectedIcon = Icons.Filled.Extension,
        unselectedIcon = Icons.Outlined.Extension,
        testTag = "nav_item_extensions"
    )

    data object Settings : Screen(
        route = "settings",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_item_settings"
    )

    data object Account : Screen(
        route = "account",
        titleRes = R.string.nav_account,
        selectedIcon = Icons.Filled.Sync,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_item_account"
    )

    data object AniListLogin : Screen(
        route = "anilist_login",
        titleRes = R.string.nav_anilist_login,
        selectedIcon = Icons.Filled.Sync,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_item_anilist_login"
    )

    data object AnimeDetails : Screen(
        route = "details/{animeId}",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "screen_anime_details"
    ) {
        fun createRoute(animeId: String): String = "details/${java.net.URLEncoder.encode(animeId, "UTF-8")}"
    }

    data object Player : Screen(
        route = "player/{animeId}/{episodeId}",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "screen_player"
    ) {
        fun createRoute(animeId: String, episodeId: String): String =
            "player/${java.net.URLEncoder.encode(animeId, "UTF-8")}/${java.net.URLEncoder.encode(episodeId, "UTF-8")}"
    }

    data object ExtensionPreferences : Screen(
        route = "extension_preferences/{extensionId}",
        titleRes = R.string.nav_extensions,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "screen_extension_preferences"
    ) {
        fun createRoute(extensionId: String): String =
            "extension_preferences/${java.net.URLEncoder.encode(extensionId, "UTF-8")}"
    }

    data object Diagnostics : Screen(
        route = "diagnostics",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "screen_diagnostics"
    )

    companion object {
        // Bottom nav primary tabs
        val bottomNavItems = listOf(
            Home,
            Discover,
            Search,
            Library,
            Extensions
        )

        // All main top-level destinations
        val allScreens = listOf(
            Home,
            Discover,
            Search,
            Library,
            History,
            Downloads,
            Extensions,
            Settings
        )
    }
}
