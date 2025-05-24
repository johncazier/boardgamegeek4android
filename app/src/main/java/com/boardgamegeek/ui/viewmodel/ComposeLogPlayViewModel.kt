package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.NewPlayPlayer
import com.boardgamegeek.model.Player
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.ui.ComposeLogPlayRoute
import com.boardgamegeek.ui.navigation.ActionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ComposeLogPlayViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val playRepository: PlayRepository,
) : ActionViewModel() {

    val route = savedStateHandle.get<ComposeLogPlayRoute>("route")!!

    val gameId = route.gameId

    val gameName = route.gameName

    val gameImageUrl = route.gameImageUrl

    val selectedDateFlow: StateFlow<LocalDate> = savedStateHandle.getStateFlow("selectedDate", LocalDate.now())

    val availablePlayersFlow: StateFlow<List<Player>> = playRepository.loadPlayersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPlayerIdsFlow = savedStateHandle.getMutableStateFlow("selectedPlayerIds", arrayListOf<String>())

    val playerMapFlow = MutableStateFlow<Map<String, NewPlayPlayer>>(emptyMap())

    val selectedPlayersFlow: StateFlow<List<Player>> =
        combine(availablePlayersFlow, selectedPlayerIdsFlow) { availablePlayers, selectedIds ->
            availablePlayers
                .filter { player -> selectedIds.contains(player.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commentsFlow = savedStateHandle.getMutableStateFlow("comments", "")

    private val prefs: SharedPreferences by lazy { application.preferences() }

    private var loadedPlayers = false

    init {
        if (!loadedPlayers) {
            val recentlyPlayed = (prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L).howManyHoursOld() < 1
            val players = prefs.getLastPlayPlayers()

            val initialPlayers = if (recentlyPlayed || players.isEmpty()) players else listOf(players.first())

            selectedPlayerIdsFlow.value = ArrayList(initialPlayers.map { it.id })

            loadedPlayers = true
        }
    }

    fun updateSelectedDate(selectedDate: LocalDate?) {
        savedStateHandle["selectedDate"] = selectedDate ?: LocalDate.now()
    }

    fun cancel() {
        leave()
    }

    fun save() {
        //todo
        leave()
    }

    fun onPlayersSelected(playerIds: List<String>) {
        selectedPlayerIdsFlow.value = ArrayList(playerIds)
    }

    fun updatePlayerScore(player: Player, score: String) {
        val updatedPlayer = (playerMapFlow.value[player.id] ?: NewPlayPlayer(player)).copy(score = score)
        playerMapFlow.value = playerMapFlow.value + (player.id to updatedPlayer)
    }

    fun onPlayerWinChanged(player: Player, win: Boolean) {
        val updatedPlayer = (playerMapFlow.value[player.id] ?: NewPlayPlayer(player)).copy(isWin = win)
        playerMapFlow.value = playerMapFlow.value + (player.id to updatedPlayer)
    }
}