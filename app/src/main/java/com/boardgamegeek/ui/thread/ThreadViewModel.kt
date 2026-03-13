package com.boardgamegeek.ui.thread

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.ThreadArticles
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val application: Application,
    private val repository: ForumRepository,
) : ViewModel() {
    private val threadId = MutableStateFlow(BggContract.INVALID_ID)
    private val _articles = MutableStateFlow<RefreshableResource<ThreadArticles>?>(null)
    val articles: StateFlow<RefreshableResource<ThreadArticles>?> = _articles

    init {
        viewModelScope.launch {
            threadId.collectLatest { id ->
                if (id == BggContract.INVALID_ID) {
                    _articles.value = RefreshableResource.error("Invalid thread ID.")
                    return@collectLatest
                }
                _articles.value = RefreshableResource.refreshing(_articles.value?.data)
                _articles.value = try {
                    RefreshableResource.success(repository.loadThread(id))
                } catch (e: Exception) {
                    RefreshableResource.error(e, application, _articles.value?.data)
                }
            }
        }
    }

    fun setThreadId(id: Int) {
        if (threadId.value != id) threadId.value = id
    }
}
