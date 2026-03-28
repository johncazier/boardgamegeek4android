package com.boardgamegeek.ui.sync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : ComponentActivity() {
    private val viewModel by viewModels<SyncViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SyncRouteScreen()
        }
    }
}

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
