package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AnimeyTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val container = (application as AnimeyApplication).container
    setContent {
      val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())

      val isDark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
      }
      val isAmoled = settings.amoledMode || settings.themeMode == AppThemeMode.AMOLED

      AnimeyTheme(
        darkTheme = isDark,
        dynamicColor = settings.dynamicColor,
        amoled = isAmoled,
        accentColor = settings.accentColor
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation()
        }
      }
    }
  }
}

