package com.example.extension

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.data.extension.repository.RepositoryFetcher
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.SafeAnimeExtension
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntime
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionInfo
import com.example.domain.model.ExtensionInstallState
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.RepositoryIndex
import com.example.domain.model.SourceType
import com.example.domain.model.VideoSource
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.util.VersionComparator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionArchitectureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeStorageRepo: FakeExtensionStorageRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeFetcher: FakeRepositoryFetcher
    private lateinit var extensionManager: DefaultExtensionManager
    private lateinit var testExtension: TestAnimeExtension

    @Before
    fun setUp() {
        fakeStorageRepo = FakeExtensionStorageRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        fakeFetcher = FakeRepositoryFetcher()
        testExtension = TestAnimeExtension()

        extensionManager = DefaultExtensionManager(
            storageRepository = fakeStorageRepo,
            settingsRepository = fakeSettingsRepo,
            repositoryFetcher = fakeFetcher,
            scope = testScope
        )
        extensionManager.registerExtension(testExtension)
    }

    @Test
    fun testVersionComparator() {
        // Equal versions
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("v1.2.3", "1.2.3"))
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))

        // Update available
        assertTrue(VersionComparator.isUpdateAvailable("1.0.0", "1.0.1"))
        assertTrue(VersionComparator.isUpdateAvailable("1.2.0", "1.3.0"))
        assertTrue(VersionComparator.isUpdateAvailable("1.9.9", "2.0.0"))
        assertTrue(VersionComparator.isUpdateAvailable("v1.0.0", "v1.0.5"))

        // No update / downgrade
        assertFalse(VersionComparator.isUpdateAvailable("1.0.1", "1.0.0"))
        assertFalse(VersionComparator.isUpdateAvailable("2.0.0", "1.9.9"))
        assertFalse(VersionComparator.isUpdateAvailable("1.0.0", "1.0.0"))
    }

    @Test
    fun testRepositorySync_andExtensionLifecycle() = runTest(testDispatcher) {
        // Setup mock remote repository index
        val repoUrl = "https://extensions.animey.app/index.json"
        fakeFetcher.mockRepository(
            repoUrl,
            RepositoryIndex(
                name = "Official Community Repo",
                version = 1,
                extensions = listOf(
                    ExtensionManifest(
                        id = "builtin.test.anime",
                        name = "Animey Test Stream",
                        version = "1.1.0", // Update available over 1.0.0
                        language = "en",
                        description = "Updated test extension",
                        downloadUrl = "https://extensions.animey.app/test-1.1.0.apk",
                        isBuiltIn = true
                    ),
                    ExtensionManifest(
                        id = "community.anime.mock",
                        name = "Mock Anime Source",
                        version = "1.0.0",
                        language = "ja",
                        description = "Mock community extension",
                        downloadUrl = "https://extensions.animey.app/mock-1.0.0.apk",
                        isBuiltIn = false
                    )
                )
            )
        )

        // 1. Add Repository
        val addResult = extensionManager.addRepository("Official", repoUrl)
        assertTrue("Repository should be added", addResult is AppResult.Success)

        // 2. Sync Repositories & check update detection
        val syncResult = extensionManager.syncRepositories()
        assertTrue("Sync should succeed", syncResult is AppResult.Success)

        val updates = extensionManager.checkUpdates()
        assertTrue("Check updates should succeed", updates is AppResult.Success)
        val updateList = (updates as AppResult.Success).data
        assertEquals("Should detect update for builtin test extension", 1, updateList.size)
        assertEquals("builtin.test.anime", updateList.first().id)
        assertEquals("1.1.0", updateList.first().version)

        // 3. Install available extension from repository
        val mockManifest = ExtensionManifest(
            id = "community.anime.mock",
            name = "Mock Anime Source",
            version = "1.0.0",
            language = "ja",
            isBuiltIn = false
        )
        val installResult = extensionManager.installExtension(mockManifest)
        assertTrue("Install should succeed", installResult is AppResult.Success)

        // Verify stored in DB
        val installed = fakeStorageRepo.installedList
        assertTrue("Should contain installed mock extension", installed.any { it.id == "community.anime.mock" })

        // 4. Uninstall extension
        val uninstallResult = extensionManager.uninstallExtension("community.anime.mock")
        assertTrue("Uninstall should succeed", uninstallResult is AppResult.Success)
        assertFalse("Should no longer be installed", fakeStorageRepo.installedList.any { it.id == "community.anime.mock" })

        // Built-in uninstall protection
        val uninstallBuiltIn = extensionManager.uninstallExtension("builtin.test.anime")
        assertTrue("Should reject uninstalling built-in extension", uninstallBuiltIn is AppResult.Error)
    }

    @Test
    fun testBuiltInExtension_endToEndFlow() = runTest(testDispatcher) {
        // 1. Search
        val searchResults = testExtension.searchAnime("frieren")
        assertTrue("Search should succeed", searchResults is AppResult.Success)
        val animes = (searchResults as AppResult.Success).data
        assertEquals("Should find 1 anime for query 'frieren'", 1, animes.size)
        val frieren = animes.first()
        assertEquals("Frieren: Beyond Journey's End", frieren.title)

        // 2. Details
        val detailsResult = testExtension.getAnimeDetails(frieren.sourceAnimeId)
        assertTrue("Details should succeed", detailsResult is AppResult.Success)
        val animeDetails = (detailsResult as AppResult.Success).data
        assertEquals("Madhouse", animeDetails.studios.first())

        // 3. Episodes
        val episodesResult = testExtension.getEpisodes(frieren.sourceAnimeId)
        assertTrue("Episodes should succeed", episodesResult is AppResult.Success)
        val episodes = (episodesResult as AppResult.Success).data
        assertTrue("Should have episodes", episodes.isNotEmpty())
        val ep1 = episodes.first()
        assertEquals(1.0f, ep1.number)
        assertTrue("Episode 1 should have skip segments", ep1.skipSegments.isNotEmpty())

        // 4. Streams -> VideoSource
        val streamsResult = testExtension.getEpisodeStreams(ep1.sourceEpisodeId)
        assertTrue("Streams should succeed", streamsResult is AppResult.Success)
        val streams = (streamsResult as AppResult.Success).data
        assertTrue("Should provide streams", streams.isNotEmpty())
        val defaultStream = streams.first { it.isDefault }
        assertEquals(SourceType.HLS, defaultStream.type)
        assertTrue("Stream should have subtitles", defaultStream.subtitles.isNotEmpty())
    }

    @Test
    fun testSafeAnimeExtension_catchesExceptionsGracefully() = runTest(testDispatcher) {
        val brokenExtension = object : AnimeExtension {
            override val id = "broken.test"
            override val name = "Broken Provider"
            override val lang = "en"
            override val iconUrl = ""
            override val baseUrl = "https://broken.example"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                throw RuntimeException("Simulated scraper parsing failure or network crash")
            }
            override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> = throw IllegalStateException()
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>): AppResult<List<Anime>> = throw NullPointerException()
            override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> = throw RuntimeException()
            override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> = throw RuntimeException()
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>): AppResult<List<VideoSource>> = throw RuntimeException()
        }

        val safe = SafeAnimeExtension(brokenExtension)
        val result = safe.getPopularAnime(1)

        assertTrue("SafeAnimeExtension should catch exception and return AppResult.Error", result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue("Error should be ProviderError", error is AppError.ProviderError)
        assertEquals("Broken Provider", (error as AppError.ProviderError).provider)
    }

    @Test
    fun testExtensionManager_searchAllAndPreferences() = runTest(testDispatcher) {
        val searchResults = extensionManager.searchAll("Solo")
        assertEquals(1, searchResults.size)
        assertEquals("Solo Leveling", searchResults.first().title)

        // Test Preferences
        val prefs = testExtension.getPreferences()
        assertTrue("Test extension should expose preferences", prefs.isNotEmpty())
        val serverPref = prefs.first { it.key == "server_select" }
        assertNotNull(serverPref)

        testExtension.setPreference("server_select", "Mirror 1")
        val streams = (testExtension.getEpisodeStreams("ep1") as AppResult.Success).data
        assertTrue(streams.first().serverName.contains("Mirror 1"))
    }
}

private class FakeRepositoryFetcher : RepositoryFetcher {
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

private class FakeExtensionStorageRepository : ExtensionStorageRepository {
    private val manifestsFlow = MutableStateFlow<List<ExtensionManifest>>(emptyList())
    private val reposFlow = MutableStateFlow<List<ExtensionRepository>>(emptyList())

    val installedList: List<ExtensionManifest> get() = manifestsFlow.value

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

    override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)

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

private class FakeSettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun setThemeMode(mode: AppThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }
    override suspend fun setAmoledMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(amoledMode = enabled)
    }
    override suspend fun setDynamicColor(enabled: Boolean) {
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }
    override suspend fun setAutoPlayNext(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoPlayNext = enabled)
    }
    override suspend fun setAutoSkipIntro(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSkipIntro = enabled)
    }
    override suspend fun setAutoSkipOutro(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSkipOutro = enabled)
    }
    override suspend fun setAutoSkipRecap(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSkipRecap = enabled)
    }
    override suspend fun setSkipOnce(enabled: Boolean) {
        _settings.value = _settings.value.copy(skipOnce = enabled)
    }
    override suspend fun setManualSkip(enabled: Boolean) {
        _settings.value = _settings.value.copy(manualSkip = enabled)
    }
    override suspend fun setDefaultQuality(quality: String) {
        _settings.value = _settings.value.copy(defaultQuality = quality)
    }
    override suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        _settings.value = _settings.value.copy(
            gestures = _settings.value.gestures.copy(doubleTapSeekSeconds = seconds)
        )
    }
    override suspend fun setDoubleTapSeek(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            gestures = _settings.value.gestures.copy(enableDoubleTapSeek = enabled)
        )
    }
    override suspend fun setPreferredAudioLanguage(language: String) {
        _settings.value = _settings.value.copy(preferredAudioLanguage = language)
    }
    override suspend fun setSubtitleLanguage(language: String) {
        _settings.value = _settings.value.copy(
            subtitles = _settings.value.subtitles.copy(preferredLanguage = language)
        )
    }
    override suspend fun setSubtitleFontSize(sizeSp: Int) {
        _settings.value = _settings.value.copy(
            subtitles = _settings.value.subtitles.copy(fontSizeSp = sizeSp)
        )
    }
    override suspend fun setDefaultExtensionId(extensionId: String) {
        _settings.value = _settings.value.copy(defaultExtensionId = extensionId)
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
