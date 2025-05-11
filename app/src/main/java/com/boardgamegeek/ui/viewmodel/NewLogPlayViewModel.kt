package com.boardgamegeek.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.boardgamegeek.ui.NewLogPlayRoute
import com.boardgamegeek.ui.navigation.ActionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NewLogPlayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ActionViewModel() {

    val route = savedStateHandle.get<NewLogPlayRoute>("route")!!

    val gameId = route.gameId

    val gameName = route.gameName

    val gameImageUrl = route.gameImageUrl

    val selectedDateFlow: StateFlow<LocalDate> = savedStateHandle.getStateFlow("selectedDate", LocalDate.now())

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
}