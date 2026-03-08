package com.boardgamegeek.ui.play

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.boardgamegeek.model.Play
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PlayViewModel @Inject constructor(
    application: Application,
    private val repository: PlayRepository,
) : AndroidViewModel(application) {
    private val arePlaysRefreshing = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()
    private val internalId = MutableStateFlow<Long?>(null)

    private val isDownloading = MutableStateFlow(false)
    private val isUploading: StateFlow<Boolean> = WorkManager.getInstance(getApplication())
        .getWorkInfosByTagLiveData(WORK_TAG)
        .asFlow()
        .map { list -> list.any { workInfo -> !workInfo.state.isFinished } }
        .stateInWhileSubscribed(viewModelScope, false)

    val isRefreshingFlow: StateFlow<Boolean> = combine(isDownloading, isUploading) { downloading, uploading ->
        downloading || uploading
    }.stateInWhileSubscribed(viewModelScope, false)

    private val _errorMessageEvents = MutableSharedFlow<String>(replay = 0)
    val errorMessageEvents: SharedFlow<String> = _errorMessageEvents.asSharedFlow()

    val play: StateFlow<Play?> = internalId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            repository.loadPlayFlow(id)
                .distinctUntilChanged()
                .onStart { attemptRefresh() }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val relatedExpansionPlays: StateFlow<List<Play>> = play
        .filterNotNull()
        .flatMapLatest { currentPlay ->
            repository.loadRelatedExpansionPlaysFlow(currentPlay)
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }

    fun refresh() {
        forceRefresh.set(true)
        attemptRefresh()
    }

    private fun attemptRefresh() {
        viewModelScope.launch {
            if (arePlaysRefreshing.compareAndSet(false, true)) {
                play.value?.let { play ->
                    if (forceRefresh.compareAndSet(true, false) ||
                        play.syncTimestamp.isOlderThan(2.hours)) {
                        isDownloading.value = true
                        try {
                            repository.refreshPlay(play)?.let {
                                _errorMessageEvents.emit(it)
                            }
                        } finally {
                            isDownloading.value = false
                        }
                    }
                }
                arePlaysRefreshing.set(false)
            }
        }
    }

    fun reload() {
        attemptRefresh()
    }

    fun discard() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsDiscarded(it.internalId))
                    refresh()
            }
        }
    }

    fun send() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsUpdated(it.internalId)) {
                    repository.enqueueUploadRequest(it.internalId, WORK_TAG)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            play.value?.let {
                if (repository.markAsDeleted(it.internalId)) {
                    repository.enqueueUploadRequest(it.internalId, WORK_TAG)
                }
            }
        }
    }

    companion object {
        private const val WORK_TAG = "PlayViewModel"
    }
}
