package com.example.extension

import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiDomBridge
import com.example.data.extension.mangayomi.bridge.MangayomiHttpBridge
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import com.example.data.extension.mangayomi.runtime.MangayomiExecutionContext
import com.example.data.extension.mangayomi.runtime.MangayomiPhase
import com.example.data.extension.mangayomi.runtime.MangayomiRuntime
import com.example.data.extension.mangayomi.runtime.MangayomiRuntimeError
import com.example.data.source.adapter.MangayomiSourceAdapter
import com.example.data.source.mapping.AnimeSourceMapper
import com.example.domain.source.DefaultSourceRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class MangayomiBootstrapAndDetailPipelineTest {

    private lateinit var sourceRegistry: DefaultSourceRegistry
    private lateinit var runtime: MangayomiRuntime

    private val validJust4AnimeJs = """
        class Just4AnimeExtension extends Source {
            constructor() {
                super();
                this.name = "Just4Anime";
                this.baseUrl = "https://just4anime.test";
            }

            async getPopular(page) {
                class InnerItem {}
                return Promise.resolve([
                    {
                        name: "Frieren: Beyond Journey's End",
                        link: "/anime/frieren-123",
                        imageUrl: "https://just4anime.test/poster/frieren.jpg",
                        description: "An elven mage and her companions",
                        status: "Ongoing"
                    }
                ]);
            }

            async getDetail(url) {
                var fullUrl = url.startsWith("http") ? url : this.baseUrl + url;
                return Promise.resolve({
                    name: "Frieren: Beyond Journey's End",
                    imageUrl: "https://just4anime.test/poster/frieren.jpg",
                    coverUrl: "https://just4anime.test/banner/frieren.jpg",
                    description: "An elven mage and her former party members",
                    genre: ["Adventure", "Drama", "Fantasy"],
                    status: "Ongoing",
                    author: "Kanehito Yamada",
                    episodes: [
                        {
                            name: "Episode 1",
                            url: "https://just4anime.test/watch/frieren-ep1",
                            number: 1,
                            dateUpload: "2023-09-29"
                        },
                        {
                            name: "Episode 2",
                            url: "https://just4anime.test/watch/frieren-ep2",
                            number: 2,
                            dateUpload: "2023-10-06"
                        }
                    ]
                });
            }

            async getVideoList(url) {
                return Promise.resolve([
                    {
                        url: "https://just4anime.test/stream/ep1_1080p.m3u8",
                        quality: "1080p",
                        originalUrl: url,
                        server: "MainServer",
                        headers: { "Referer": "https://just4anime.test" }
                    }
                ]);
            }
        }
        var source = new Just4AnimeExtension();
    """.trimIndent()

    @Before
    fun setUp() {
        sourceRegistry = DefaultSourceRegistry()
        runtime = MangayomiRuntime(
            sourceRegistry = sourceRegistry,
            httpBridge = MangayomiHttpBridge(),
            domBridge = MangayomiDomBridge(),
            cryptoBridge = MangayomiCryptoBridge()
        )
    }

    @Test
    fun testBootstrapWithEs6AndPreambleSucceeds() = runBlocking {
        val manifest = MangayomiExtensionManifest(
            id = "just4anime",
            name = "Just4Anime",
            version = "1.0.0",
            scriptContent = validJust4AnimeJs
        )

        val loadResult = runtime.loadExtension(manifest)
        assertTrue("Expected loadExtension to succeed, got: $loadResult", loadResult is AppResult.Success)

        val adapter = (loadResult as AppResult.Success).data
        assertNotNull(adapter)

        val detailsRes = adapter.getAnimeDetails("/anime/frieren-123")
        assertTrue("Expected getAnimeDetails to succeed", detailsRes is AppResult.Success)

        val anime = (detailsRes as AppResult.Success).data
        assertEquals("Frieren: Beyond Journey's End", anime.title)
        assertEquals("https://just4anime.test/poster/frieren.jpg", anime.poster)
        assertTrue(anime.genres.contains("Fantasy"))
        assertEquals("anime_just4anime_${"/anime/frieren-123".hashCode()}", anime.localId)

        val episodesRes = adapter.getEpisodes("/anime/frieren-123")
        assertTrue("Expected getEpisodes to succeed", episodesRes is AppResult.Success)

        val episodes = (episodesRes as AppResult.Success).data
        assertEquals(2, episodes.size)

        // Verify Episode.animeId matches Anime.localId
        val ep1 = episodes.first()
        assertEquals("anime_just4anime_${"/anime/frieren-123".hashCode()}", ep1.animeId)
        assertEquals("https://just4anime.test/watch/frieren-ep1", ep1.sourceEpisodeId)
        assertEquals(1.0f, ep1.number)
    }

    @Test
    fun testBrokenExtensionScriptThrowsStructuredRuntimeError() = runBlocking {
        val invalidScript = """
            class BrokenExtension extends Source {
                getPopular(page) {
                    throw new Error("Syntax or execution failure inside extension");
                }
            }
            var source = new BrokenExtension();
        """.trimIndent()

        val manifest = MangayomiExtensionManifest(
            id = "broken_ext",
            name = "BrokenExt",
            version = "1.0.0",
            scriptContent = invalidScript
        )

        val loadResult = runtime.loadExtension(manifest)
        assertTrue(loadResult is AppResult.Success)
        val adapter = (loadResult as AppResult.Success).data

        val popResult = adapter.getPopularAnime(1)
        assertTrue("Expected error result when method throws", popResult is AppResult.Error)

        val err = (popResult as AppResult.Error).error
        assertTrue("Error message should mention method failure, got: ${err.message}", err.message.contains("execution failed") || err.message.contains("Syntax or execution failure"))
    }
}
