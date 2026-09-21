package com.example.extension

import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiDomBridge
import com.example.data.extension.mangayomi.bridge.MangayomiHttpBridge
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import com.example.data.extension.mangayomi.runtime.MangayomiExecutionContext
import com.example.data.extension.mangayomi.runtime.MangayomiJsEngine
import com.example.data.extension.mangayomi.runtime.MangayomiRuntime
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.SourceType
import com.example.domain.source.DefaultSourceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.javascript.Context

/**
 * Verification test suite for Phase 3 Mangayomi JavaScript Extension Runtime.
 * Uses real JS engine execution and real adapter integration.
 */
class Phase3MangayomiRuntimeTest {

    // Real test extension script fixture adhering to Mangayomi Anime extension standard
    private val validTestExtensionScript = """
        var source = {
            getPopular: function(page) {
                return [
                    {
                        link: "https://anime.test/watch/frieren-1",
                        name: "Frieren: Beyond Journey's End",
                        imageUrl: "https://anime.test/covers/frieren.jpg",
                        description: "An elf mage journeys across the land.",
                        status: "Finished"
                    },
                    {
                        link: "https://anime.test/watch/jujutsu-2",
                        name: "Jujutsu Kaisen Season 2",
                        imageUrl: "https://anime.test/covers/jjk2.jpg",
                        description: "Shibuya Incident Arc.",
                        status: "Finished"
                    }
                ];
            },
            getLatestUpdates: function(page) {
                return [
                    {
                        link: "https://anime.test/watch/solo-leveling-ep5",
                        name: "Solo Leveling",
                        imageUrl: "https://anime.test/covers/solo.jpg",
                        description: "Sung Jin-woo grows stronger.",
                        status: "Ongoing"
                    }
                ];
            },
            search: function(query, page, filters) {
                if (query.toLowerCase().indexOf("frieren") !== -1) {
                    return [
                        {
                            link: "https://anime.test/watch/frieren-1",
                            name: "Frieren: Beyond Journey's End",
                            imageUrl: "https://anime.test/covers/frieren.jpg",
                            description: "An elf mage journeys across the land.",
                            status: "Finished"
                        }
                    ];
                }
                return [];
            },
            getDetail: function(url) {
                return {
                    name: "Frieren: Beyond Journey's End",
                    imageUrl: "https://anime.test/covers/frieren.jpg",
                    description: "An elf mage embarks on a nostalgic quest.",
                    status: "Completed",
                    author: "Kanehito Yamada",
                    genres: ["Adventure", "Drama", "Fantasy"],
                    episodes: [
                        {
                            number: 1,
                            name: "The Journey's End",
                            url: "https://anime.test/watch/frieren-ep-1",
                            dateUpload: "2023-09-29",
                            scanlator: "SubGroup"
                        },
                        {
                            number: 2,
                            name: "It Didn't Have to Be Magic",
                            url: "https://anime.test/watch/frieren-ep-2",
                            dateUpload: "2023-09-29",
                            scanlator: "SubGroup"
                        }
                    ]
                };
            },
            getVideoList: function(url) {
                return [
                    {
                        url: "https://stream.anime.test/frieren/ep1/master.m3u8",
                        originalUrl: "https://stream.anime.test/frieren/ep1/master.m3u8",
                        quality: "1080p (Multi-Audio)",
                        server: "FastStream CDN",
                        headers: {
                            "Referer": "https://anime.test/",
                            "User-Agent": "Animey/1.0"
                        },
                        subtitles: [
                            {
                                url: "https://stream.anime.test/subs/en.vtt",
                                lang: "en",
                                label: "English [Sub]"
                            },
                            {
                                url: "https://stream.anime.test/subs/es.vtt",
                                lang: "es",
                                label: "Spanish"
                            }
                        ],
                        audios: [
                            {
                                url: "https://stream.anime.test/audio/jp.aac",
                                lang: "jp",
                                label: "Japanese (Original)"
                            },
                            {
                                url: "https://stream.anime.test/audio/en.aac",
                                lang: "en",
                                label: "English Dub"
                            }
                        ]
                    }
                ];
            },
            getSourcePreferences: function() {
                return [
                    {
                        key: "server_pref",
                        title: "Default Server",
                        summary: "Choose default streaming mirror",
                        defaultValue: "FastStream CDN"
                    }
                ];
            },
            setPreference: function(key, val) {
                // saved
            }
        };
    """.trimIndent()

    @Test
    fun testJsEngineSandboxedExecution() {
        val cx = Context.enter()
        try {
            val scope = MangayomiJsEngine.createSandboxedScope(cx)
            val result = cx.evaluateString(scope, "2 + 3 * 4", "test.js", 1, null)
            assertEquals(14, (result as Number).toInt())
        } finally {
            Context.exit()
        }
    }

    @Test
    fun testCryptoBridgeInJs() {
        val cx = Context.enter()
        try {
            val scope = MangayomiJsEngine.createSandboxedScope(cx)
            val base64Res = cx.evaluateString(scope, "btoa('Hello Animey')", "test.js", 1, null)
            assertEquals("SGVsbG8gQW5pbWV5", Context.toString(base64Res))

            val atobRes = cx.evaluateString(scope, "atob('SGVsbG8gQW5pbWV5')", "test.js", 1, null)
            assertEquals("Hello Animey", Context.toString(atobRes))

            val md5Res = cx.evaluateString(scope, "md5('test')", "test.js", 1, null)
            assertEquals("098f6bcd4621d373cade4e832627b4f6", Context.toString(md5Res))
        } finally {
            Context.exit()
        }
    }

    @Test
    fun testDomBridgeHtmlParsing() {
        val dom = MangayomiDomBridge()
        val doc = dom.parseHtml("<div class='container'><h1 class='title'>Frieren</h1><a href='/watch' class='link'>Watch</a></div>")

        val title = doc.querySelector(".title")
        assertNotNull(title)
        assertEquals("Frieren", title?.text)

        val link = doc.querySelector(".link")
        assertNotNull(link)
        assertEquals("/watch", link?.attr("href"))
    }

    @Test
    fun testMangayomiExtensionFullLifecycleAndExecution() = runBlocking {
        val registry = DefaultSourceRegistry()
        val runtime = MangayomiRuntime(sourceRegistry = registry)

        val manifest = MangayomiExtensionManifest(
            id = "mangayomi.test.stream",
            name = "Mangayomi Anime Test Source",
            lang = "en",
            baseUrl = "https://anime.test",
            scriptContent = validTestExtensionScript
        )

        // 1. Load extension
        val loadResult = runtime.loadExtension(manifest)
        assertTrue(loadResult is AppResult.Success)
        val adapter = (loadResult as AppResult.Success).data
        assertNotNull(adapter)
        assertEquals("mangayomi.test.stream", adapter.id)

        // Check registry integration
        val registeredSource = registry.getSource("mangayomi.test.stream")
        assertNotNull(registeredSource)
        assertEquals(ExtensionLifecycleState.ACTIVE, runtime.getExtensionState("mangayomi.test.stream"))

        // 2. Real Popular Anime execution
        val popularResult = adapter.getPopularAnime(1)
        assertTrue(popularResult is AppResult.Success)
        val popularList = (popularResult as AppResult.Success).data
        assertEquals(2, popularList.size)
        assertEquals("Frieren: Beyond Journey's End", popularList[0].title)
        assertEquals("mangayomi.test.stream", popularList[0].sourceId)

        // 3. Real Search execution
        val searchResult = adapter.searchAnime("Frieren", 1)
        assertTrue(searchResult is AppResult.Success)
        val searchList = (searchResult as AppResult.Success).data
        assertEquals(1, searchList.size)
        assertEquals("Frieren: Beyond Journey's End", searchList[0].title)

        // 4. Real Anime Details execution
        val detailResult = adapter.getAnimeDetails("https://anime.test/watch/frieren-1")
        assertTrue(detailResult is AppResult.Success)
        val detail = (detailResult as AppResult.Success).data
        assertEquals("Frieren: Beyond Journey's End", detail.title)
        assertTrue(detail.genres.contains("Adventure"))
        assertTrue(detail.description.contains("Kanehito Yamada"))

        // 5. Real Episodes extraction
        val epResult = adapter.getEpisodes("https://anime.test/watch/frieren-1")
        assertTrue(epResult is AppResult.Success)
        val epList = (epResult as AppResult.Success).data
        assertEquals(2, epList.size)
        assertEquals(1f, epList[0].number)
        assertEquals("The Journey's End [SubGroup]", epList[0].title)
        assertEquals("https://anime.test/watch/frieren-ep-1", epList[0].sourceEpisodeId)

        // 6. Real Video Sources extraction & Mapping to Animey VideoSource
        val videoResult = adapter.getVideoSources(epList[0])
        assertTrue(videoResult is AppResult.Success)
        val videos = (videoResult as AppResult.Success).data
        assertEquals(1, videos.size)

        val stream = videos[0]
        assertEquals("https://stream.anime.test/frieren/ep1/master.m3u8", stream.url)
        assertEquals(SourceType.HLS, stream.type)
        assertEquals("1080p (Multi-Audio)", stream.quality)
        assertEquals("FastStream CDN", stream.serverName)
        assertEquals("https://anime.test/", stream.referer)
        assertEquals("https://anime.test/", stream.headers["Referer"])

        // Subtitles mapping
        assertEquals(2, stream.subtitles.size)
        assertEquals("English [Sub]", stream.subtitles[0].label)
        assertEquals("en", stream.subtitles[0].language)
        assertEquals("https://stream.anime.test/subs/en.vtt", stream.subtitles[0].url)

        // Audio tracks mapping
        assertEquals(2, stream.audioTracks.size)
        assertEquals("Japanese (Original)", stream.audioTracks[0].label)
        assertEquals("jp", stream.audioTracks[0].language)
        assertEquals("https://stream.anime.test/audio/jp.aac", stream.audioTracks[0].url)

        // 7. Preferences extraction
        val prefs = adapter.getPreferences()
        assertEquals(1, prefs.size)
        assertEquals("server_pref", prefs[0].key)
        assertEquals("Default Server", prefs[0].title)

        // 8. Disable / Enable lifecycle
        runtime.setExtensionEnabled("mangayomi.test.stream", false)
        assertEquals(ExtensionLifecycleState.DISABLED, runtime.getExtensionState("mangayomi.test.stream"))
        assertFalse(registry.enabledSources.value.any { it.id == "mangayomi.test.stream" })

        runtime.setExtensionEnabled("mangayomi.test.stream", true)
        assertEquals(ExtensionLifecycleState.ACTIVE, runtime.getExtensionState("mangayomi.test.stream"))
        assertTrue(registry.enabledSources.value.any { it.id == "mangayomi.test.stream" })

        // 9. Unload extension
        val unloadResult = runtime.unloadExtension("mangayomi.test.stream")
        assertTrue(unloadResult is AppResult.Success)
        assertNull(registry.getSource("mangayomi.test.stream"))
    }

    @Test
    fun testBrokenExtensionIsolation() = runBlocking {
        val registry = DefaultSourceRegistry()
        val runtime = MangayomiRuntime(sourceRegistry = registry)

        // Broken extension that throws runtime exceptions in JS
        val brokenScript = """
            var source = {
                getPopular: function(page) {
                    throw new Error("Syntax or Network crash inside script");
                }
            };
        """.trimIndent()

        val brokenManifest = MangayomiExtensionManifest(
            id = "mangayomi.broken.stream",
            name = "Broken Extension",
            lang = "en",
            baseUrl = "https://broken.test",
            scriptContent = brokenScript
        )

        val loadResult = runtime.loadExtension(brokenManifest)
        assertTrue(loadResult is AppResult.Success)
        val adapter = (loadResult as AppResult.Success).data

        // Broken execution should return AppResult.Error gracefully without crashing the app
        val result = adapter.getPopularAnime(1)
        assertTrue(result is AppResult.Error)
        val err = (result as AppResult.Error).error
        assertTrue(err.message.contains("Syntax or Network crash inside script"))
    }
}
