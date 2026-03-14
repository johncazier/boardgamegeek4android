package com.boardgamegeek.ui.logplayer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.User
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@HiltViewModel
class LogPlayerViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val gameId = MutableStateFlow<Int?>(null)

    fun setGameId(id: Int) {
        if (gameId.value != id) {
            gameId.value = id
        }
    }

    val players: StateFlow<List<Player>> = gameId
        .flatMapLatest { flow { emit(playRepository.loadPlayers()) } }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val users: StateFlow<List<User>> = gameId
        .flatMapLatest { flow { emit(userRepository.loadUsers()) } }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val colors: StateFlow<List<String>> = gameId
        .flatMapLatest { id ->
            flow {
                emit(if (id == null) emptyList() else gameRepository.getPlayColors(id))
            }
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())
}
