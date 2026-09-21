package com.example.player

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.HistoryEntry
import com.example.domain.model.SkipSegment
import com.example.domain.model.SkipType
import com.example.domain.model.SourceType
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerArchitectureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeEngine: FakePlayerEngine
    private lateinit var fakeHistoryRepo: FakeHistoryRepo
    private lateinit var fakeEpisodeRepo: FakeEpisodeRepo
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var playerController: PlayerController

    private val testHlsSource = VideoSource(
        id = "test_hls",
        providerId = "builtin.test.anime",
        serverName = "Animey HLS Fast Server",
        url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        type = SourceType.HLS,
        quality = "1080p",
        headers = mapOf("User-Agent" to "Animey/1.0", "Referer" to "https://animey.app"),
        subtitles = listOf(
            TrackInfo("sub_en", "English", "en", "https://animey.app/subs/en.vtt", isDefault = true),
            TrackInfo("sub_es", "Spanish", "es", "https://animey.app/subs/es.vtt")
        ),
        audioTracks = listOf(
            TrackInfo("audio_ja", "Japanese", "ja", isDefault = true),
            TrackInfo("audio_en", "English Dub", "en")
        ),
        isDefault = true
    )

    private val testMp4Source = VideoSource(
        id = "test_mp4",
        providerId = "builtin.test.anime",
        serverName = "Animey MP4 Fallback",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        type = SourceType.MP4,
        quality = "720p",
        isDefault = false
    )

    private val testEpisode = Episode(
        id = "ep_1",
        animeId = "anime_frieren",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "source_ep_1",
        number = 1.0f,
        title = "The Journey Begins",
        durationMs = 1_440_000L, // 24 mins
        lastPositionMs = 300_000L, // 5 mins resume position
        skipSegments = listOf(
            SkipSegment(SkipType.INTRO, 90_000L, 180_000L),
            SkipSegment(SkipType.OUTRO, 1_320_000L, 1_410_000L)
        )
    )

    @Before
    fun setUp() {
        fakeEngine = FakePlayerEngine()
        fakeHistoryRepo = FakeHistoryRepo()
        fakeEpisodeRepo = FakeEpisodeRepo()
        fakeSettingsRepo = FakeSettingsRepo()

        playerController = PlayerController(
            playerEngine = fakeEngine,
            historyRepository = fakeHistoryRepo,
            episodeRepository = fakeEpisodeRepo,
            settingsRepository = fakeSettingsRepo,
            scope = testScope
        )
    }

    @Test
    fun testPlayerEngine_HlsPlaybackAndTracks() = runTest(testDispatcher) {
        // Prepare with HLS source and resume position
        fakeEngine.prepare(testHlsSource, startPositionMs = 300_000L, autoPlay = true)

        val state = fakeEngine.state.value
        assertEquals(PlaybackState.READY, state.playbackState)
        assertTrue(state.isPlaying)
        assertEquals(300_000L, state.currentPositionMs)
        assertEquals(testHlsSource, state.currentSource)

        // Verify quality/video tracks
        assertEquals(3, state.availableVideoTracks.size)
        fakeEngine.selectVideoTrack("1080p")
        assertEquals("1080p", fakeEngine.state.value.selectedVideoTrack?.label)

        // Verify audio tracks & subtitles
        fakeEngine.selectAudioTrack("audio_ja")
        assertEquals("audio_ja", fakeEngine.state.value.selectedAudioTrack?.id)

        fakeEngine.selectSubtitle("sub_en")
        assertEquals("sub_en", fakeEngine.state.value.selectedSubtitle?.id)
        assertTrue(fakeEngine.state.value.isSubtitleVisible)

        fakeEngine.setSubtitleVisible(false)
        assertFalse(fakeEngine.state.value.isSubtitleVisible)
    }

    @Test
    fun testPlayerEngine_Mp4PlaybackSpeedAndSeeking() = runTest(testDispatcher) {
        fakeEngine.prepare(testMp4Source, startPositionMs = 0L)
        assertEquals(testMp4Source, fakeEngine.state.value.currentSource)

        // Playback speed
        fakeEngine.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, fakeEngine.state.value.playbackSpeed, 0.01f)

        // Seeking
        fakeEngine.seekTo(50_000L)
        assertEquals(50_000L, fakeEngine.state.value.currentPositionMs)

        fakeEngine.seekForward(10_000L)
        assertEquals(60_000L, fakeEngine.state.value.currentPositionMs)

        fakeEngine.seekBackward(20_000L)
        assertEquals(40_000L, fakeEngine.state.value.currentPositionMs)

        // Fullscreen and PiP
        fakeEngine.setFullscreen(true)
        assertTrue(fakeEngine.state.value.isFullscreen)

        fakeEngine.setPipActive(true)
        assertTrue(fakeEngine.state.value.isPipActive)
    }

    @Test
    fun testPlayerEngine_ErrorHandlingAndRetry() = runTest(testDispatcher) {
        fakeEngine.prepare(testHlsSource)
        fakeEngine.simulateError(AppError.PlayerError(errorCode = 2001, message = "Network timeout"))

        assertEquals(PlaybackState.ERROR, fakeEngine.state.value.playbackState)
        assertNotNull(fakeEngine.state.value.error)

        fakeEngine.retry()
        assertEquals(PlaybackState.READY, fakeEngine.state.value.playbackState)
        assertNull(fakeEngine.state.value.error)
    }

    @Test
    fun testPlayerController_resumePositionAndAutoSkip() = runTest(testDispatcher) {
        fakeSettingsRepo.setAutoSkipIntro(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Play episode with resume position and skip segments
        playerController.playEpisode("anime_frieren", testEpisode, testHlsSource)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(300_000L, fakeEngine.state.value.currentPositionMs)

        // Simulate entering intro segment (90s - 180s)
        fakeEngine.seekTo(100_000L)
        fakeEngine.updateCurrentPosition(100_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should automatically skip intro to 180s
        assertEquals(180_000L, fakeEngine.state.value.currentPositionMs)

        // Verify history recorded
        val historyRecord = fakeHistoryRepo.lastRecorded
        assertNotNull("History should be recorded", historyRecord)
        assertEquals("anime_frieren", historyRecord?.animeId)
        assertEquals("ep_1", historyRecord?.episodeId)
    }
}

private class FakePlayerEngine : PlayerEngine {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentSkipSegments: List<SkipSegment> = emptyList()

    override fun prepare(source: VideoSource, startPositionMs: Long, autoPlay: Boolean) {
        _state.update {
            it.copy(
                playbackState = PlaybackState.READY,
                isPlaying = autoPlay,
                currentSource = source,
                currentPositionMs = startPositionMs,
                durationMs = 1_440_000L,
                error = null,
                availableVideoTracks = listOf(
                    VideoTrack("1080p", 1920, 1080, 5000000, "1080p", isSelected = true),
                    VideoTrack("720p", 1280, 720, 2500000, "720p"),
                    VideoTrack("480p", 854, 480, 1000000, "480p")
                ),
                availableAudioTracks = source.audioTracks,
                availableSubtitles = source.subtitles
            )
        }
    }

    override fun play() {
        _state.update { it.copy(isPlaying = true) }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false) }
    }

    override fun togglePlayPause() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(currentPositionMs = positionMs) }
        checkSegments(positionMs)
    }

    override fun seekForward(offsetMs: Long) {
        seekTo(_state.value.currentPositionMs + offsetMs)
    }

    override fun seekBackward(offsetMs: Long) {
        seekTo((_state.value.currentPositionMs - offsetMs).coerceAtLeast(0L))
    }

    override fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume) }
    }

    override fun setMuted(isMuted: Boolean) {
        _state.update { it.copy(isMuted = isMuted) }
    }

    override fun selectVideoTrack(trackId: String?) {
        val selected = _state.value.availableVideoTracks.firstOrNull { it.id == trackId }
        _state.update { it.copy(selectedVideoTrack = selected) }
    }

    override fun selectAudioTrack(trackId: String?) {
        val selected = _state.value.availableAudioTracks.firstOrNull { it.id == trackId }
        _state.update { it.copy(selectedAudioTrack = selected) }
    }

    override fun selectSubtitle(trackId: String?) {
        val selected = _state.value.availableSubtitles.firstOrNull { it.id == trackId }
        _state.update { it.copy(selectedSubtitle = selected) }
    }

    override fun setSubtitleVisible(isVisible: Boolean) {
        _state.update { it.copy(isSubtitleVisible = isVisible) }
    }

    override fun setSubtitleDelay(delayMs: Long) {}

    override fun setSkipSegments(segments: List<SkipSegment>) {
        this.currentSkipSegments = segments
        checkSegments(_state.value.currentPositionMs)
    }

    override fun setFullscreen(isFullscreen: Boolean) {
        _state.update { it.copy(isFullscreen = isFullscreen) }
    }

    override fun setPipActive(isPip: Boolean) {
        _state.update { it.copy(isPipActive = isPip) }
    }

    override fun retry() {
        _state.update { it.copy(playbackState = PlaybackState.READY, error = null) }
    }

    override fun release() {
        _state.update { it.copy(playbackState = PlaybackState.IDLE) }
    }

    fun updateCurrentPosition(pos: Long) {
        _state.update { it.copy(currentPositionMs = pos) }
        checkSegments(pos)
    }

    fun simulateError(error: AppError) {
        _state.update { it.copy(playbackState = PlaybackState.ERROR, error = error) }
    }

    private fun checkSegments(pos: Long) {
        val active = currentSkipSegments.firstOrNull { pos in it.startMs..it.endMs }
        _state.update { it.copy(activeSkipSegment = active) }
    }
}

private class FakeHistoryRepo : HistoryRepository {
    data class RecordedHistory(
        val animeId: String,
        val episodeId: String,
        val sourceId: String,
        val positionMs: Long,
        val durationMs: Long
    )

    var lastRecorded: RecordedHistory? = null

    override fun getAllHistory(): Flow<List<HistoryEntry>> = MutableStateFlow(emptyList())
    override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> = MutableStateFlow(null)

    override suspend fun recordHistory(
        animeId: String,
        episodeId: String,
        sourceId: String,
        positionMs: Long,
        durationMs: Long
    ): AppResult<Unit> {
        lastRecorded = RecordedHistory(animeId, episodeId, sourceId, positionMs, durationMs)
        return AppResult.Success(Unit)
    }

    override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun clearAllHistory(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeEpisodeRepo : EpisodeRepository {
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

private class FakeSettingsRepo : SettingsRepository {
    private val _settings = MutableStateFlow(com.example.domain.model.AppSettings())
    override val settings: Flow<com.example.domain.model.AppSettings> = _settings.asStateFlow()

    override suspend fun setThemeMode(mode: com.example.domain.model.AppThemeMode) {}
    override suspend fun setAmoledMode(enabled: Boolean) {}
    override suspend fun setDynamicColor(enabled: Boolean) {}
    override suspend fun setAutoPlayNext(enabled: Boolean) {}
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
    override suspend fun setPreferredAudioLanguage(language: String) {}
    override suspend fun setSubtitleLanguage(language: String) {}
    override suspend fun setSubtitleFontSize(sizeSp: Int) {}
    override suspend fun setDefaultExtensionId(extensionId: String) {}
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
