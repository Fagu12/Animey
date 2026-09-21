package com.example.features.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.download.DownloadManager
import com.example.domain.model.DownloadInfo
import com.example.domain.model.DownloadStatus
import com.example.domain.model.StorageUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DownloadFilterTab {
    ALL,
    DOWNLOADING,
    COMPLETED
}

data class DownloadsUiState(
    val downloads: List<DownloadInfo> = emptyList(),
    val filteredDownloads: List<DownloadInfo> = emptyList(),
    val storageUsage: StorageUsage = StorageUsage(),
    val selectedTab: DownloadFilterTab = DownloadFilterTab.ALL,
    val isLoading: Boolean = false,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val allCount: Int = 0
)

class DownloadsViewModel(
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DownloadFilterTab.ALL)
    private val _storageUsage = MutableStateFlow(StorageUsage())
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadManager.getAllDownloads(),
        _selectedTab,
        _storageUsage,
        _isLoading
    ) { allDownloads, tab, storage, loading ->
        val active = allDownloads.filter {
            it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.PAUSED
        }
        val completed = allDownloads.filter { it.status == DownloadStatus.COMPLETED }

        val filtered = when (tab) {
            DownloadFilterTab.ALL -> allDownloads
            DownloadFilterTab.DOWNLOADING -> active
            DownloadFilterTab.COMPLETED -> completed
        }

        DownloadsUiState(
            downloads = allDownloads,
            filteredDownloads = filtered,
            storageUsage = storage,
            selectedTab = tab,
            isLoading = loading,
            activeCount = active.size,
            completedCount = completed.size,
            allCount = allDownloads.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState(isLoading = true)
    )

    init {
        refreshStorage()
    }

    fun setSelectedTab(tab: DownloadFilterTab) {
        _selectedTab.value = tab
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _storageUsage.value = downloadManager.getStorageUsage()
        }
    }

    fun pauseDownload(episodeId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(episodeId)
            refreshStorage()
        }
    }

    fun resumeDownload(episodeId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(episodeId)
            refreshStorage()
        }
    }

    fun cancelDownload(episodeId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(episodeId)
            refreshStorage()
        }
    }

    fun retryDownload(episodeId: String) {
        viewModelScope.launch {
            downloadManager.retryDownload(episodeId)
            refreshStorage()
        }
    }

    fun deleteDownload(episodeId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownload(episodeId)
            refreshStorage()
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadManager.deleteAllDownloads()
            refreshStorage()
        }
    }

    companion object {
        fun provideFactory(
            downloadManager: DownloadManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadsViewModel(downloadManager) as T
            }
        }
    }
}
