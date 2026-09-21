package com.example.extension

import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiDomBridge
import com.example.data.extension.mangayomi.bridge.MangayomiHttpBridge
import com.example.data.extension.mangayomi.installer.DefaultMangayomiExtensionInstaller
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import com.example.data.extension.mangayomi.repository.DefaultMangayomiRepositoryClient
import com.example.data.extension.mangayomi.runtime.MangayomiRuntime
import com.example.data.extension.mangayomi.storage.DefaultInstalledExtensionStore
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.source.DefaultSourceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class Phase3BMangayomiRealExtensionTest {

    private lateinit var sourceRegistry: DefaultSourceRegistry
    private lateinit var runtime: MangayomiRuntime
    private lateinit var fakeStorageRepository: TestPhase3BExtensionStorageRepository
    private lateinit var installedStore: DefaultInstalledExtensionStore
    private lateinit var installer: DefaultMangayomiExtensionInstaller

    private val realAnimeExtensionJs = """
        class AnimePaheExtension extends Source {
            constructor() {
                super();
                this.baseUrl = "https://animepahe.ru";
                this.apiUrl = "https://animepahe.ru/api";
            }

            getPopular(page) {
                var res = new Client().get(this.apiUrl + "?m=airing&page=" + page);
                var json = JSON.parse(res.body);
                var list = [];
                for (var i = 0; i < json.data.length; i++) {
                    var item = json.data[i];
                    list.push({
                        name: item.anime_title,
                        link: "/anime/" + item.anime_session,
                        imageUrl: item.snapshot,
                        description: "Status: " + item.status,
                        status: item.status
                    });
                }
                return {
                    list: list,
                    hasNextPage: json.current_page < json.last_page
                };
            }

            getLatestUpdates(page) {
                return this.getPopular(page);
            }

            search(query, page, filters) {
                var res = new Client().get(this.apiUrl + "?m=search&q=" + encodeURIComponent(query));
                var json = JSON.parse(res.body);
                var list = [];
                for (var i = 0; i < json.data.length; i++) {
                    var item = json.data[i];
                    list.push({
                        name: item.title,
                        link: "/anime/" + item.session,
                        imageUrl: item.poster,
                        description: item.type + " • " + item.episodes + " eps",
                        status: item.status
                    });
                }
                return {
                    list: list,
                    hasNextPage: false
                };
            }

            getDetail(url) {
                var fullUrl = url.startsWith("http") ? url : this.baseUrl + url;
                var res = new Client().get(fullUrl);
                var doc = new Document(res.body);
                var title = doc.querySelector("h1") ? doc.querySelector("h1").text : "Unknown";
                var synopsis = doc.querySelector(".anime-synopsis") ? doc.querySelector(".anime-synopsis").text : "";
                var poster = doc.querySelector(".anime-poster img") ? doc.querySelector(".anime-poster img").attr("src") : "";
                var episodes = [];

                var epNodes = doc.querySelectorAll(".episode-item");
                for (var i = 0; i < epNodes.length; i++) {
                    var node = epNodes[i];
                    var epLink = node.attr("href");
                    var epNum = i + 1;
                    episodes.push({
                        name: "Episode " + epNum,
                        url: epLink,
                        number: epNum,
                        dateUpload: "2024-01-01"
                    });
                }

                // Test localStorage & AES decryption integration
                localStorage.setItem("last_viewed", title);
                var secretKey = "mangayomi_secret_key_16bytes";
                var encrypted = CryptoJS.AES.encrypt(title, secretKey);
                var decrypted = CryptoJS.AES.decrypt(encrypted, secretKey).toString(CryptoJS.enc.Utf8);
                localStorage.setItem("decrypted_title", decrypted);

                return {
                    name: title,
                    imageUrl: poster,
                    description: synopsis,
                    episodes: episodes,
                    genre: ["Action", "Fantasy", "Animation"]
                };
            }

            getVideoList(url) {
                var fullUrl = url.startsWith("http") ? url : this.baseUrl + url;
                var res = new Client().get(fullUrl);
                var doc = new Document(res.body);
                var videoLinks = [];

                var buttons = doc.querySelectorAll("#resolutionMenu button");
                for (var i = 0; i < buttons.length; i++) {
                    var btn = buttons[i];
                    var streamUrl = btn.attr("data-src");
                    var quality = btn.text;
                    if (streamUrl) {
                        videoLinks.push({
                            url: streamUrl,
                            quality: quality,
                            originalUrl: fullUrl,
                            server: "PaheServer",
                            headers: {
                                "Referer": this.baseUrl,
                                "User-Agent": "AnimeyPlayer/1.0"
                            },
                            subtitles: [
                                {
                                    url: "https://animepahe.ru/sub/ep1_en.vtt",
                                    lang: "en",
                                    title: "English"
                                }
                            ]
                        });
                    }
                }
                return videoLinks;
            }

            getSourcePreferences() {
                return [
                    {
                        key: "preferred_quality",
                        title: "Preferred Quality",
                        summary: "Default resolution",
                        defaultValue: "1080p"
                    }
                ];
            }
        }
        var source = new AnimePaheExtension();
    """.trimIndent()

    @Before
    fun setUp() {
        sourceRegistry = DefaultSourceRegistry()
        fakeStorageRepository = TestPhase3BExtensionStorageRepository()

        val tempDir = File(System.getProperty("java.io.tmpdir"), "animey_test_ext_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        installedStore = DefaultInstalledExtensionStore(baseDirOverride = tempDir)

        // Mock OkHttpClient returning real-looking HTML and JSON responses
        val mockHttpClient = createMockHttpClient()
        val httpBridge = MangayomiHttpBridge(mockHttpClient)
        val domBridge = MangayomiDomBridge()
        val cryptoBridge = MangayomiCryptoBridge()

        runtime = MangayomiRuntime(
            sourceRegistry = sourceRegistry,
            httpBridge = httpBridge,
            domBridge = domBridge,
            cryptoBridge = cryptoBridge
        )

        val repoClient = DefaultMangayomiRepositoryClient(mockHttpClient)

        installer = DefaultMangayomiExtensionInstaller(
            runtime = runtime,
            storageRepository = fakeStorageRepository,
            extensionStore = installedStore,
            repositoryClient = repoClient
        )
    }

    @Test
    fun testRepositoryIndexParsingAndFiltering() = runBlocking {
        val repoJson = """
            [
                {
                    "id": "en.animepahe",
                    "name": "AnimePahe",
                    "version": "0.1.5",
                    "versionCode": 5,
                    "lang": "en",
                    "baseUrl": "https://animepahe.ru",
                    "iconUrl": "https://animepahe.ru/icon.png",
                    "sourceCodeUrl": "https://raw.githubusercontent.com/user/repo/main/animepahe.js",
                    "itemType": 1,
                    "appMinVerReq": "0.1.0"
                },
                {
                    "id": "en.manga_source",
                    "name": "MangaDex",
                    "version": "1.0.0",
                    "itemType": 0,
                    "isManga": true
                }
            ]
        """.trimIndent()

        val client = DefaultMangayomiRepositoryClient(createStaticResponseClient(repoJson))
        val result = client.fetchRepositoryIndex("https://example.com/anime-index.json")

        assertTrue(result is AppResult.Success)
        val manifests = (result as AppResult.Success).data
        assertEquals(1, manifests.size)
        val manifest = manifests.first()
        assertEquals("en.animepahe", manifest.id)
        assertEquals("AnimePahe", manifest.name)
        assertEquals("0.1.5", manifest.version)
        assertEquals(1, manifest.itemType)
    }

    @Test
    fun testEndToEndInstallationAndExecution() = runBlocking {
        val manifest = MangayomiExtensionManifest(
            id = "en.animepahe",
            name = "AnimePahe",
            version = "0.1.5",
            versionCode = 5,
            lang = "en",
            baseUrl = "https://animepahe.ru",
            iconUrl = "https://animepahe.ru/icon.png",
            sourceCodeUrl = "https://example.com/animepahe.js",
            itemType = 1,
            scriptContent = realAnimeExtensionJs
        )

        // 1. Install extension
        val installResult = installer.installExtension(manifest)
        assertTrue("Install should succeed: $installResult", installResult is AppResult.Success)
        val adapter = (installResult as AppResult.Success).data
        assertNotNull(adapter)
        assertEquals("en.animepahe", adapter.id)

        // Verify stored in registry & db
        assertNotNull(sourceRegistry.getSource("en.animepahe"))
        assertTrue(installedStore.hasScript("en.animepahe"))
        assertEquals(1, fakeStorageRepository.installed.size)

        // 2. Test getPopular
        val popularResult = adapter.getPopularAnime(1)
        assertTrue(popularResult is AppResult.Success)
        val popularAnime = (popularResult as AppResult.Success).data
        assertTrue(popularAnime.isNotEmpty())
        assertEquals("Sousou no Frieren", popularAnime.first().title)
        assertEquals("/anime/frieren-session", popularAnime.first().sourceAnimeId)

        // 3. Test searchAnime
        val searchResult = adapter.searchAnime("Frieren", 1, emptyMap())
        assertTrue(searchResult is AppResult.Success)
        val searchList = (searchResult as AppResult.Success).data
        assertEquals(1, searchList.size)
        assertEquals("Sousou no Frieren", searchList.first().title)

        // 4. Test getAnimeDetails
        val detailsResult = adapter.getAnimeDetails("/anime/frieren-session")
        assertTrue(detailsResult is AppResult.Success)
        val details = (detailsResult as AppResult.Success).data
        assertEquals("Sousou no Frieren", details.title)
        assertTrue(details.description.contains("Elven mage"))

        // 5. Test getEpisodes
        val episodesResult = adapter.getEpisodes("/anime/frieren-session")
        assertTrue(episodesResult is AppResult.Success)
        val episodes = (episodesResult as AppResult.Success).data
        assertEquals(2, episodes.size)
        assertEquals(1f, episodes[0].number)
        assertEquals("/play/frieren/ep1", episodes[0].sourceEpisodeId)

        // 6. Test getVideoSources
        val testEpisode = Episode(
            id = "ep_1",
            animeId = "anime_1",
            sourceId = "en.animepahe",
            sourceEpisodeId = "/play/frieren/ep1",
            number = 1f,
            seasonNumber = 1,
            title = "Episode 1"
        )
        val streamResult = adapter.getVideoSources(testEpisode)
        assertTrue(streamResult is AppResult.Success)
        val streams = (streamResult as AppResult.Success).data
        assertEquals(2, streams.size)
        assertEquals("1080p", streams[0].quality)
        assertEquals("https://pahe.stream/frieren_1080p.m3u8", streams[0].url)
        assertEquals("https://animepahe.ru", streams[0].headers["Referer"])
        assertEquals(1, streams[0].subtitles.size)
        assertEquals("English", streams[0].subtitles[0].label)

        // 7. Test Extension Preferences
        val prefs = adapter.getPreferences()
        assertEquals(1, prefs.size)
        assertEquals("preferred_quality", prefs[0].key)

        // 8. Test Uninstall
        val uninstallResult = installer.uninstallExtension("en.animepahe")
        assertTrue(uninstallResult is AppResult.Success)
        assertEquals(0, fakeStorageRepository.installed.size)
    }

    private fun createStaticResponseClient(body: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    private fun createMockHttpClient(): OkHttpClient {
        val popularJson = """
            {
                "current_page": 1,
                "last_page": 5,
                "data": [
                    {
                        "anime_title": "Sousou no Frieren",
                        "anime_session": "frieren-session",
                        "snapshot": "https://animepahe.ru/poster1.jpg",
                        "status": "Finished Airing"
                    }
                ]
            }
        """.trimIndent()

        val searchJson = """
            {
                "data": [
                    {
                        "title": "Sousou no Frieren",
                        "session": "frieren-session",
                        "poster": "https://animepahe.ru/poster1.jpg",
                        "type": "TV",
                        "episodes": 28,
                        "status": "Finished"
                    }
                ]
            }
        """.trimIndent()

        val detailHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Sousou no Frieren</title></head>
            <body>
                <h1>Sousou no Frieren</h1>
                <div class="anime-poster"><img src="https://animepahe.ru/poster1.jpg" /></div>
                <div class="anime-synopsis">Elven mage Frieren embarks on a journey to understand humanity.</div>
                <div class="episode-list">
                    <a class="episode-item" href="/play/frieren/ep1">Episode 1</a>
                    <a class="episode-item" href="/play/frieren/ep2">Episode 2</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val playHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <div id="resolutionMenu">
                    <button data-src="https://pahe.stream/frieren_1080p.m3u8">1080p</button>
                    <button data-src="https://pahe.stream/frieren_720p.m3u8">720p</button>
                </div>
            </body>
            </html>
        """.trimIndent()

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val (responseBody, contentType) = when {
                    url.contains("m=airing") -> popularJson to "application/json"
                    url.contains("m=search") -> searchJson to "application/json"
                    url.contains("/play/") -> playHtml to "text/html"
                    url.contains("/anime/") -> detailHtml to "text/html"
                    else -> "{}" to "application/json"
                }

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody(contentType.toMediaType()))
                    .build()
            }
            .build()
    }
}

class TestPhase3BExtensionStorageRepository : ExtensionStorageRepository {
    val installed = mutableMapOf<String, ExtensionManifest>()
    val repositories = mutableMapOf<String, ExtensionRepository>()

    override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> = flowOf(installed.values.toList())
    override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> = flowOf(installed.values.filter { it.isEnabled })
    override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> = flowOf(installed.values.filter { it.hasUpdate })
    override fun getExtensionById(id: String): Flow<ExtensionManifest?> = flowOf(installed[id])
    override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> {
        installed[manifest.id] = manifest
        return AppResult.Success(Unit)
    }
    override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
        installed[id]?.let { installed[id] = it.copy(isEnabled = isEnabled) }
        return AppResult.Success(Unit)
    }
    override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> {
        installed[id]?.let { installed[id] = it.copy(hasUpdate = hasUpdate) }
        return AppResult.Success(Unit)
    }
    override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> {
        installed[id]?.let { installed[id] = it.copy(order = order) }
        return AppResult.Success(Unit)
    }
    override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteExtension(id: String): AppResult<Unit> {
        installed.remove(id)
        return AppResult.Success(Unit)
    }

    override fun getAllRepositories(): Flow<List<ExtensionRepository>> = flowOf(repositories.values.toList())
    override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> = flowOf(repositories.values.filter { it.isEnabled })
    override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> {
        repositories[repo.id] = repo
        return AppResult.Success(Unit)
    }
    override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
        repositories[id]?.let { repositories[id] = it.copy(isEnabled = isEnabled) }
        return AppResult.Success(Unit)
    }
    override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> {
        repositories[id]?.let { repositories[id] = it.copy(extensionCount = count) }
        return AppResult.Success(Unit)
    }
    override suspend fun deleteRepository(id: String): AppResult<Unit> {
        repositories.remove(id)
        return AppResult.Success(Unit)
    }
}
