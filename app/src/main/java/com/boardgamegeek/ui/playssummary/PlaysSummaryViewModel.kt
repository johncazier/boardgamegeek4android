package com.boardgamegeek.ui.playssummary

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP
import com.boardgamegeek.extensions.PlayStatPrefs
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.HIndex
import com.boardgamegeek.model.Location
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.PlayerColor
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel
class PlaysSummaryViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    val username: StateFlow<String?> =
        preferenceFlow<String>(AccountPreferences.KEY_USERNAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val syncPlays: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(PREFERENCES_KEY_SYNC_PLAYS)
            .stateInWhileSubscribed(viewModelScope, prefs[PREFERENCES_KEY_SYNC_PLAYS, false])

    val syncPlaysTimestamp: StateFlow<Long?> =
        preferenceFlow<Long>(PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP)
            .stateInWhileSubscribed(viewModelScope, prefs[PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP, 0L])

    val oldestSyncDate: StateFlow<Long?> =
        preferenceFlow<Long>(SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val newestSyncDate: StateFlow<Long?> =
        preferenceFlow<Long>(SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    private val plays: StateFlow<List<Play>?> =
        syncPlays
            .flatMapLatest { enabled ->
                if (enabled == true) {
                    playRepository.loadPlaysFlow()
                        .distinctUntilChanged()
                        .onStart { refresh() }
                } else {
                    flowOf(null)
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    val playCount: StateFlow<Int> =
        plays.map { list -> list?.sumOf { it.quantity } ?: 0 }
            .distinctUntilChanged()
            .stateInWhileSubscribed(viewModelScope, 0)

    val playsInProgress: StateFlow<List<Play>> =
        plays.map { list -> list?.filter { it.dirtyTimestamp > 0L }.orEmpty() }
            .distinctUntilChanged()
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val playsNotInProgress: StateFlow<List<Play>> =
        plays.map { list -> list?.filter { it.dirtyTimestamp == 0L }.orEmpty().take(ITEMS_TO_DISPLAY) }
            .distinctUntilChanged()
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val players: StateFlow<List<Player>> =
        combine(syncPlays, username) { enabled, user -> Pair(enabled == true, user) }
            .flatMapLatest { (enabled, user) ->
                if (!enabled) {
                    flowOf(emptyList())
                } else {
                    playRepository.loadPlayersFlow()
                        .map { list ->
                            list.filter { player -> player.username != user }
                                .take(ITEMS_TO_DISPLAY)
                        }
                        .distinctUntilChanged()
                }
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val locations: StateFlow<List<Location>> =
        syncPlays.flatMapLatest { enabled ->
            if (enabled == true) {
                playRepository.loadLocationsFlow()
                    .map { list -> list.filter { it.name.isNotBlank() }.take(ITEMS_TO_DISPLAY) }
                    .distinctUntilChanged()
            } else {
                flowOf(emptyList())
            }
        }.stateInWhileSubscribed(viewModelScope, emptyList())

    val colors: StateFlow<List<PlayerColor>> =
        username.flatMapLatest { user ->
            playRepository.loadUserColorsFlow(user.orEmpty()).distinctUntilChanged()
        }.stateInWhileSubscribed(viewModelScope, emptyList())

    val hIndex: StateFlow<HIndex> =
        combine(
            preferenceFlow<Int>(PlayStatPrefs.KEY_GAME_H_INDEX),
            preferenceFlow<Int>(PlayStatPrefs.KEY_GAME_H_INDEX + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX),
        ) { h, n ->
            HIndex(h ?: 0, n ?: 0)
        }.stateInWhileSubscribed(viewModelScope, HIndex.invalid())

    fun enableSyncing(enable: Boolean) {
        if (enable) {
            prefs[PREFERENCES_KEY_SYNC_PLAYS] = true
        } else {
            prefs[PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    fun refresh(): Boolean {
        return if (syncPlays.value == true && !_isSyncing.value) {
            viewModelScope.launch {
                try {
                    _isSyncing.value = true
                    playRepository.refreshRecentPlays()?.let { errorMessage ->
                        _errorMessage.tryEmit(errorMessage)
                    }
                } finally {
                    _isSyncing.value = false
                }
            }
            true
        } else {
            false
        }
    }

    fun reset() {
        viewModelScope.launch {
            playRepository.resetPlays()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> preferenceFlow(preferenceKey: String, prefsName: String? = null): Flow<T?> = callbackFlow {
        val preferences = getApplication<Application>().preferences(prefsName)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
            if (changedKey == preferenceKey) {
                trySend(sharedPreferences.all[preferenceKey] as? T)
            }
        }
        trySend(preferences.all[preferenceKey] as? T)
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        const val ITEMS_TO_DISPLAY = 5
    }
}
