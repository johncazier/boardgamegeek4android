package com.boardgamegeek.ui.playstats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GamePlayStatsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val gameCollectionRepository: GameCollectionRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val arePlaysRefreshing = AtomicBoolean(false)
    private val areItemsRefreshing = AtomicBoolean(false)
    private val refreshItemsMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _gameId = MutableStateFlow<Int?>(null)
    fun setGameId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    val collectionItems: StateFlow<List<CollectionItem>?> = _gameId
        .filterNotNull()
        .flatMapLatest { gameId ->
            gameCollectionRepository.loadCollectionItemsForGameFlow(gameId)
                .distinctUntilChanged()
                .onEach { list ->
                    if (areItemsRefreshing.compareAndSet(false, true)) {
                        val lastUpdated = list.minOfOrNull { it.syncTimestamp } ?: 0L
                        if (lastUpdated.isOlderThan(refreshItemsMinutes.minutes)) {
                            gameCollectionRepository.refreshCollectionItems(gameId)
                        }
                        areItemsRefreshing.set(false)
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plays: StateFlow<List<Play>?> = _gameId
        .filterNotNull()
        .flatMapLatest { gameId ->
            playRepository
                .loadPlaysByGameFlow(gameId)
                .distinctUntilChanged { old, new ->
                    old.size == new.size && old.map { it.copy(updateTimestamp = 0) }.toSet() == new.map { it.copy(updateTimestamp = 0) }.toSet()
                }
                .onEach {
                    if (arePlaysRefreshing.compareAndSet(false, true)) {
                        val game = gameRepository.loadGame(gameId)
                        if (game == null || game.updatedPlays.isOlderThan(10.minutes)) {
                            playRepository.refreshPlaysForGame(gameId)
                        }
                        arePlaysRefreshing.set(false)
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val players: StateFlow<List<PlayPlayer>?> = _gameId
        .filterNotNull()
        .flatMapLatest { gameId ->
            playRepository.loadPlayersByGameFlow(gameId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
