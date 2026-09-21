package com.example.core.di

import android.content.Context
import com.example.data.local.database.AnimeyDatabase
import com.example.data.local.datastore.SettingsDataStore
import com.example.data.repository.AnimeRepositoryImpl
import com.example.data.repository.DownloadRepositoryImpl
import com.example.data.repository.EpisodeRepositoryImpl
import com.example.data.repository.ExtensionStorageRepositoryImpl
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.LibraryRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.TrackingBindingRepositoryImpl
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.ExtensionManager
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.manager.ExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.parser.ExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidator
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SkipMetadataRepository
import com.example.data.repository.SkipMetadataRepositoryImpl
import com.example.domain.repository.TrackingBindingRepository
import com.example.domain.usecase.AnimeSearchUseCase
import com.example.domain.usecase.GetAnimeDetailsUseCase
import com.example.domain.usecase.GetEpisodesUseCase
import com.example.domain.usecase.GetLatestAnimeUseCase
import com.example.domain.usecase.GetPopularAnimeUseCase
import com.example.domain.repository.SourcePreferenceRepository
import com.example.data.repository.SourcePreferenceRepositoryImpl
import com.example.domain.player.selector.ProviderSelector
import com.example.domain.player.selector.ServerSelector
import com.example.domain.player.selector.SourceSelector
import com.example.data.extension.builtin.MirrorTestAnimeExtension

import com.example.data.repository.AniListRepositoryImpl
import com.example.data.repository.TrackingRepositoryImpl
import com.example.data.tracking.anilist.AniListService
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.TrackingRepository
import com.example.domain.usecase.tracking.AniListLoginUseCase
import com.example.domain.usecase.tracking.AniListLogoutUseCase
import com.example.domain.usecase.tracking.AutoMapAnimeUseCase
import com.example.domain.usecase.tracking.GetAniListUserUseCase
import com.example.domain.usecase.tracking.SearchAniListMediaUseCase
import com.example.domain.usecase.tracking.UpdateAniListProgressUseCase

interface AppContainer {
    val database: AnimeyDatabase
    val settingsDataStore: SettingsDataStore
    val animeRepository: AnimeRepository
    val episodeRepository: EpisodeRepository
    val historyRepository: HistoryRepository
    val libraryRepository: LibraryRepository
    val downloadRepository: DownloadRepository
    val downloadManager: com.example.download.DownloadManager
    val networkMonitor: com.example.core.network.NetworkMonitor
    val cacheManager: com.example.core.cache.CacheManager
    val extensionStorageRepository: ExtensionStorageRepository
    val trackingBindingRepository: TrackingBindingRepository
    val aniListRepository: AniListRepository
    val trackingRepository: TrackingRepository
    val settingsRepository: SettingsRepository
    val skipMetadataRepository: SkipMetadataRepository
    val sourcePreferenceRepository: SourcePreferenceRepository
    val providerSelector: ProviderSelector
    val serverSelector: ServerSelector
    val sourceSelector: SourceSelector
    val extensionValidator: ExtensionValidator
    val extensionManifestParser: ExtensionManifestParser
    val extensionLogger: ExtensionLogger
    val extensionRuntimeManager: ExtensionRuntimeManager
    val extensionManager: ExtensionManager
    val sourceRegistry: com.example.domain.source.SourceRegistry
    val mangayomiRuntime: com.example.data.extension.mangayomi.runtime.MangayomiRuntime
    val mangayomiRepositoryClient: com.example.data.extension.mangayomi.repository.MangayomiRepositoryClient
    val installedExtensionStore: com.example.data.extension.mangayomi.storage.InstalledExtensionStore
    val mangayomiExtensionInstaller: com.example.data.extension.mangayomi.installer.MangayomiExtensionInstaller

    // Browsing use cases
    val animeSearchUseCase: AnimeSearchUseCase
    val getPopularAnimeUseCase: GetPopularAnimeUseCase
    val getLatestAnimeUseCase: GetLatestAnimeUseCase
    val getAnimeDetailsUseCase: GetAnimeDetailsUseCase
    val getEpisodesUseCase: GetEpisodesUseCase

    // Tracking use cases
    val aniListLoginUseCase: AniListLoginUseCase
    val aniListLogoutUseCase: AniListLogoutUseCase
    val getAniListUserUseCase: GetAniListUserUseCase
    val updateAniListProgressUseCase: UpdateAniListProgressUseCase
    val searchAniListMediaUseCase: SearchAniListMediaUseCase
    val autoMapAnimeUseCase: AutoMapAnimeUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AnimeyDatabase by lazy {
        AnimeyDatabase.getInstance(context)
    }

    override val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    override val extensionStorageRepository: ExtensionStorageRepository by lazy {
        ExtensionStorageRepositoryImpl(database.extensionDao(), database.repositoryDao())
    }

    override val trackingBindingRepository: TrackingBindingRepository by lazy {
        TrackingBindingRepositoryImpl(database.trackingBindingDao())
    }

    private val aniListService: AniListService by lazy {
        AniListService()
    }

    override val aniListRepository: AniListRepository by lazy {
        AniListRepositoryImpl(
            aniListService = aniListService,
            settingsDataStore = settingsDataStore
        )
    }

    override val trackingRepository: TrackingRepository by lazy {
        TrackingRepositoryImpl(
            aniListRepository = aniListRepository,
            trackingBindingRepository = trackingBindingRepository
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsDataStore)
    }

    override val sourcePreferenceRepository: SourcePreferenceRepository by lazy {
        SourcePreferenceRepositoryImpl(settingsDataStore)
    }

    override val providerSelector: ProviderSelector by lazy {
        ProviderSelector(extensionManager, sourcePreferenceRepository)
    }

    override val serverSelector: ServerSelector by lazy {
        ServerSelector(sourcePreferenceRepository)
    }

    override val sourceSelector: SourceSelector by lazy {
        SourceSelector(
            extensionManager = extensionManager,
            providerSelector = providerSelector,
            serverSelector = serverSelector,
            sourcePreferenceRepository = sourcePreferenceRepository
        )
    }

    override val extensionValidator: ExtensionValidator by lazy {
        DefaultExtensionValidator()
    }

    override val extensionManifestParser: ExtensionManifestParser by lazy {
        DefaultExtensionManifestParser()
    }

    override val extensionLogger: ExtensionLogger by lazy {
        DefaultExtensionLogger()
    }

    override val extensionRuntimeManager: ExtensionRuntimeManager by lazy {
        val builtInRuntime = BuiltInExtensionRuntime(extensionLogger)
        DefaultExtensionRuntimeManager(
            builtInRuntime = builtInRuntime,
            validator = extensionValidator,
            logger = extensionLogger
        )
    }

    override val sourceRegistry: com.example.domain.source.SourceRegistry by lazy {
        com.example.domain.source.DefaultSourceRegistry().apply {
            registerSource(com.example.data.source.adapter.ExistingApiSourceAdapter(TestAnimeExtension()))
            registerSource(com.example.data.source.adapter.ExistingApiSourceAdapter(MirrorTestAnimeExtension()))
        }
    }

    override val mangayomiRuntime: com.example.data.extension.mangayomi.runtime.MangayomiRuntime by lazy {
        com.example.data.extension.mangayomi.runtime.MangayomiRuntime(
            sourceRegistry = sourceRegistry
        )
    }

    override val mangayomiRepositoryClient: com.example.data.extension.mangayomi.repository.MangayomiRepositoryClient by lazy {
        com.example.data.extension.mangayomi.repository.DefaultMangayomiRepositoryClient()
    }

    override val installedExtensionStore: com.example.data.extension.mangayomi.storage.InstalledExtensionStore by lazy {
        com.example.data.extension.mangayomi.storage.DefaultInstalledExtensionStore(context)
    }

    override val mangayomiExtensionInstaller: com.example.data.extension.mangayomi.installer.MangayomiExtensionInstaller by lazy {
        com.example.data.extension.mangayomi.installer.DefaultMangayomiExtensionInstaller(
            runtime = mangayomiRuntime,
            storageRepository = extensionStorageRepository,
            extensionStore = installedExtensionStore,
            repositoryClient = mangayomiRepositoryClient
        )
    }

    override val extensionManager: ExtensionManager by lazy {
        DefaultExtensionManager(
            storageRepository = extensionStorageRepository,
            settingsRepository = settingsRepository,
            runtimeManager = extensionRuntimeManager,
            validator = extensionValidator,
            manifestParser = extensionManifestParser,
            mangayomiInstaller = mangayomiExtensionInstaller,
            sourceRegistry = sourceRegistry
        ).apply {
            registerExtension(TestAnimeExtension())
            registerExtension(MirrorTestAnimeExtension())
        }
    }

    override val animeRepository: AnimeRepository by lazy {
        AnimeRepositoryImpl(
            animeDao = database.animeDao(),
            episodeDao = database.episodeDao(),
            extensionManager = extensionManager
        )
    }

    override val episodeRepository: EpisodeRepository by lazy {
        EpisodeRepositoryImpl(database.episodeDao())
    }

    override val skipMetadataRepository: SkipMetadataRepository by lazy {
        SkipMetadataRepositoryImpl(database.episodeDao())
    }

    override val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.historyDao())
    }

    override val libraryRepository: LibraryRepository by lazy {
        LibraryRepositoryImpl(database.libraryDao())
    }

    override val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(database.downloadDao())
    }

    override val downloadManager: com.example.download.DownloadManager by lazy {
        com.example.download.DownloadManager(
            context = context,
            downloadRepository = downloadRepository,
            episodeRepository = episodeRepository,
            settingsRepository = settingsRepository
        )
    }

    override val networkMonitor: com.example.core.network.NetworkMonitor by lazy {
        com.example.core.network.ConnectivityNetworkMonitor(context)
    }

    override val cacheManager: com.example.core.cache.CacheManager by lazy {
        com.example.core.cache.CacheManager(
            context = context,
            animeDao = database.animeDao(),
            episodeDao = database.episodeDao()
        )
    }

    override val animeSearchUseCase: AnimeSearchUseCase by lazy {
        AnimeSearchUseCase(animeRepository)
    }

    override val getPopularAnimeUseCase: GetPopularAnimeUseCase by lazy {
        GetPopularAnimeUseCase(animeRepository)
    }

    override val getLatestAnimeUseCase: GetLatestAnimeUseCase by lazy {
        GetLatestAnimeUseCase(animeRepository)
    }

    override val getAnimeDetailsUseCase: GetAnimeDetailsUseCase by lazy {
        GetAnimeDetailsUseCase(animeRepository)
    }

    override val getEpisodesUseCase: GetEpisodesUseCase by lazy {
        GetEpisodesUseCase(animeRepository, episodeRepository)
    }

    override val aniListLoginUseCase: AniListLoginUseCase by lazy {
        AniListLoginUseCase(aniListRepository)
    }

    override val aniListLogoutUseCase: AniListLogoutUseCase by lazy {
        AniListLogoutUseCase(aniListRepository)
    }

    override val getAniListUserUseCase: GetAniListUserUseCase by lazy {
        GetAniListUserUseCase(aniListRepository)
    }

    override val updateAniListProgressUseCase: UpdateAniListProgressUseCase by lazy {
        UpdateAniListProgressUseCase(trackingRepository)
    }

    override val searchAniListMediaUseCase: SearchAniListMediaUseCase by lazy {
        SearchAniListMediaUseCase(aniListRepository)
    }

    override val autoMapAnimeUseCase: AutoMapAnimeUseCase by lazy {
        AutoMapAnimeUseCase(aniListRepository)
    }
}
