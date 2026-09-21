package com.example.features.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.SyncStatus
import com.example.domain.model.tracking.TrackStatus
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.TrackingRepository
import com.example.domain.usecase.tracking.AniListLoginUseCase
import com.example.domain.usecase.tracking.AniListLogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: AniListUser? = null,
    val syncStatus: SyncStatus = SyncStatus.NOT_LOGGED_IN,
    val selectedTab: TrackStatus = TrackStatus.WATCHING,
    val mediaEntries: List<AniListMediaEntry> = emptyList(),
    val isLoadingList: Boolean = false,
    val isLoggingIn: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

class AccountViewModel(
    private val aniListRepository: AniListRepository,
    private val trackingRepository: TrackingRepository,
    private val loginUseCase: AniListLoginUseCase,
    private val logoutUseCase: AniListLogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = combine(
        _uiState,
        aniListRepository.getStoredUser(),
        trackingRepository.getSyncStatus()
    ) { state, user, syncStatus ->
        state.copy(
            user = user,
            syncStatus = if (user == null) SyncStatus.NOT_LOGGED_IN else syncStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountUiState()
    )

    init {
        refreshUserProfile()
        loadMediaList(_uiState.value.selectedTab)
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            aniListRepository.fetchCurrentUserProfile()
        }
    }

    fun loginWithToken(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
            when (val result = loginUseCase(token)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoggingIn = false, user = result.data) }
                    loadMediaList(_uiState.value.selectedTab)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoggingIn = false) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update {
                it.copy(
                    user = null,
                    mediaEntries = emptyList(),
                    isLoggedOut = true,
                    syncStatus = SyncStatus.NOT_LOGGED_IN
                )
            }
        }
    }

    fun selectTab(status: TrackStatus) {
        _uiState.update { it.copy(selectedTab = status) }
        loadMediaList(status)
    }

    fun loadMediaList(status: TrackStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }
            when (val res = trackingRepository.getUserAnimeList(status)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoadingList = false, mediaEntries = res.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoadingList = false) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingList = false) }
                }
            }
        }
    }

    fun updateMediaProgress(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double? = null
    ) {
        viewModelScope.launch {
            val res = trackingRepository.updateTrackEntry(mediaId, status, progress, score)
            if (res is AppResult.Success) {
                // Refresh list
                loadMediaList(_uiState.value.selectedTab)
                refreshUserProfile()
            } else if (res is AppResult.Error) {
                _uiState.update { it.copy(errorMessage = res.error.message ?: "Failed to update AniList entry") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            aniListRepository: AniListRepository,
            trackingRepository: TrackingRepository,
            loginUseCase: AniListLoginUseCase,
            logoutUseCase: AniListLogoutUseCase
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountViewModel(
                    aniListRepository = aniListRepository,
                    trackingRepository = trackingRepository,
                    loginUseCase = loginUseCase,
                    logoutUseCase = logoutUseCase
                ) as T
            }
        }
    }
}
