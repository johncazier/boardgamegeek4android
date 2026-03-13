package com.boardgamegeek.ui.sync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : ComponentActivity() {
    private val viewModel by viewModels<SyncViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                AppScreen(
                    topBarTitle = stringResource(R.string.title_sync),
                    currentScreenRouteFromActivity = "sync",
                    onSearchClick = { startActivity(Intent(this, SearchResultsActivity::class.java)) }
                ) { paddingValues ->
                    SyncScreen(
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                    )
                }
            }
        }
    }
}
