package com.boardgamegeek.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Location
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val playRepository: PlayRepository,
) : ViewModel() {
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _sortType = MutableStateFlow(Location.SortType.NAME)
    val sortType: StateFlow<Location.SortType> = _sortType

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val locations: StateFlow<List<Location>?> = combine(
        _sortType,
        refreshRequests.stateIn(viewModelScope, SharingStarted.Eagerly, Unit),
    ) { sortType, _ ->
        sortType
    }
        .flatMapLatest { sortType ->
            playRepository.loadLocationsFlow(sortType)
                .distinctUntilChanged()
                .onEach { _isRefreshing.value = false }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    fun refresh() {
        _isRefreshing.value = true
        refreshRequests.tryEmit(Unit)
    }

    fun sort(sortType: Location.SortType) {
        if (_sortType.value == sortType) {
            refresh()
            return
        }
        _isRefreshing.value = true
        _sortType.value = sortType
        refreshRequests.tryEmit(Unit)
    }

    fun sectionHeader(location: Location?): String {
        return when (_sortType.value) {
            Location.SortType.NAME -> location?.name.firstChar()
            Location.SortType.PLAY_COUNT -> (location?.playCount ?: 0).orderOfMagnitude()
        }
    }
}
