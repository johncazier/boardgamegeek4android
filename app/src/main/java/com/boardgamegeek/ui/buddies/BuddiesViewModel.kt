package com.boardgamegeek.ui.buddies

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BuddiesViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val prefs: SharedPreferences = application.preferences()
    private val syncPrefs: SharedPreferences = SyncPrefs.getPrefs(application.applicationContext)
    private val refreshGuard = AtomicBoolean()

    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _sortType = MutableStateFlow(User.SortType.USERNAME)
    val sortType: StateFlow<User.SortType> = _sortType

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _syncEnabled = MutableStateFlow(prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled

    val buddies: StateFlow<List<User>?> = combine(
        _sortType,
        refreshRequests.stateIn(viewModelScope, SharingStarted.Eagerly, Unit),
    ) { sortType, _ ->
        sortType
    }
        .flatMapLatest { sortType ->
            userRepository.loadBuddiesFlow(sortType)
                .distinctUntilChanged()
                .onEach { _isRefreshing.value = false }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true &&
                    syncPrefs.getBuddiesTimestamp().isOlderThan(1.days) &&
                    refreshGuard.compareAndSet(false, true)
                ) {
                    userRepository.refreshBuddies()?.let { errorMessage ->
                        _errorMessage.value = errorMessage
                    }
                }
            } finally {
                _isRefreshing.value = false
                refreshGuard.set(false)
            }
        }
    }

    fun sort(sortType: User.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun sectionHeader(user: User?): String {
        return when (_sortType.value) {
            User.SortType.FIRST_NAME -> user?.firstName.firstChar()
            User.SortType.LAST_NAME -> user?.lastName.firstChar()
            User.SortType.USERNAME -> user?.username.firstChar()
            else -> DEFAULT_HEADER
        }
    }

    fun enableSyncAndRefresh() {
        prefs[PREFERENCES_KEY_SYNC_BUDDIES] = true
        _syncEnabled.value = true
        refresh()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
