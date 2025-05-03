package com.boardgamegeek.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.boardgamegeek.ui.NewLogPlayRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewLogPlayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val route = savedStateHandle.get<NewLogPlayRoute>("route")!!

    val gameId = route.gameId

    val gameName = route.gameName

}