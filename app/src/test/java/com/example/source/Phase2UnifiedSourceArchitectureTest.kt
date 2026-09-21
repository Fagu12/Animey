package com.example.source

import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.data.source.adapter.AniyomiSourceAdapter
import com.example.data.source.adapter.ExistingApiSourceAdapter
import com.example.data.source.adapter.MangayomiSourceAdapter
import com.example.data.source.mapping.AnimeSourceMapper
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.source.DefaultSourceRegistry
import com.example.domain.source.ExtensionCompatibilityStatus
import com.example.domain.source.SourceTypeInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2UnifiedSourceArchitectureTest {

    @Test
    fun testExistingApiSourceAdapterDelegation() = runBlocking {
        val testExt = TestAnimeExtension()
        val adapter = ExistingApiSourceAdapter(testExt)

        assertEquals("builtin.test.anime", adapter.id)
        assertEquals("Animey Test Stream", adapter.name)
        assertEquals("en", adapter.lang)
        assertEquals(SourceTypeInfo.BUILTIN_API, adapter.sourceType)
        assertEquals(ExtensionCompatibilityStatus.COMPATIBLE, adapter.compatibilityStatus)

        // Test Popular
        val popular = adapter.getPopularAnime(1)
        assertTrue(popular is AppResult.Success)
        val animeList = (popular as AppResult.Success).data
        assertTrue(animeList.isNotEmpty())

        // Test Search
        val search = adapter.searchAnime("Solo", 1)
        assertTrue(search is AppResult.Success)
        val searchList = (search as AppResult.Success).data
        assertTrue(searchList.any { it.title.contains("Solo", ignoreCase = true) })

        // Test Details
        val details = adapter.getAnimeDetails(animeList.first().sourceAnimeId)
        assertTrue(details is AppResult.Success)
        val anime = (details as AppResult.Success).data
        assertEquals(animeList.first().title, anime.title)

        // Test Episodes
        val episodes = adapter.getEpisodes(animeList.first().sourceAnimeId)
        assertTrue(episodes is AppResult.Success)
        val epList = (episodes as AppResult.Success).data
        assertTrue(epList.isNotEmpty())

        // Test VideoSources
        val videos = adapter.getVideoSources(epList.first())
        assertTrue(videos is AppResult.Success)
        val vidList = (videos as AppResult.Success).data
        assertTrue(vidList.isNotEmpty())
    }

    @Test
    fun testAnimeSourceMapper() {
        val anime = AnimeSourceMapper.mapRawToAnime(
            sourceId = "test_source",
            sourceAnimeId = "anime_123",
            title = "Test Anime Title",
            description = "A wonderful story",
            poster = "https://example.com/poster.jpg",
            statusString = "airing",
            genres = listOf("Action", "Sci-Fi"),
            rating = 8.5,
            episodeCount = 24
        )

        assertEquals("Test Anime Title", anime.title)
        assertEquals("test_source", anime.sourceId)
        assertEquals("anime_123", anime.sourceAnimeId)
        assertEquals("Ongoing", anime.status)
        assertEquals(2, anime.genres.size)
        assertEquals(8.5, anime.rating ?: 0.0, 0.01)

        val episode = AnimeSourceMapper.mapRawToEpisode(
            sourceId = "test_source",
            animeId = anime.localId,
            sourceEpisodeId = "ep_1",
            episodeNumber = 1f,
            seasonNumber = 1,
            title = "Pilot Episode",
            durationMs = 1440000L
        )

        assertEquals("Pilot Episode", episode.title)
        assertEquals(1f, episode.number)
        assertEquals(1440000L, episode.durationMs)

        val videoSource = AnimeSourceMapper.mapRawToVideoSource(
            providerId = "test_source",
            serverName = "FastCDN",
            url = "https://stream.example.com/master.m3u8",
            quality = "1080p",
            headers = mapOf("User-Agent" to "Animey/1.0"),
            referer = "https://example.com",
            subtitles = listOf(TrackInfo("sub_en", "English", "en", "https://example.com/en.vtt", true))
        )

        assertEquals(SourceType.HLS, videoSource.type)
        assertEquals("1080p", videoSource.quality)
        assertEquals("FastCDN", videoSource.serverName)
        assertEquals("https://example.com", videoSource.headers["Referer"])
        assertEquals("https://example.com", videoSource.referer)
        assertEquals(1, videoSource.subtitles.size)
    }

    @Test
    fun testSourceTypeInference() {
        assertEquals(SourceType.HLS, SourceType.fromUrlOrMime("https://example.com/video.m3u8"))
        assertEquals(SourceType.HLS, SourceType.fromUrlOrMime("https://example.com/stream?auth=1", "application/x-mpegURL"))
        assertEquals(SourceType.MP4, SourceType.fromUrlOrMime("https://example.com/video.mp4"))
        assertEquals(SourceType.DASH, SourceType.fromUrlOrMime("https://example.com/manifest.mpd"))
        assertEquals(SourceType.DASH, SourceType.fromUrlOrMime("https://example.com/stream", "application/dash+xml"))
        assertEquals(SourceType.FILE, SourceType.fromUrlOrMime("file:///data/user/0/app/cache/video.mp4"))
        assertEquals(SourceType.EMBED, SourceType.fromUrlOrMime("https://embed.example.com/player/v123"))
        assertEquals(SourceType.UNKNOWN, SourceType.fromUrlOrMime("https://api.example.com/raw_stream_blob"))
    }

    @Test
    fun testSourceRegistryOperations() {
        val registry = DefaultSourceRegistry()
        val testExt1 = ExistingApiSourceAdapter(TestAnimeExtension())
        val aniyomiStub = AniyomiSourceAdapter("aniyomi.gogo", "GogoAnime", "en", "https://gogo.test")
        val mangayomiStub = MangayomiSourceAdapter("mangayomi.animepahe", "AnimePahe", "en", "https://animepahe.test")

        registry.registerSource(testExt1)
        registry.registerSource(aniyomiStub)
        registry.registerSource(mangayomiStub)

        assertEquals(3, registry.allSources.value.size)
        assertEquals(3, registry.enabledSources.value.size)
        assertEquals(testExt1.id, registry.preferredSource.value?.id)

        // Test Type Filtering
        val builtin = registry.getSourcesByType(SourceTypeInfo.BUILTIN_API)
        assertEquals(1, builtin.size)
        assertEquals(testExt1.id, builtin.first().id)

        val aniyomi = registry.getSourcesByType(SourceTypeInfo.ANIYOMI_EXTENSION)
        assertEquals(1, aniyomi.size)
        assertEquals(aniyomiStub.id, aniyomi.first().id)

        val mangayomi = registry.getSourcesByType(SourceTypeInfo.MANGAYOMI_EXTENSION)
        assertEquals(1, mangayomi.size)
        assertEquals(mangayomiStub.id, mangayomi.first().id)

        // Test Disabling Source
        registry.setSourceEnabled(aniyomiStub.id, false)
        assertEquals(3, registry.allSources.value.size)
        assertEquals(2, registry.enabledSources.value.size)
        assertFalse(registry.enabledSources.value.any { it.id == aniyomiStub.id })

        // Test Preferred Source Switching
        registry.setPreferredSource(mangayomiStub.id)
        assertEquals(mangayomiStub.id, registry.preferredSource.value?.id)

        // Test Unregister
        registry.unregisterSource(testExt1.id)
        assertNull(registry.getSource(testExt1.id))
        assertEquals(2, registry.allSources.value.size)
    }

    @Test
    fun testAniyomiAndMangayomiStubsFailGracefully() = runBlocking {
        val aniyomiStub = AniyomiSourceAdapter("aniyomi.stub", "Aniyomi Stub", "en", "https://aniyomi.test")
        val mangayomiStub = MangayomiSourceAdapter("mangayomi.stub", "Mangayomi Stub", "en", "https://mangayomi.test")

        val aniyomiRes = aniyomiStub.getPopularAnime(1)
        assertTrue(aniyomiRes is AppResult.Error)
        assertTrue((aniyomiRes as AppResult.Error).error.message.contains("Phase 4"))

        val mangayomiRes = mangayomiStub.getPopularAnime(1)
        assertTrue(mangayomiRes is AppResult.Error)
        assertTrue((mangayomiRes as AppResult.Error).error.message.contains("execution context is not loaded"))
    }

    @Test
    fun testExtensionLifecycleStateTransitions() {
        // Valid transitions
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.UNINSTALLED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.INSTALLING))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.INSTALLING.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.INSTALLED))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.INSTALLED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.LOADING))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.LOADING.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.INITIALIZING))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.INITIALIZING.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.PAUSED))
        assertTrue(com.example.domain.extension.runtime.ExtensionLifecycleState.PAUSED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE))

        // Invalid transitions should be rejected
        assertFalse(com.example.domain.extension.runtime.ExtensionLifecycleState.UNINSTALLED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE))
        assertFalse(com.example.domain.extension.runtime.ExtensionLifecycleState.DESTROYED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE))
        assertFalse(com.example.domain.extension.runtime.ExtensionLifecycleState.INSTALLED.canTransitionTo(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE))

        var exceptionThrown = false
        try {
            com.example.domain.extension.runtime.ExtensionLifecycleState.UNINSTALLED.validateTransition(com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }
}
