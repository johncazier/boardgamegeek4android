package com.boardgamegeek.ui.hotness

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.HotnessRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.ui.navigation.ActionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotnessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    hotnessRepository: HotnessRepository,
    private val playRepository: PlayRepository,
) : ActionViewModel() {

    val errorMessageFlow = savedStateHandle.getMutableStateFlow<String?>("errorMessage", null)

    val loggedPlayResultFlow = MutableStateFlow<PlayUploadResult?>(null)

    val hotGamesFlow = hotnessRepository.getHotnessFlow().stateInWhileSubscribed(viewModelScope, null)

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure)
                postError(result.exceptionOrNull())
            else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.Companion.INVALID_ID)
                        loggedPlayResultFlow.value = it
                }
            }
        }
    }

    private fun postError(exception: Throwable?) {
        errorMessageFlow.value = exception?.message.orEmpty()
    }
}