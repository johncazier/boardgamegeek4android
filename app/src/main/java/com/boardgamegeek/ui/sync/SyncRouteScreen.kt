package com.boardgamegeek.ui.sync

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.theme.AppTheme

@Composable
fun SyncRouteScreen(
    viewModel: SyncViewModel = hiltViewModel(),
) {
    AppTheme {
        AppScreen(
            topBarTitle = stringResource(R.string.title_sync),
            currentScreenRouteFromActivity = "sync",
        ) { paddingValues ->
            SyncScreen(
                viewModel = viewModel,
                paddingValues = paddingValues,
            )
        }
    }
}
