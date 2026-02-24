package com.boardgamegeek.ui.plays

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class PlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val syncPlays: Boolean
        get() = getApplication<Application>().preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] == true

    private data class PlayInfo(
        val mode: Mode,
        val name: String = "",
        val id: Int = BggContract.INVALID_ID,
    )

    enum class Mode {
        ALL, GAME, BUDDY, PLAYER, LOCATION
    }

    enum class FilterType {
        ALL, DIRTY, PENDING
    }

    enum class SortType {
        DATE, LOCATION, GAME, LENGTH
    }

    private val playInfo = MutableStateFlow<PlayInfo?>(null)

    private val _updateMessageFlow = MutableStateFlow<String?>(null)
    val updateMessageFlow: StateFlow<String?> = _updateMessageFlow.asStateFlow()

    private val _errorMessageFlow = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessageFlow.asStateFlow()

    private val _loggedPlayResultFlow = MutableStateFlow<PlayUploadResult?>(null)
    val loggedPlayResultFlow: StateFlow<PlayUploadResult?> = _loggedPlayResultFlow.asStateFlow()

    private val _isRefreshingFlow = MutableStateFlow(false)
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val allPlays: StateFlow<List<Play>> = playInfo
        .flatMapLatest {
            if (it == null) {
                emptyFlow()
            } else {
                when (it.mode) {
                    Mode.ALL -> playRepository.loadPlaysFlow()
                    Mode.GAME -> playRepository.loadPlaysByGameFlow(it.id)
                    Mode.LOCATION -> playRepository.loadPlaysByLocationFlow(it.name)
                    Mode.BUDDY -> playRepository.loadPlaysByUsernameFlow(it.name)
                    Mode.PLAYER -> playRepository.loadPlaysByPlayerNameFlow(it.name)
                }
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val plays: StateFlow<List<Play>> = combine(allPlays, sortType, filterType) { list, sortType, filterType ->
        filterAndSortPlays(list, sortType, filterType)
    }.stateInWhileSubscribed(viewModelScope, emptyList())

    val location: StateFlow<String> = playInfo
        .map { if (it?.mode == Mode.LOCATION) it.name else "" }
        .stateInWhileSubscribed(viewModelScope, "")

    fun clearErrorMessage() {
        _errorMessageFlow.value = null
    }

    fun clearLoggedPlayResult() {
        _loggedPlayResultFlow.value = null
    }

    fun clearUpdateMessage() {
        _updateMessageFlow.value = null
    }

    private fun filterAndSortPlays(
        list: List<Play>,
        sortType: SortType,
        filterType: FilterType,
    ): List<Play> {
        val filteredList = when (filterType) {
            FilterType.ALL -> list.filter { it.deleteTimestamp == 0L }
            FilterType.DIRTY -> list.filter { it.dirtyTimestamp > 0L }
            FilterType.PENDING -> list.filter { it.updateTimestamp > 0L || it.deleteTimestamp > 0L }
        }
        return when (sortType) {
            SortType.DATE -> filteredList.sortedByDescending { it.dateInMillis }
            SortType.LOCATION -> filteredList.sortedBy { it.location }
            SortType.GAME -> filteredList.sortedBy { it.gameName }
            SortType.LENGTH -> filteredList.sortedByDescending { it.length }
        }
    }

    fun setAll() {
        setFilter(FilterType.ALL)
        setSort(SortType.DATE)
        playInfo.value = PlayInfo(Mode.ALL)
    }

    fun setGame(gameId: Int) {
        playInfo.value = PlayInfo(Mode.GAME, id = gameId)
    }

    fun setLocation(locationName: String) {
        playInfo.value = PlayInfo(Mode.LOCATION, locationName)
    }

    fun setUsername(username: String) {
        playInfo.value = PlayInfo(Mode.BUDDY, username)
    }

    fun setPlayerName(playerName: String) {
        playInfo.value = PlayInfo(Mode.PLAYER, playerName)
    }

    fun setFilter(type: FilterType) {
        if (_filterType.value != type) _filterType.value = type
    }

    fun setSort(type: SortType) {
        if (_sortType.value != type) _sortType.value = type
    }

    fun renameLocation(oldLocationName: String, newLocationName: String) {
        viewModelScope.launch {
            val internalIds = playRepository.renameLocation(oldLocationName, newLocationName)
            playRepository.enqueueUploadRequest(internalIds)
            _updateMessageFlow.value =
                getApplication<BggApplication>().resources.getQuantityString(
                    R.plurals.msg_play_location_change,
                    internalIds.size,
                    internalIds.size,
                    oldLocationName,
                    newLocationName
                )
            setLocation(newLocationName)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                if (syncPlays && !_isRefreshingFlow.value) {
                    _isRefreshingFlow.value = true
                    val id = playInfo.value?.id ?: 0
                    when (playInfo.value?.mode) {
                        Mode.GAME -> playRepository.refreshPlaysForGame(id)
                        else -> playRepository.refreshRecentPlays()
                    }
                }
            } catch (e: Exception) {
                _errorMessageFlow.value = e.localizedMessage.orEmpty()
            } finally {
                _isRefreshingFlow.value = false
            }
        }
    }

    fun refreshPlaysByDate(timeInMillis: Long) {
        viewModelScope.launch {
            try {
                if (syncPlays && !_isRefreshingFlow.value) {
                    _isRefreshingFlow.value = true
                    playRepository.refreshPlaysForDate(timeInMillis)?.let {
                        _errorMessageFlow.value = it
                    }
                }
            } catch (e: Exception) {
                _errorMessageFlow.value = e.localizedMessage.orEmpty()
            } finally {
                _isRefreshingFlow.value = false
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                _errorMessageFlow.value = result.exceptionOrNull()?.localizedMessage.orEmpty()
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID) {
                        _loggedPlayResultFlow.value = it
                    }
                }
            }
        }
    }

    fun send(plays: List<Play>) {
        viewModelScope.launch {
            val idsToSend = mutableListOf<Long>()
            plays.forEach {
                if (playRepository.markAsUpdated(it.internalId)) {
                    idsToSend += it.internalId
                }
            }
            playRepository.enqueueUploadRequest(idsToSend)
        }
    }

    fun delete(plays: List<Play>) {
        viewModelScope.launch {
            val idsDeleted = mutableListOf<Long>()
            plays.forEach {
                if (playRepository.markAsDeleted(it.internalId)) {
                    idsDeleted += it.internalId
                }
            }
            playRepository.enqueueUploadRequest(idsDeleted)
        }
    }
}
