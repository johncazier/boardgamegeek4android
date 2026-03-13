package com.boardgamegeek.ui.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.boardgamegeek.io.model.GameRemote
import com.boardgamegeek.livedata.CommentsPagingSource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GameCommentsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        RATING, USER
    }

    private data class CommentState(val id: Int, val sort: SortType)

    private val state = MutableStateFlow(CommentState(BggContract.INVALID_ID, SortType.USER))

    val sort: StateFlow<SortType> = state
        .map { it.sort }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortType.USER)

    fun setGameId(id: Int) {
        if (state.value.id != id) {
            state.value = state.value.copy(id = id)
        }
    }

    fun setSort(sort: SortType) {
        if (state.value.sort != sort) {
            state.value = state.value.copy(sort = sort)
        }
    }

    val comments = state.flatMapLatest { current ->
        if (current.id == BggContract.INVALID_ID) {
            flowOf(PagingData.empty())
        } else {
            val sortByRating = current.sort == SortType.RATING
            Pager(PagingConfig(GameRemote.PAGE_SIZE)) {
                CommentsPagingSource(current.id, sortByRating, gameRepository)
            }.flow
        }
    }.cachedIn(viewModelScope)
}
