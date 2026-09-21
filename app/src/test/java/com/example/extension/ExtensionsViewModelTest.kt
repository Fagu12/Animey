package com.example.extension

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.data.extension.repository.RepositoryFetcher
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ExtensionInfo
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.ExtensionType
import com.example.domain.model.RepositoryIndex
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import com.example.features.extensions.ExtensionSortOption
import com.example.features.extensions.ExtensionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeStorageRepo: FakeVmExtensionStorageRepository
    private lateinit var fakeSettingsRepo: FakeVmSettingsRepository
    private lateinit var fakeFetcher: FakeVmRepositoryFetcher
    private lateinit var extensionManager: DefaultExtensionManager
    private lateinit var testExtension: TestAnimeExtension
    private lateinit var viewModel: ExtensionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeStorageRepo = FakeVmExtensionStorageRepository()
        fakeSettingsRepo = FakeVmSettingsRepository()
        fakeFetcher = FakeVmRepositoryFetcher()
        testExtension = TestAnimeExtension()

        extensionManager = DefaultExtensionManager(
            storageRepository = fakeStorageRepo,
            settingsRepository = fakeSettingsRepo,
            repositoryFetcher = fakeFetcher,
            scope = testScope
        )
        extensionManager.registerExtension(testExtension)

        viewModel = ExtensionsViewModel(
            extensionManager = extensionManager,
            settingsRepository = fakeSettingsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTabNavigationAndDefaultState() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(0, state.selectedTab)
        assertEquals("ALL", state.selectedLanguage)
        assertNull(state.selectedType)
        assertEquals(ExtensionSortOption.ORDER, state.sortOption)

        viewModel.setTab(1)
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedTab)

        viewModel.setTab(2)
        testScheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.selectedTab)

        viewModel.setTab(3)
        testScheduler.advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testSearchAndFiltering() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        // Set search query
        viewModel.setSearchQuery("Anime Provider")
        testScheduler.advanceUntilIdle()
        assertEquals("Anime Provider", viewModel.uiState.value.searchQuery)

        // Set language filter
        viewModel.setLanguageFilter("EN")
        testScheduler.advanceUntilIdle()
        assertEquals("EN", viewModel.uiState.value.selectedLanguage)

        // Set type filter
        viewModel.setTypeFilter(ExtensionType.ANIME)
        testScheduler.advanceUntilIdle()
        assertEquals(ExtensionType.ANIME, viewModel.uiState.value.selectedType)

        // Set sort option
        viewModel.setSortOption(ExtensionSortOption.NAME)
        testScheduler.advanceUntilIdle()
        assertEquals(ExtensionSortOption.NAME, viewModel.uiState.value.sortOption)
    }

    @Test
    fun testExtensionToggleAndPreferredSelection() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        // Toggle enabled state
        viewModel.toggleExtensionEnabled("builtin.test.anime", false)
        testScheduler.advanceUntilIdle()

        // Set preferred extension
        viewModel.setPreferredExtension("builtin.test.anime")
        testScheduler.advanceUntilIdle()
        assertEquals("builtin.test.anime", fakeSettingsRepo.settingsFlow.value.defaultExtensionId)
    }

    @Test
    fun testRepositoryManagementLifecycle() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        val repoUrl = "https://custom.anime.org/index.json"
        fakeFetcher.mockRepository(repoUrl, RepositoryIndex("Custom Anime Repo", 1, emptyList()))

        // Add repository
        viewModel.addRepository("Custom Anime Repo", repoUrl)
        testScheduler.advanceUntilIdle()

        val msg = viewModel.uiState.value.userMessage
        val repos = fakeStorageRepo.getAllRepositories().first()
        assertTrue("Expected repo added but user message was: $msg, repos: $repos", repos.any { it.name == "Custom Anime Repo" })

        val repo = repos.first { it.name == "Custom Anime Repo" }

        // Toggle repository
        viewModel.toggleRepository(repo.id, false)
        testScheduler.advanceUntilIdle()

        // Delete repository
        viewModel.requestDeleteRepo(repo)
        testScheduler.advanceUntilIdle()
        assertEquals(repo, viewModel.uiState.value.deleteRepoTarget)

        viewModel.confirmDeleteRepo()
        testScheduler.advanceUntilIdle()

        val updatedRepos = fakeStorageRepo.getAllRepositories().first()
        assertFalse(updatedRepos.any { it.id == repo.id })
    }

    @Test
    fun testExtensionPreferencesManagement() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        val manifest = ExtensionManifest(
            id = "builtin.test.anime",
            name = "Test Anime",
            version = "1.0.0",
            language = "en",
            isInstalled = true
        )
        val info = ExtensionInfo(manifest = manifest, isLoaded = true)

        viewModel.openPreferences(info)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activePreferencesSheet)

        // Change a preference
        viewModel.setPreferenceValue("builtin.test.anime", "pref_video_quality", "1080p")
        testScheduler.advanceUntilIdle()

        viewModel.dismissPreferencesSheet()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.activePreferencesSheet)
    }

    @Test
    fun testExtensionDiagnosticsLogs() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        val manifest = ExtensionManifest(
            id = "builtin.test.anime",
            name = "Test Anime",
            version = "1.0.0",
            language = "en",
            isInstalled = true
        )
        val info = ExtensionInfo(manifest = manifest, isLoaded = true)

        viewModel.openLogs(info)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeLogsSheet)

        viewModel.dismissLogsSheet()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeLogsSheet)
    }

    private class FakeVmRepositoryFetcher : RepositoryFetcher {
        private val repositories = mutableMapOf<String, RepositoryIndex>()

        fun mockRepository(url: String, index: RepositoryIndex) {
            repositories[url] = index
        }

        override suspend fun fetchRepositoryIndex(url: String): AppResult<RepositoryIndex> {
            val index = repositories[url]
                ?: return AppResult.Error(AppError.NotFoundError("Repository not found: $url"))
            return AppResult.Success(index)
        }

        override fun parseIndexJson(json: String, baseRepoUrl: String): AppResult<RepositoryIndex> {
            return AppResult.Success(RepositoryIndex("Parsed Repo", 1, emptyList()))
        }
    }

    private class FakeVmExtensionStorageRepository : ExtensionStorageRepository {
        private val manifestsFlow = MutableStateFlow<List<ExtensionManifest>>(emptyList())
        private val reposFlow = MutableStateFlow<List<ExtensionRepository>>(emptyList())

        override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> = manifestsFlow
        override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> = manifestsFlow.map { it.filter { ext -> ext.isEnabled } }
        override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> = manifestsFlow.map { it.filter { ext -> ext.hasUpdate } }
        override fun getExtensionById(id: String): Flow<ExtensionManifest?> = manifestsFlow.map { it.firstOrNull { ext -> ext.id == id } }

        override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> {
            val list = manifestsFlow.value.toMutableList()
            val index = list.indexOfFirst { it.id == manifest.id }
            if (index != -1) list[index] = manifest else list.add(manifest)
            manifestsFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
            val list = manifestsFlow.value.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
            manifestsFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> {
            val list = manifestsFlow.value.map { if (it.id == id) it.copy(hasUpdate = hasUpdate) else it }
            manifestsFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> {
            val list = manifestsFlow.value.map { if (it.id == id) it.copy(order = order) else it }
            manifestsFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> {
            val list = manifestsFlow.value.map { ext ->
                val idx = orderedIds.indexOf(ext.id)
                if (idx >= 0) ext.copy(order = idx) else ext
            }
            manifestsFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun deleteExtension(id: String): AppResult<Unit> {
            manifestsFlow.value = manifestsFlow.value.filterNot { it.id == id }
            return AppResult.Success(Unit)
        }

        override fun getAllRepositories(): Flow<List<ExtensionRepository>> = reposFlow
        override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> = reposFlow.map { it.filter { repo -> repo.isEnabled } }

        override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> {
            val list = reposFlow.value.toMutableList()
            val index = list.indexOfFirst { it.id == repo.id }
            if (index != -1) list[index] = repo else list.add(repo)
            reposFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
            val list = reposFlow.value.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
            reposFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> {
            val list = reposFlow.value.map { if (it.id == id) it.copy(extensionCount = count) else it }
            reposFlow.value = list
            return AppResult.Success(Unit)
        }

        override suspend fun deleteRepository(id: String): AppResult<Unit> {
            reposFlow.value = reposFlow.value.filterNot { it.id == id }
            return AppResult.Success(Unit)
        }
    }

    private class FakeVmSettingsRepository : SettingsRepository {
        val settingsFlow = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = settingsFlow.asStateFlow()

        override suspend fun setThemeMode(mode: AppThemeMode) {
            settingsFlow.value = settingsFlow.value.copy(themeMode = mode)
        }
        override suspend fun setAmoledMode(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(amoledMode = enabled)
        }
        override suspend fun setDynamicColor(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(dynamicColor = enabled)
        }
        override suspend fun setAutoPlayNext(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoPlayNext = enabled)
        }
        override suspend fun setAutoSkipIntro(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoSkipIntro = enabled)
        }
        override suspend fun setAutoSkipOutro(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoSkipOutro = enabled)
        }
        override suspend fun setAutoSkipRecap(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoSkipRecap = enabled)
        }
        override suspend fun setSkipOnce(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(skipOnce = enabled)
        }
        override suspend fun setManualSkip(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(manualSkip = enabled)
        }
        override suspend fun setDefaultQuality(quality: String) {
            settingsFlow.value = settingsFlow.value.copy(defaultQuality = quality)
        }
        override suspend fun setDoubleTapSeekSeconds(seconds: Int) {
            settingsFlow.value = settingsFlow.value.copy(
                gestures = settingsFlow.value.gestures.copy(doubleTapSeekSeconds = seconds)
            )
        }
        override suspend fun setDoubleTapSeek(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(
                gestures = settingsFlow.value.gestures.copy(enableDoubleTapSeek = enabled)
            )
        }
        override suspend fun setPreferredAudioLanguage(language: String) {
            settingsFlow.value = settingsFlow.value.copy(preferredAudioLanguage = language)
        }
        override suspend fun setSubtitleLanguage(language: String) {
            settingsFlow.value = settingsFlow.value.copy(
                subtitles = settingsFlow.value.subtitles.copy(preferredLanguage = language)
            )
        }
        override suspend fun setSubtitleFontSize(sizeSp: Int) {
            settingsFlow.value = settingsFlow.value.copy(
                subtitles = settingsFlow.value.subtitles.copy(fontSizeSp = sizeSp)
            )
        }
        override suspend fun setDefaultExtensionId(extensionId: String) {
            settingsFlow.value = settingsFlow.value.copy(defaultExtensionId = extensionId)
        }
        override suspend fun setSwipeSeek(enabled: Boolean) {}
        override suspend fun setBrightnessGesture(enabled: Boolean) {}
        override suspend fun setVolumeGesture(enabled: Boolean) {}
        override suspend fun setLongPressSpeed(enabled: Boolean) {}
        override suspend fun setLongPressSpeedMultiplier(multiplier: Float) {}
        override suspend fun setKeepScreenAwake(enabled: Boolean) {}
        override suspend fun setVideoFitMode(fitMode: String) {}
        override suspend fun setSubtitleDelayMs(delayMs: Long) {}
        override suspend fun setSubtitleFontStyle(fontStyle: String) {}
        override suspend fun setSubtitleTextColor(colorHex: String) {}
        override suspend fun setSubtitleTextOpacity(opacity: Float) {}
        override suspend fun setSubtitleBackgroundColor(colorHex: String) {}
        override suspend fun setSubtitleBackgroundOpacity(opacity: Float) {}
        override suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {}
        override suspend fun setSubtitleOutlineColor(colorHex: String) {}
        override suspend fun setSubtitleBottomMarginDp(marginDp: Int) {}
        override suspend fun setAutoUpdateAniList(enabled: Boolean) {}
        override suspend fun setCompletionPercentage(percentage: Int) {}
        override suspend fun setSyncProgress(enabled: Boolean) {}
        override suspend fun setAutoMarkCompleted(enabled: Boolean) {}
    }
}
