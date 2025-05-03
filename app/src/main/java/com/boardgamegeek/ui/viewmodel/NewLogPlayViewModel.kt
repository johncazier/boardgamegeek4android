package com.boardgamegeek.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.boardgamegeek.ui.NewLogPlayActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewLogPlayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val gameId = savedStateHandle.get<Int>(NewLogPlayActivity.KEY_GAME_ID)

    val gameName = savedStateHandle.get<String>(NewLogPlayActivity.KEY_GAME_NAME)

}