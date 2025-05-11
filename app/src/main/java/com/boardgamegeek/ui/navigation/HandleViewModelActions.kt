package com.boardgamegeek.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun HandleViewModelActions(viewModel: ActionViewModel) {

    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.viewModelActionFlow.collect { action ->
            when (action) {
                ViewModelAction.Leave -> activity?.finish()
            }
        }
    }
}