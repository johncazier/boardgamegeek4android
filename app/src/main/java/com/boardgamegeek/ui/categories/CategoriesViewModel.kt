package com.boardgamegeek.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Category
import com.boardgamegeek.repository.CategoryRepository
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
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository,
) : ViewModel() {
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _sortType = MutableStateFlow(Category.SortType.ITEM_COUNT)
    val sortType: StateFlow<Category.SortType> = _sortType

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val categories: StateFlow<List<Category>?> = combine(
        _sortType,
        refreshRequests.stateIn(viewModelScope, SharingStarted.Eagerly, Unit),
    ) { sortType, _ ->
        sortType
    }
        .flatMapLatest { sortType ->
            repository.loadCategoriesFlow(sortType)
                .distinctUntilChanged()
                .onEach { _isRefreshing.value = false }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    fun sort(sortType: Category.SortType) {
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
}
