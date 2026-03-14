package com.boardgamegeek.ui.buddycollection

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class BuddyCollectionViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
) : ViewModel() {
    private data class Params(val username: String, val status: CollectionStatus)

    private val usernameAndStatus = MutableStateFlow<Params?>(null)

    fun setUsername(username: String) {
        if (usernameAndStatus.value?.username != username) {
            usernameAndStatus.value = Params(username, usernameAndStatus.value?.status ?: DEFAULT_STATUS)
        }
    }

    fun setStatus(status: CollectionStatus) {
        FirebaseAnalytics.getInstance(application).logEvent("Filter") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
            param("filterType", status.toString())
        }
        if (usernameAndStatus.value?.status != status) {
            usernameAndStatus.value = Params(usernameAndStatus.value?.username.orEmpty(), status)
        }
    }

    val status: StateFlow<CollectionStatus> = usernameAndStatus
        .map { it?.status ?: DEFAULT_STATUS }
        .stateInWhileSubscribed(viewModelScope, DEFAULT_STATUS)

    val collection: StateFlow<RefreshableResource<List<CollectionItem>>?> =
        usernameAndStatus
            .flatMapLatest { params ->
                if (params == null || params.username.isBlank()) {
                    flow { emit(RefreshableResource.success(emptyList())) }
                } else {
                    flow {
                        emit(RefreshableResource.refreshing(null))
                        try {
                            emit(RefreshableResource.success(userRepository.refreshCollection(params.username, params.status)))
                        } catch (e: Exception) {
                            emit(RefreshableResource.error(e, application))
                        }
                    }
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    companion object {
        val DEFAULT_STATUS: CollectionStatus = CollectionStatus.Own
    }
}
