package com.boardgamegeek.ui.forum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.livedata.ForumPagingSource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@HiltViewModel
class ForumViewModel @Inject constructor(
    application: Application,
    private val repository: ForumRepository
) : AndroidViewModel(application) {
    private val forumId = MutableStateFlow(BggContract.INVALID_ID)

    fun setForumId(id: Int) {
        if (forumId.value != id) forumId.value = id
    }

    val threads = forumId.flatMapLatest { id ->
        if (id == BggContract.INVALID_ID) {
            flowOf(PagingData.empty())
        } else {
            Pager(PagingConfig(ForumResponse.PAGE_SIZE)) {
                ForumPagingSource(id, repository)
            }.flow
        }
    }.cachedIn(viewModelScope)
}
