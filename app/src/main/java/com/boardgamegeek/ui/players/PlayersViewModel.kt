package com.boardgamegeek.ui.players

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Player
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val application: Application,
    private val playRepository: PlayRepository,
) : ViewModel() {
    private val _sortType = MutableStateFlow(Player.SortType.NAME)
    val sortType: StateFlow<Player.SortType> = _sortType

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter

    private val allPlayers: StateFlow<List<Player>?> = sortType
        .flatMapLatest { sort ->
            playRepository.loadPlayersFlow(sort)
                .distinctUntilChanged()
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val players: StateFlow<List<Player>?> = combine(allPlayers, filter) { allPlayers, filterText ->
        assembleAvailablePlayers(allPlayers, filterText)
    }.stateInWhileSubscribed(viewModelScope, null)

    fun sort(sortType: Player.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun filter(filter: String) {
        if (_filter.value != filter) _filter.value = filter
    }

    fun sectionHeader(player: Player?): String {
        return when (_sortType.value) {
            Player.SortType.NAME -> player?.name.firstChar()
            Player.SortType.PLAY_COUNT -> (player?.playCount ?: 0).orderOfMagnitude()
            Player.SortType.WIN_COUNT -> (player?.winCount ?: 0).orderOfMagnitude()
            else -> ""
        }
    }

    fun getDisplayText(player: Player?): String {
        val context = application as BggApplication
        return when (_sortType.value) {
            Player.SortType.WIN_COUNT -> {
                val winCount = player?.winCount ?: 0
                context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount)
            }
            else -> {
                val playCount = player?.playCount ?: 0
                context.resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount)
            }
        }
    }

    private fun assembleAvailablePlayers(
        allPlayers: List<Player>?,
        filter: String,
    ): List<Player> {
        return if (filter.isNotBlank()) {
            allPlayers?.filter {
                it.name.contains(filter, true) ||
                    it.username.contains(filter, true) ||
                    (it.userFullName?.contains(filter, true) == true)
            }.orEmpty()
        } else {
            allPlayers.orEmpty()
        }
    }
}
