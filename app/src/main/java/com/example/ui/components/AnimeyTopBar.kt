package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeyTopBar(
    title: String,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToDownloads: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {
        if (onNavigateToHistory != null) {
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.testTag("topbar_history_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Watch History",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (onNavigateToDownloads != null) {
            IconButton(
                onClick = onNavigateToDownloads,
                modifier = Modifier.testTag("topbar_downloads_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Downloads",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (onNavigateToSettings != null) {
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("topbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
