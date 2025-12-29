package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.TopGame
import com.boardgamegeek.repository.TopGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopGamesViewModel @Inject constructor(
    private val application: Application,
    private val repository: TopGameRepository,
) : AndroidViewModel(application) {

    private val _topGamesFlow = MutableStateFlow<RefreshableResource<List<TopGame>>?>(null)
    val topGamesFlow: StateFlow<RefreshableResource<List<TopGame>>?> = _topGamesFlow.asStateFlow()

    val errorMessageFlow = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _topGamesFlow.value = RefreshableResource.refreshing(_topGamesFlow.value?.data)
            try {
                repository.findTopGames().let {
                    _topGamesFlow.value = RefreshableResource.success(it)
                }
            } catch (e: Exception) {
                _topGamesFlow.value = RefreshableResource.error(e, application)
                errorMessageFlow.value = e.message
            }
        }
    }
}
