package com.boardgamegeek.ui.buddy

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.R
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.PlayerColor
import com.boardgamegeek.model.User
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BuddyViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
    private val playRepository: PlayRepository,
) : ViewModel() {
    private data class Target(val name: String, val type: PlayRepository.PlayerType)

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application)
    private val isRefreshing = AtomicBoolean()
    private val target = MutableStateFlow<Target?>(null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _updateMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val updateMessage: SharedFlow<String> = _updateMessage.asSharedFlow()

    private val _isUsernameValid = MutableStateFlow<Boolean?>(null)
    val isUsernameValid: StateFlow<Boolean?> = _isUsernameValid

    private val _validatingUsername = MutableStateFlow(false)
    val validatingUsername: StateFlow<Boolean> = _validatingUsername

    val username: StateFlow<String?> = target
        .map { item ->
            when (item?.type) {
                PlayRepository.PlayerType.USER -> item.name
                else -> null
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, null)

    val playerName: StateFlow<String?> = target
        .map { item ->
            when (item?.type) {
                PlayRepository.PlayerType.NON_USER -> item.name
                else -> null
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, null)

    val buddy: StateFlow<User?> = target
        .flatMapLatest { item ->
            if (item?.type == PlayRepository.PlayerType.USER && item.name.isNotBlank()) {
                userRepository.loadUserFlow(item.name)
                    .distinctUntilChanged()
            } else {
                flowOf(null)
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val player: StateFlow<Player?> = target
        .flatMapLatest { item ->
            if (item == null) {
                flowOf<Player?>(null)
            } else {
                flow { emit(playRepository.loadPlayer(item.name, item.type)) }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val colors: StateFlow<List<PlayerColor>?> = target
        .flatMapLatest { item ->
            if (item == null || item.name.isBlank()) {
                flowOf(emptyList<PlayerColor>())
            } else {
                flow { emit(playRepository.loadPlayerColors(item.name, item.type)) }
            }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    init {
        viewModelScope.launch {
            buddy.collect { loadedBuddy ->
                if (loadedBuddy?.updatedTimestamp.isOlderThan(1.days)) refresh()
            }
        }
    }

    fun setUsername(name: String?) {
        if (name.isNullOrBlank()) return
        val newTarget = Target(name, PlayRepository.PlayerType.USER)
        if (target.value != newTarget) target.value = newTarget
    }

    fun setPlayerName(name: String?) {
        if (name.isNullOrBlank()) return
        val newTarget = Target(name, PlayRepository.PlayerType.NON_USER)
        if (target.value != newTarget) target.value = newTarget
    }

    fun refresh() {
        viewModelScope.launch {
            val item = target.value
            if (item?.type != PlayRepository.PlayerType.USER || item.name.isBlank()) return@launch
            try {
                _refreshing.value = true
                if (isRefreshing.compareAndSet(false, true)) {
                    userRepository.refresh(item.name)?.let { _error.value = it }
                }
            } finally {
                _refreshing.value = false
                isRefreshing.set(false)
            }
        }
    }

    fun updateNickName(nickName: String, updatePlays: Boolean) {
        viewModelScope.launch {
            val item = target.value
            if (item?.type != PlayRepository.PlayerType.USER || item.name.isBlank()) return@launch
            val username = item.name
            userRepository.updateNickName(username, nickName)

            val message = if (updatePlays) {
                val newNickName = nickName.ifBlank { buddy.value?.fullName }
                if (newNickName.isNullOrBlank()) {
                    application.getString(R.string.msg_missing_nickname)
                } else {
                    val internalIds = playRepository.updatePlaysWithNickName(username, newNickName)
                    playRepository.enqueueUploadRequest(internalIds)
                    application.resources.getQuantityString(
                        R.plurals.msg_updated_plays_buddy_nickname,
                        internalIds.size,
                        internalIds.size,
                        username,
                        newNickName
                    )
                }
            } else {
                application.getString(R.string.msg_updated_nickname, nickName)
            }

            _updateMessage.emit(message)
            firebaseAnalytics.logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyNickname")
                param("Username", username)
                param("NickName", nickName)
                param("Action", "Edit")
            }
        }
    }

    fun renamePlayer(newName: String) {
        viewModelScope.launch {
            val item = target.value
            if (item?.type != PlayRepository.PlayerType.NON_USER || newName.isBlank() || item.name.isBlank()) return@launch
            val oldName = item.name
            val internalIds = playRepository.renamePlayer(oldName, newName)
            playRepository.enqueueUploadRequest(internalIds)
            _updateMessage.emit(application.getString(R.string.msg_play_player_change, oldName, newName))
            setPlayerName(newName)
            firebaseAnalytics.logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "RenamePlayer")
                param("OldName", oldName)
                param("NewName", newName)
                param("Action", "Edit")
            }
        }
    }

    fun addUsernameToPlayer(username: String) {
        viewModelScope.launch {
            val item = target.value
            if (item?.type != PlayRepository.PlayerType.NON_USER || username.isBlank() || item.name.isBlank()) return@launch
            val playerName = item.name
            val errorMessage = userRepository.refresh(username)
            if (errorMessage.isNullOrEmpty()) {
                userRepository.updateNickName(username, playerName)
                val internalIds = playRepository.addUsernameToPlayer(playerName, username)
                playRepository.enqueueUploadRequest(internalIds)
                _updateMessage.emit(application.getString(R.string.msg_player_add_username, username, playerName))
                setUsername(username)
                firebaseAnalytics.logEvent("DataManipulation") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "AddUserName")
                    param("PlayerName", playerName)
                    param("Username", username)
                    param("Action", "Edit")
                }
            } else {
                _error.value = errorMessage
            }
        }
    }

    fun clearUsernameValidation() {
        _isUsernameValid.value = null
    }

    fun validateUsername(username: String) {
        viewModelScope.launch {
            _validatingUsername.value = true
            _isUsernameValid.value = null
            _isUsernameValid.value = userRepository.validateUsername(username)
            _validatingUsername.value = false
        }
    }

    fun clearError() {
        _error.update { null }
    }
}
