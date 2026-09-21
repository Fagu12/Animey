package com.example.player

import com.example.core.result.AppResult
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Episode
import com.example.domain.model.HistoryEntry
import com.example.domain.model.SkipSegment
import com.example.domain.model.SkipType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import com.example.domain.player.PlaybackState
import com.example.domain.player.PlayerEngine
import com.example.domain.player.PlayerState
import com.example.domain.player.VideoTrack
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkipSegmentSystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeEngine: TestPlayerEngine
    private lateinit var fakeSettingsRepo: TestSettingsRepo
    private lateinit var fakeEpisodeRepo: TestEpisodeRepo
    private lateinit var fakeHistoryRepo: TestHistoryRepo
    private lateinit var playerController: PlayerController

    @Before
    fun setup() {
        fakeEngine = TestPlayerEngine()
        fakeSettingsRepo = TestSettingsRepo()
        fakeEpisodeRepo = TestEpisodeRepo()
        fakeHistoryRepo = TestHistoryRepo()

        playerController = PlayerController(
            playerEngine = fakeEngine,
            historyRepository = fakeHistoryRepo,
            episodeRepository = fakeEpisodeRepo,
            settingsRepository = fakeSettingsRepo,
            scope = testScope
        )
    }

    @Test
    fun testSkipSegmentModelFieldsAndTypes() {
        val intro = SkipSegment(
            type = SkipType.INTRO,
            startTime = 90000L,
            endTime = 180000L,
            source = "independent_aniskip"
        )
        val outro = SkipSegment(
            type = SkipType.OUTRO,
            startTime = 1200000L,
            endTime = 1290000L,
            source = "independent_metadata"
        )
        val recap = SkipSegment(
            type = SkipType.RECAP,
            startTime = 0L,
            endTime = 45000L,
            source = "independent_community"
        )

        assertEquals(SkipType.INTRO, intro.type)
        assertEquals(90000L, intro.startTime)
        assertEquals(180000L, intro.endTime)
        assertEquals("independent_aniskip", intro.source)
        assertEquals(90000L, intro.startMs)
        assertEquals(180000L, intro.endMs)

        assertEquals(SkipType.OUTRO, outro.type)
        assertEquals(1200000L, outro.startTime)
        assertEquals(1290000L, outro.endTime)

        assertEquals(SkipType.RECAP, recap.type)
        assertEquals(0L, recap.startTime)
        assertEquals(45000L, recap.endTime)
    }

    @Test
    fun testSkipMetadataIsIndependentFromStreamingProvider() = runTest(testDispatcher) {
        val independentSegments = listOf(
            SkipSegment(SkipType.RECAP, 0L, 30000L, source = "independent"),
            SkipSegment(SkipType.INTRO, 60000L, 150000L, source = "independent"),
            SkipSegment(SkipType.OUTRO, 1200000L, 1290000L, source = "independent")
        )

        val episode = Episode(
            id = "ep_1",
            animeId = "anime_1",
            sourceId = "independent_meta",
            sourceEpisodeId = "1",
            number = 1f,
            title = "Episode 1",
            durationMs = 1440000L,
            skipSegments = independentSegments
        )

        // Provider 1 (e.g. HLS stream from CDN A)
        val providerSourceA = VideoSource(
            id = "stream_a",
            providerId = "provider_a",
            serverName = "Server A",
            quality = "1080p",
            url = "https://cdn-a.example.com/master.m3u8"
        )
        playerController.playEpisode("anime_1", episode, providerSourceA)
        advanceUntilIdle()

        assertEquals(3, fakeEngine.segments.size)
        assertEquals(independentSegments, fakeEngine.segments)

        // Switch to Provider 2 (e.g. MP4 stream from CDN B)
        val providerSourceB = VideoSource(
            id = "stream_b",
            providerId = "provider_b",
            serverName = "Server B",
            quality = "720p",
            url = "https://cdn-b.example.com/video.mp4"
        )
        playerController.playEpisode("anime_1", episode, providerSourceB)
        advanceUntilIdle()

        // Verify skip metadata is completely preserved and independent of provider
        assertEquals(3, fakeEngine.segments.size)
        assertEquals(independentSegments, fakeEngine.segments)
    }

    @Test
    fun testAutomaticIntroOutroRecapSkip() = runTest(testDispatcher) {
        fakeSettingsRepo.setAutoSkipIntro(true)
        fakeSettingsRepo.setAutoSkipOutro(true)
        fakeSettingsRepo.setAutoSkipRecap(true)
        advanceUntilIdle()

        val segments = listOf(
            SkipSegment(SkipType.RECAP, 0L, 30000L),
            SkipSegment(SkipType.INTRO, 60000L, 150000L),
            SkipSegment(SkipType.OUTRO, 1200000L, 1290000L)
        )

        val episode = Episode(
            id = "ep_1",
            animeId = "anime_1",
            sourceId = "test",
            sourceEpisodeId = "1",
            number = 1f,
            title = "Episode 1",
            durationMs = 1440000L,
            skipSegments = segments
        )

        val source = VideoSource(id = "s1", providerId = "p1", serverName = "srv", quality = "1080p", url = "https://test.com/stream.m3u8")
        playerController.playEpisode("anime_1", episode, source)
        advanceUntilIdle()

        // 1. Enter Recap segment
        fakeEngine.simulatePlayback(positionMs = 10000L, activeSegment = segments[0])
        advanceUntilIdle()
        assertEquals(30000L, fakeEngine.lastSeekPosition)

        // 2. Enter Intro segment
        fakeEngine.simulatePlayback(positionMs = 70000L, activeSegment = segments[1])
        advanceUntilIdle()
        assertEquals(150000L, fakeEngine.lastSeekPosition)

        // 3. Enter Outro segment
        fakeEngine.simulatePlayback(positionMs = 1210000L, activeSegment = segments[2])
        advanceUntilIdle()
        assertEquals(1290000L, fakeEngine.lastSeekPosition)
    }

    @Test
    fun testSkipOncePreventsReSkippingWhenSeekingBack() = runTest(testDispatcher) {
        fakeSettingsRepo.setAutoSkipIntro(true)
        fakeSettingsRepo.setSkipOnce(true)
        advanceUntilIdle()

        val intro = SkipSegment(SkipType.INTRO, 60000L, 150000L)
        val episode = Episode(
            id = "ep_1",
            animeId = "anime_1",
            sourceId = "test",
            sourceEpisodeId = "1",
            number = 1f,
            title = "Episode 1",
            durationMs = 1440000L,
            skipSegments = listOf(intro)
        )

        val source = VideoSource(id = "s1", providerId = "p1", serverName = "srv", quality = "1080p", url = "https://test.com/stream.m3u8")
        playerController.playEpisode("anime_1", episode, source)
        advanceUntilIdle()

        // Playback enters intro -> auto skips
        fakeEngine.simulatePlayback(positionMs = 70000L, activeSegment = intro)
        advanceUntilIdle()
        assertEquals(150000L, fakeEngine.lastSeekPosition)

        // Reset seek tracker
        fakeEngine.lastSeekPosition = -1L

        // User rewinds back into intro (e.g. to listen to the song)
        fakeEngine.simulatePlayback(positionMs = 80000L, activeSegment = intro)
        advanceUntilIdle()

        // Because skipOnce is enabled, it must NOT auto-skip again
        assertEquals(-1L, fakeEngine.lastSeekPosition)
    }

    @Test
    fun testManualSkipButtonAction() = runTest(testDispatcher) {
        fakeSettingsRepo.setAutoSkipIntro(false)
        fakeSettingsRepo.setManualSkip(true)
        advanceUntilIdle()

        val intro = SkipSegment(SkipType.INTRO, 60000L, 150000L)
        val episode = Episode(
            id = "ep_1",
            animeId = "anime_1",
            sourceId = "test",
            sourceEpisodeId = "1",
            number = 1f,
            title = "Episode 1",
            durationMs = 1440000L,
            skipSegments = listOf(intro)
        )

        val source = VideoSource(id = "s1", providerId = "p1", serverName = "srv", quality = "1080p", url = "https://test.com/stream.m3u8")
        playerController.playEpisode("anime_1", episode, source)
        advanceUntilIdle()

        // Playback enters intro without auto-skip
        fakeEngine.simulatePlayback(positionMs = 70000L, activeSegment = intro)
        advanceUntilIdle()
        assertEquals(-1L, fakeEngine.lastSeekPosition)

        // User clicks the skip intro button
        playerController.skipCurrentSegment()
        advanceUntilIdle()
        assertEquals(150000L, fakeEngine.lastSeekPosition)
    }

    // Fakes for testing
    private class TestPlayerEngine : PlayerEngine {
        private val _state = MutableStateFlow(PlayerState())
        override val state: StateFlow<PlayerState> = _state.asStateFlow()
        var segments: List<SkipSegment> = emptyList()
        var lastSeekPosition: Long = -1L

        override fun prepare(source: VideoSource, startPositionMs: Long, autoPlay: Boolean) {
            _state.update {
                it.copy(
                    currentSource = source,
                    currentPositionMs = startPositionMs,
                    playbackState = PlaybackState.READY
                )
            }
        }

        override fun play() {}
        override fun pause() {}
        override fun togglePlayPause() {}
        override fun seekTo(positionMs: Long) {
            lastSeekPosition = positionMs
            _state.update { it.copy(currentPositionMs = positionMs) }
        }
        override fun seekForward(offsetMs: Long) {}
        override fun seekBackward(offsetMs: Long) {}

        override fun setPlaybackSpeed(speed: Float) {}
        override fun setVolume(volume: Float) {}
        override fun setMuted(isMuted: Boolean) {}
        override fun release() {}
        override fun selectAudioTrack(trackId: String?) {}
        override fun selectSubtitle(trackId: String?) {}
        override fun setSubtitleVisible(isVisible: Boolean) {}
        override fun selectVideoTrack(trackId: String?) {}
        override fun setSubtitleDelay(delayMs: Long) {}
        override fun setFullscreen(isFullscreen: Boolean) {}
        override fun setPipActive(isPip: Boolean) {}
        override fun retry() {}

        override fun setSkipSegments(segments: List<SkipSegment>) {
            this.segments = segments
        }

        fun simulatePlayback(positionMs: Long, activeSegment: SkipSegment?) {
            _state.update {
                it.copy(
                    currentPositionMs = positionMs,
                    activeSkipSegment = activeSegment,
                    durationMs = 1440000L
                )
            }
        }
    }

    private class TestSettingsRepo : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings.asStateFlow()

        override suspend fun setThemeMode(mode: AppThemeMode) {}
        override suspend fun setAmoledMode(enabled: Boolean) {}
        override suspend fun setDynamicColor(enabled: Boolean) {}
        override suspend fun setAutoPlayNext(enabled: Boolean) {
            _settings.update { it.copy(autoPlayNext = enabled) }
        }
        override suspend fun setAutoSkipIntro(enabled: Boolean) {
            _settings.update { it.copy(autoSkipIntro = enabled) }
        }
        override suspend fun setAutoSkipOutro(enabled: Boolean) {
            _settings.update { it.copy(autoSkipOutro = enabled) }
        }
        override suspend fun setAutoSkipRecap(enabled: Boolean) {
            _settings.update { it.copy(autoSkipRecap = enabled) }
        }
        override suspend fun setSkipOnce(enabled: Boolean) {
            _settings.update { it.copy(skipOnce = enabled) }
        }
        override suspend fun setManualSkip(enabled: Boolean) {
            _settings.update { it.copy(manualSkip = enabled) }
        }

        override suspend fun setDefaultQuality(quality: String) {}
        override suspend fun setDoubleTapSeekSeconds(seconds: Int) {}
        override suspend fun setDoubleTapSeek(enabled: Boolean) {}
        override suspend fun setSwipeSeek(enabled: Boolean) {}
        override suspend fun setBrightnessGesture(enabled: Boolean) {}
        override suspend fun setVolumeGesture(enabled: Boolean) {}
        override suspend fun setKeepScreenAwake(enabled: Boolean) {}
        override suspend fun setVideoFitMode(fitMode: String) {}
        override suspend fun setLongPressSpeed(enabled: Boolean) {}
        override suspend fun setLongPressSpeedMultiplier(multiplier: Float) {}
        override suspend fun setSubtitleLanguage(language: String) {}
        override suspend fun setSubtitleFontSize(sizeSp: Int) {}
        override suspend fun setSubtitleFontStyle(fontStyle: String) {}
        override suspend fun setSubtitleTextColor(colorHex: String) {}
        override suspend fun setSubtitleTextOpacity(opacity: Float) {}
        override suspend fun setSubtitleBackgroundColor(colorHex: String) {}
        override suspend fun setSubtitleBackgroundOpacity(opacity: Float) {}
        override suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {}
        override suspend fun setSubtitleOutlineColor(colorHex: String) {}
        override suspend fun setSubtitleBottomMarginDp(marginDp: Int) {}
        override suspend fun setSubtitleDelayMs(delayMs: Long) {}
        override suspend fun setAutoUpdateAniList(enabled: Boolean) {}
        override suspend fun setCompletionPercentage(percentage: Int) {}
        override suspend fun setSyncProgress(enabled: Boolean) {}
        override suspend fun setAutoMarkCompleted(enabled: Boolean) {}
        override suspend fun setDefaultExtensionId(extensionId: String) {}
        override suspend fun setPreferredAudioLanguage(language: String) {}
    }

    private class TestEpisodeRepo : EpisodeRepository {
        override fun getEpisodeById(episodeId: String): Flow<Episode?> = MutableStateFlow(null)
        override suspend fun getEpisodeByIdDirect(episodeId: String): Episode? = null
        override fun getEpisodesForAnime(animeId: String): Flow<List<Episode>> = MutableStateFlow(emptyList())
        override suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode> = emptyList()
        override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>> = MutableStateFlow(emptyList())
        override suspend fun saveEpisode(episode: Episode): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class TestHistoryRepo : HistoryRepository {
        override fun getAllHistory(): Flow<List<HistoryEntry>> = MutableStateFlow(emptyList())
        override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> = MutableStateFlow(null)
        override suspend fun recordHistory(
            animeId: String,
            episodeId: String,
            sourceId: String,
            positionMs: Long,
            durationMs: Long
        ): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearAllHistory(): AppResult<Unit> = AppResult.Success(Unit)
    }
}
