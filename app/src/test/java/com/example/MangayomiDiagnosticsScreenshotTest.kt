package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.extension.mangayomi.diagnostics.MangayomiRuntimeDiagnostics
import com.example.features.diagnostics.MangayomiDiagnosticsScreen
import com.example.features.diagnostics.MangayomiDiagnosticsViewModel
import com.example.ui.theme.AnimeyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class MangayomiDiagnosticsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mangayomi_diagnostics_screenshot() {
        val vm = MangayomiDiagnosticsViewModel(autoRun = false)

        composeTestRule.setContent {
            AnimeyTheme(darkTheme = true) {
                MangayomiDiagnosticsScreen(
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mangayomi_diagnostics.png")
    }
}
