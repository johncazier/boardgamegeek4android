package com.boardgamegeek.ui.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository
import com.boardgamegeek.extensions.stateInWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class CategoryViewModel @Inject constructor(
    application: Application,
    private val repository: CategoryRepository,
) : AndroidViewModel(application) {
    private val categoryId = MutableStateFlow(BggContract.INVALID_ID)
    private val sortType = MutableStateFlow(CollectionItem.SortType.RATING)
    private val reloadCounter = MutableStateFlow(0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val sort: StateFlow<CollectionItem.SortType> = sortType

    val collection: StateFlow<List<CollectionItem>?> =
        combine(categoryId, sortType, reloadCounter) { id, sort, _ -> id to sort }
            .flatMapLatest { (id, sort) ->
                if (id == BggContract.INVALID_ID) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.loadCollectionFlow(id, sort)
                        .distinctUntilChanged()
                }
            }
            .onEach { _isRefreshing.value = false }
            .stateInWhileSubscribed(viewModelScope, null)

    fun setId(id: Int) {
        if (categoryId.value != id) {
            categoryId.value = id
        }
    }

    fun setSort(sort: CollectionItem.SortType) {
        if (sortType.value != sort) {
            sortType.value = sort
        }
    }

    fun reload() {
        _isRefreshing.value = true
        reloadCounter.value = reloadCounter.value + 1
    }
}
