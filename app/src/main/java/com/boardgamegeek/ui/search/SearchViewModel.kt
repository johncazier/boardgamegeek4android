package com.boardgamegeek.ui.search

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.SearchRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val application: Application,
    private val repository: SearchRepository,
    private val playRepository: PlayRepository
) : ViewModel() {
    private val _query = MutableStateFlow<Pair<String, Boolean>?>(null)
    val query: StateFlow<Pair<String, Boolean>?> = _query

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage = _errorMessage.asSharedFlow()

    private val _loggedPlayResult = MutableSharedFlow<PlayUploadResult>(extraBufferCapacity = 1)
    val loggedPlayResult = _loggedPlayResult.asSharedFlow()

    fun search(query: String) {
        FirebaseAnalytics.getInstance(application).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
            param("exact", true.toString())
        }
        if (_query.value?.first != query) _query.value = (query to true)
    }

    fun searchInexact(query: String) {
        FirebaseAnalytics.getInstance(application).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
            param("exact", false.toString())
        }
        if (_query.value?.first != query || _query.value?.second != false) _query.value = query to false
    }

    val searchResults: StateFlow<RefreshableResource<List<SearchResult>>?> = _query
        .flatMapLatest { q ->
            flow {
                if (q == null || q.first.isBlank()) {
                    emit(RefreshableResource.success(null))
                    return@flow
                }
                emit(RefreshableResource.refreshing(null))
                try {
                    val results = repository.search(q.first, q.second)
                    if (results.isEmpty() && q.second) {
                        searchInexact(q.first)
                        return@flow
                    }
                    emit(RefreshableResource.success(results))
                } catch (e: Exception) {
                    emit(RefreshableResource.error(e, application))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RefreshableResource.success(null))

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                _errorMessage.tryEmit(result.exceptionOrNull()?.message.orEmpty())
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID) {
                        _loggedPlayResult.tryEmit(it)
                    }
                }
            }
        }
    }
}
