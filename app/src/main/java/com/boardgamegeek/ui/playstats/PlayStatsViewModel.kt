package com.boardgamegeek.ui.playstats

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.addSyncStatus
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getSyncStatusesOrDefault
import com.boardgamegeek.extensions.mapToEnum
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.model.PlayerStats
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.work.SyncCollectionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class PlayStatsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }

    val includeIncomplete: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(LOG_PLAY_STATS_INCOMPLETE)
            .stateInWhileSubscribed(viewModelScope, prefs[LOG_PLAY_STATS_INCOMPLETE, false])

    val includeExpansions: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(LOG_PLAY_STATS_EXPANSIONS)
            .stateInWhileSubscribed(viewModelScope, prefs[LOG_PLAY_STATS_EXPANSIONS, false])

    val includeAccessories: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(LOG_PLAY_STATS_ACCESSORIES)
            .stateInWhileSubscribed(viewModelScope, prefs[LOG_PLAY_STATS_ACCESSORIES, false])

    val syncCollectionStatuses: StateFlow<Set<CollectionStatus>> =
        preferenceFlow<Set<String>>(PREFERENCES_KEY_SYNC_STATUSES)
            .map { set ->
                set?.map { it.mapToEnum() }?.toSet() ?: prefs.getSyncStatusesOrDefault()
            }
            .stateInWhileSubscribed(viewModelScope, prefs.getSyncStatusesOrDefault())

    val playStats: StateFlow<PlayStats?> =
        combine(includeIncomplete, includeExpansions, includeAccessories) { _, _, _ -> Unit }
            .flatMapLatest {
                flow {
                    emit(playRepository.calculatePlayStats())
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    val playerStats: StateFlow<PlayerStats?> =
        includeIncomplete
            .flatMapLatest {
                flow {
                    emit(playRepository.calculatePlayerStats())
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    fun setIncludeSettings(incomplete: Boolean, expansions: Boolean, accessories: Boolean) {
        prefs[LOG_PLAY_STATS_INCOMPLETE] = incomplete
        prefs[LOG_PLAY_STATS_EXPANSIONS] = expansions
        prefs[LOG_PLAY_STATS_ACCESSORIES] = accessories
    }

    fun enableOwnedPlayedSync() {
        prefs.addSyncStatus(CollectionStatus.Own)
        prefs.addSyncStatus(CollectionStatus.Played)
        SyncCollectionWorker.requestSync(getApplication())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> preferenceFlow(preferenceKey: String): Flow<T?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
            if (changedKey == preferenceKey) {
                trySend(sharedPreferences.all[preferenceKey] as? T)
            }
        }
        trySend(prefs.all[preferenceKey] as? T)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
