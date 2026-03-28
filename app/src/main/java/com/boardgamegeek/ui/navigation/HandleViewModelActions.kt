package com.boardgamegeek.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun HandleViewModelActions(viewModel: ActionViewModel) {
    val navigator = LocalAppNavigator.current

    LaunchedEffect(Unit) {
        viewModel.viewModelActionFlow.collect { action ->
            when (action) {
                ViewModelAction.Leave -> navigator.popBackStack()
            }
        }
    }
}
