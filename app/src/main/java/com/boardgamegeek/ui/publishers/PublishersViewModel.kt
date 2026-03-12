package com.boardgamegeek.ui.publishers

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.BggApplication
import com.boardgamegeek.extensions.PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Company
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PublishersViewModel @Inject constructor(
    private val application: Application,
    private val publisherRepository: PublisherRepository,
) : ViewModel() {
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _sortType = MutableStateFlow(initialSort())
    val sortType: StateFlow<Company.SortType> = _sortType

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress: StateFlow<Pair<Int, Int>?> = _progress

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val isCalculating = AtomicBoolean()

    val publishers: StateFlow<List<Company>?> = combine(
        _sortType,
        refreshRequests.stateIn(viewModelScope, SharingStarted.Eagerly, Unit),
    ) { sortType, _ ->
        sortType
    }
        .flatMapLatest { sortType ->
            publisherRepository.loadPublishersFlow(sortType)
                .distinctUntilChanged()
                .onEach { _isRefreshing.value = false }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    init {
        refreshMissingThumbnails()
        calculateStats()
    }

    fun sort(sortType: Company.SortType) {
        if (_sortType.value == sortType) {
            refresh()
            return
        }
        _isRefreshing.value = true
        _sortType.value = sortType
        refreshRequests.tryEmit(Unit)
    }

    fun refresh() {
        _isRefreshing.value = true
        refreshRequests.tryEmit(Unit)
    }

    fun sectionHeader(publisher: Company?): String {
        return when (_sortType.value) {
            Company.SortType.NAME -> if (publisher?.name == "(Uncredited)") DEFAULT_HEADER else publisher?.name.firstChar()
            Company.SortType.ITEM_COUNT -> (publisher?.itemCount ?: 0).orderOfMagnitude()
            Company.SortType.WHITMORE_SCORE -> (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> DEFAULT_HEADER
        }
    }

    private fun refreshMissingThumbnails() {
        viewModelScope.launch {
            publisherRepository.refreshMissingThumbnails()
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation =
                (application as? BggApplication)
                    ?.preferences()
                    ?.get(PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L)
                    ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                publisherRepository.calculateStats(_progress)
                isCalculating.set(false)
            }
        }
    }

    private fun initialSort(): Company.SortType {
        return if (application.preferences().isStatusSetToSync(CollectionStatus.Rated)) {
            Company.SortType.WHITMORE_SCORE
        } else {
            Company.SortType.ITEM_COUNT
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
