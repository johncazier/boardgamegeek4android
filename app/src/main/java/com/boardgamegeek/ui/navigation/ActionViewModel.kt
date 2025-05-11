package com.boardgamegeek.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class ActionViewModel() : ViewModel() {

    val viewModelActionFlow = MutableSharedFlow<ViewModelAction>()

    fun leave() {
        viewModelScope.launch {
            viewModelActionFlow.emit(ViewModelAction.Leave)
        }
    }
}