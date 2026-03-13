package com.boardgamegeek.ui.gamecolors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.extensions.stateInWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@HiltViewModel
class GameColorsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val gameId = MutableStateFlow<Int?>(null)
    private val refreshCounter = MutableStateFlow(0)

    val colors: StateFlow<List<String>?> =
        combine(gameId, refreshCounter) { id, _ -> id }
            .flatMapLatest { id ->
                if (id == null || id == BggContract.INVALID_ID) {
                    flow { emit(emptyList()) }
                } else {
                    flow { emit(gameRepository.getPlayColors(id)) }
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    fun setGameId(id: Int) {
        if (gameId.value != id) {
            gameId.value = id
        }
    }

    fun refresh() {
        refreshCounter.value = refreshCounter.value + 1
    }

    fun addColor(color: String?) {
        viewModelScope.launch {
            gameRepository.addPlayColor(gameId.value ?: BggContract.INVALID_ID, color)
            refresh()
        }
    }

    fun removeColor(color: String) {
        viewModelScope.launch {
            gameRepository.deletePlayColor(gameId.value ?: BggContract.INVALID_ID, color)
            refresh()
        }
    }

    fun computeColors() {
        viewModelScope.launch {
            gameRepository.computePlayColors(gameId.value ?: BggContract.INVALID_ID)
            refresh()
        }
    }
}
