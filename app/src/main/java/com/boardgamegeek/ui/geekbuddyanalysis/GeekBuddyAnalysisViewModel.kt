package com.boardgamegeek.ui.geekbuddyanalysis

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.GeekBuddyAnalysis
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.repository.GeekBuddyAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GeekBuddyAnalysisViewModel @Inject constructor(
    private val application: Application,
    private val repository: GeekBuddyAnalysisRepository,
) : ViewModel() {
    private val request = MutableStateFlow<Request?>(null)

    val analysis: StateFlow<RefreshableResource<GeekBuddyAnalysis>?> = request
        .filter { it != null }
        .flatMapLatest { params ->
            flow {
                emit(RefreshableResource.refreshing())
                try {
                    emit(RefreshableResource.success(repository.load(params!!.gameId)))
                } catch (e: Exception) {
                    emit(RefreshableResource.error(e, application))
                }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    fun load(gameId: Int) {
        request.value = Request(gameId, (request.value?.refreshKey ?: 0) + 1)
    }

    private data class Request(val gameId: Int, val refreshKey: Int)
}
